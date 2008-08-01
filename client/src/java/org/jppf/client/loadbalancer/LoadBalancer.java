/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.client.loadbalancer;

import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.client.event.TaskResultEvent;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.server.scheduler.bundle.proportional.ProportionalTuneProfile;
import org.jppf.utils.*;

/**
 * This class is used to balance the number of tasks in an execution between local and remote execution.
 * It uses the proportional bundling alogrithm, which is also used by the JPPF Driver.
 * @see org.jppf.server.scheduler.bundle.proportional.AbstractProportionalBundler
 * @author Laurent Cohen
 */
public class LoadBalancer
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(LoadBalancer.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Index for local bundler.
	 */
	private static final int LOCAL = 0;
	/**
	 * Index for remote bundler.
	 */
	private static final int REMOTE = 1;
	/**
	 * Determines whetehr local execution is enabled on this client.
	 */
	private boolean localEnabled = JPPFConfiguration.getProperties().getBoolean("jppf.local.execution.enabled", true);
	/**
	 * Thread pool for local execution.
	 */
	private ExecutorService threadPool = null;
	/**
	 * The bundlers used to split the tasks between local and remote execution.
	 */
	private ClientProportionalBundler[] bundlers = null;

	/**
	 * Default constructor.
	 */
	public LoadBalancer()
	{
		if (localEnabled)
		{
			int n = Runtime.getRuntime().availableProcessors();
			int poolSize = JPPFConfiguration.getProperties().getInt("jppf.local.execution.threads", n);
			LinkedBlockingQueue queue = new LinkedBlockingQueue();
			threadPool = new ThreadPoolExecutor(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.MICROSECONDS, queue, new JPPFThreadFactory("client processing thread"));
			ProportionalTuneProfile profile = new ProportionalTuneProfile();
			profile.setPerformanceCacheSize(2000);
			profile.setProportionalityFactor(4);
			bundlers = new ClientProportionalBundler[2];
			bundlers[LOCAL] = new ClientProportionalBundler(profile);
			bundlers[REMOTE] = new ClientProportionalBundler(profile);
			for (Bundler b: bundlers) b.setup();
		}
	}

	/**
	 * Perform the execution.
	 * @param execution the execution to perform.
	 * @param connection the client connection for sending remote execution requests.
	 * @throws Exception if an error is raised during execution.
	 */
	public void execute(ClientExecution execution, JPPFClientConnectionImpl connection) throws Exception
	{
		int count = 0;
		List<JPPFTask> tasks = execution.tasks;
		for (JPPFTask task : tasks) task.setPosition(count++);
		if (localEnabled)
		{
			if (connection != null)
			{
				bundlers[LOCAL].setMaxSize(tasks.size());
				bundlers[REMOTE].setMaxSize(tasks.size());
				int n = bundlers[LOCAL].getBundleSize();
				if (n > tasks.size()) n = tasks.size() - 1;
				int n2 = bundlers[REMOTE].getBundleSize();
				if (n2 + n > tasks.size()) n2 = tasks.size() - n;
				int diff = tasks.size() - (n + n2);
				if (diff > 0)
				{
					n += diff/2;
					n2 += diff/2;
					if (tasks.size() > n + n2) n++;
				}
				if (debugEnabled) log.debug("bundlers[local="+n+", remote="+n2+"]");
				List<List<JPPFTask>> list = new ArrayList<List<JPPFTask>>();
				list.add(CollectionUtils.getAllElements(tasks, 0, n));
				list.add(CollectionUtils.getAllElements(tasks, n, n2));
				ExecutionThread[] threads = new ExecutionThread[2];
				threads[0] = new ExecutionThread(list.get(0), execution);
				threads[1] = new ExecutionThread(list.get(1), execution, connection);
				for (int i=0; i<2; i++) threads[i].start();
				if (!execution.isBlocking) for (int i=0; i<2; i++) threads[i].join();
			}
			else
			{
				ExecutionThread localThread = new ExecutionThread(tasks, execution);
				if (!execution.isBlocking) localThread.start();
				else localThread.run();
			}
		}
		else
		{
			ExecutionThread remoteThread = new ExecutionThread(tasks, execution, connection);
			if (!execution.isBlocking) remoteThread.start();
			else remoteThread.run();
		}
	}

	/**
	 * Instances of this class are intended to perform local and remote task executions concurrently.
	 */
	public class ExecutionThread extends Thread
	{
		/**
		 * Determines whether execution is local or remote.
		 */
		private boolean local = true;
		/**
		 * The tasks to execute.
		 */
		private List<JPPFTask> tasks = null;
		/**
		 * The connection to the driver to use.
		 */
		private JPPFClientConnectionImpl connection = null;
		/**
		 * Exception that may result from the execution.
		 */
		private Exception exception = null;
		/**
		 * The execution to perform.
		 */
		private ClientExecution execution = null;

		/**
		 * Initialize this execution thread for local execution.
		 * @param tasks the tasks to execute.
		 * @param execution the execution to perform.
		 */
		public ExecutionThread(List<JPPFTask> tasks, ClientExecution execution)
		{
			local = true;
			this.tasks = tasks;
			this.execution = execution;
		}

		/**
		 * Initialize this execution thread for remote excution.
		 * @param tasks the tasks to execute.
		 * @param execution the execution to perform.
		 * @param connection the connection to the driver to use.
		 */
		public ExecutionThread(List<JPPFTask> tasks, ClientExecution execution, JPPFClientConnectionImpl connection)
		{
			local = false;
			this.tasks = tasks;
			this.execution = execution;
			this.connection = connection;
		}

		/**
		 * Perform the execution.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			try
			{
				long start = System.currentTimeMillis();
				if (local)
				{
					List<Future<?>> futures = new ArrayList<Future<?>>();
					for (JPPFTask task: tasks)
					{
						task.setDataProvider(execution.dataProvider);
						futures.add(threadPool.submit(new TaskWrapper(task)));
					}
					for (Future<?> f: futures) f.get();
					if (execution.listener != null)
					{
						synchronized(execution.listener)
						{
							execution.listener.resultsReceived(new TaskResultEvent(tasks, tasks.get(0).getPosition()));
						}
					}
				}
				else
				{
					int count = 0;
					boolean completed = false;
					while (!completed)
					{
						connection.sendTasks(tasks, execution.dataProvider, execution.policy);
						while (count < tasks.size())
						{
							Pair<List<JPPFTask>, Integer> p = connection.receiveResults();
							count += p.first().size();
							if (execution.listener != null)
							{
								synchronized(execution.listener)
								{
									execution.listener.resultsReceived(new TaskResultEvent(p.first(), p.second()));
								}
							}
						}
						completed = true;
					}
				}

				if (localEnabled)
				{
					double elapsed = System.currentTimeMillis() - start;
					bundlers[local ? LOCAL : REMOTE].feedback(tasks.size(), elapsed);
				}
			}
			catch(Exception e)
			{
				if (debugEnabled) log.debug(e.getMessage(), e);
				exception = e;
			}
		}

		/**
		 * Get the resulting exception.
		 * @return an <code>Exception</code> or null if no exception was raised.
		 */
		public Exception getException()
		{
			return exception;
		}
	}

	/**
	 * JPPF task wrapper used to catch unhandled exceptions.
	 */
	private static class TaskWrapper implements Runnable
	{
		/**
		 * The JPPF task to run.
		 */
		private JPPFTask task = null;

		/**
		 * Initialize this task wrapper with the specified JPPF task.
		 * @param task the JPPF task to execute.
		 */
		public TaskWrapper(JPPFTask task)
		{
			this.task = task;
		}

		/**
		 * Run the task and handle uncaught exceptions.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			try
			{
				task.run();
			}
			catch(Throwable t)
			{
				if (t instanceof Exception) task.setException((Exception) t);
				else task.setException(new JPPFException(t));
			}
		}
	}
}
