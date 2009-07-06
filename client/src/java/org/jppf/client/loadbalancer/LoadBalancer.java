/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
import java.util.concurrent.atomic.AtomicBoolean;

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
	 * Determines whether local execution is enabled on this client.
	 */
	private boolean localEnabled = JPPFConfiguration.getProperties().getBoolean("jppf.local.execution.enabled", false);
	/**
	 * Thread pool for local execution.
	 */
	private ExecutorService threadPool = null;
	/**
	 * The bundlers used to split the tasks between local and remote execution.
	 */
	private ClientProportionalBundler[] bundlers = null;
	/**
	 * Determines whether this load balancer is currently executing tasks locally.
	 */
	private AtomicBoolean locallyExecuting = new AtomicBoolean(false);

	/**
	 * Default constructor.
	 */
	public LoadBalancer()
	{
		if (localEnabled)
		{
			int n = Runtime.getRuntime().availableProcessors();
			int poolSize = JPPFConfiguration.getProperties().getInt("jppf.local.execution.threads", n);
			log.info("local execution enabled with " + poolSize + " processing threads");
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
	 * Stop this load-balancer and cleanup any resource it uses.
	 */
	public void stop()
	{
		if (threadPool != null) threadPool.shutdownNow();
	}

	/**
	 * Perform the execution.
	 * @param job the execution to perform.
	 * @param connection the client connection for sending remote execution requests.
	 * @throws Exception if an error is raised during execution.
	 */
	public void execute(JPPFJob job, JPPFClientConnectionImpl connection) throws Exception
	{
		int count = 0;
		List<JPPFTask> tasks = job.getTasks();
		for (JPPFTask task : tasks) task.setPosition(count++);
		if (localEnabled && locallyExecuting.compareAndSet(false, true))
		{
			try
			{
				if (connection != null)
				{
					int bundleSize[] = new int[2];
					synchronized(bundlers)
					{
						for (int i=LOCAL; i<=REMOTE; i++)
						{
							bundlers[i].setMaxSize(tasks.size());
							bundleSize[i] = bundlers[i].getBundleSize();
						}
					}
					if (bundleSize[LOCAL] > tasks.size()) bundleSize[LOCAL] = tasks.size() - 1;
					if (sum(bundleSize) > tasks.size()) bundleSize[REMOTE] = tasks.size() - bundleSize[LOCAL];
					int diff = tasks.size() - sum(bundleSize);
					if (diff > 0)
					{
						for (int i=LOCAL; i<=REMOTE; i++) bundleSize[i] += diff/2;
						if (tasks.size() > sum(bundleSize)) bundleSize[LOCAL]++;
					}
					if (debugEnabled) log.debug("bundlers[local=" + bundleSize[LOCAL] + ", remote=" + bundleSize[REMOTE] + "]");
					List<List<JPPFTask>> list = new ArrayList<List<JPPFTask>>();
					int idx = 0;
					for (int i=LOCAL; i<=REMOTE; i++)
					{
						list.add(CollectionUtils.getAllElements(tasks, idx, bundleSize[i]));
						idx += bundleSize[i];
					}
					ExecutionThread[] threads = { new LocalExecutionThread(list.get(LOCAL), job), new RemoteExecutionThread(list.get(REMOTE), job, connection) };
					for (int i=LOCAL; i<=REMOTE; i++) threads[i].start();
					if (job.isBlocking())
					{
						for (int i=LOCAL; i<=REMOTE; i++) threads[i].join();
						for (int i=LOCAL; i<=REMOTE; i++) if (threads[i].getException() != null) throw threads[i].getException();
					}
				}
				else
				{
					ExecutionThread localThread = new LocalExecutionThread(tasks, job);
					if (!job.isBlocking()) localThread.start();
					else
					{
						localThread.run();
						if (localThread.getException() != null) throw localThread.getException();
					}
				}
			}
			finally
			{
				locallyExecuting.set(false);
			}
		}
		else if (connection != null)
		{
			ExecutionThread remoteThread = new RemoteExecutionThread(tasks, job, connection);
			remoteThread.run();
			if (remoteThread.getException() != null) throw remoteThread.getException();
		}
		else throw new JPPFException("Null driver connection and local executor is "  + (localEnabled ? "busy" : "disabled"));
	}

	/**
	 * Compute the sum of the elements of an int array.
	 * @param array the input array. 
	 * @return the result sum as an int value.
	 */
	private int sum(int array[])
	{
		int sum = 0;
		for (int i=0; i<array.length; i++) sum += array[i];
		return sum;
	}

	/**
	 * Instances of this class are intended to perform local and remote task executions concurrently.
	 */
	public abstract class ExecutionThread extends Thread
	{
		/**
		 * The tasks to execute.
		 */
		protected List<JPPFTask> tasks = null;
		/**
		 * Exception that may result from the execution.
		 */
		protected Exception exception = null;
		/**
		 * The execution to perform.
		 */
		protected JPPFJob job = null;

		/**
		 * Initialize this execution thread for remote excution.
		 * @param tasks the tasks to execute.
		 * @param job the execution to perform.
		 */
		public ExecutionThread(List<JPPFTask> tasks, JPPFJob job)
		{
			this.tasks = tasks;
			this.job = job;
		}

		/**
		 * Perform the execution.
		 * @see java.lang.Runnable#run()
		 */
		public abstract void run();

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
	 * Instances of this class are intended to perform local task executions concurrently.
	 */
	public class LocalExecutionThread extends ExecutionThread
	{
		/**
		 * Initialize this execution thread for local excution.
		 * @param tasks the tasks to execute.
		 * @param job the execution to perform.
		 */
		public LocalExecutionThread(List<JPPFTask> tasks, JPPFJob job)
		{
			super(tasks, job);
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
				List<Future<?>> futures = new ArrayList<Future<?>>();
				for (JPPFTask task: tasks)
				{
					task.setDataProvider(job.getDataProvider());
					futures.add(threadPool.submit(new TaskWrapper(task)));
				}
				for (Future<?> f: futures) f.get();
				if (job.getResultListener() != null)
				{
					synchronized(job.getResultListener())
					{
						job.getResultListener().resultsReceived(new TaskResultEvent(tasks, tasks.get(0).getPosition()));
					}
				}
				double elapsed = System.currentTimeMillis() - start;
				bundlers[LOCAL].feedback(tasks.size(), elapsed);
			}
			catch(Exception e)
			{
				if (debugEnabled) log.debug(e.getMessage(), e);
				exception = e;
			}
		}
	}

	/**
	 * Instances of this class are intended to perform remote task executions concurrently.
	 */
	public class RemoteExecutionThread extends ExecutionThread
	{
		/**
		 * The connection to the driver to use.
		 */
		private JPPFClientConnectionImpl connection = null;

		/**
		 * Initialize this execution thread for remote excution.
		 * @param tasks the tasks to execute.
		 * @param job the execution to perform.
		 * @param connection the connection to the driver to use.
		 */
		public RemoteExecutionThread(List<JPPFTask> tasks, JPPFJob job, JPPFClientConnectionImpl connection)
		{
			super(tasks, job);
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
				int count = 0;
				boolean completed = false;
				JPPFJob newJob = new JPPFJob(job.getDataProvider(), job.getExecutionPolicy(), job.isBlocking(), job.getResultListener(), job.getPriority());
				for (JPPFTask task: tasks) newJob.addTask(task);
				while (!completed)
				{
					connection.sendTasks(newJob);
					while (count < tasks.size())
					{
						Pair<List<JPPFTask>, Integer> p = connection.receiveResults();
						count += p.first().size();
						if (job.getResultListener() != null)
						{
							synchronized(newJob.getResultListener())
							{
								newJob.getResultListener().resultsReceived(new TaskResultEvent(p.first(), p.second()));
							}
						}
					}
					completed = true;
				}

				if (localEnabled)
				{
					double elapsed = System.currentTimeMillis() - start;
					bundlers[REMOTE].feedback(tasks.size(), elapsed);
				}
			}
			catch(Exception e)
			{
				if (debugEnabled) log.debug(e.getMessage(), e);
				exception = e;
			}
		}
	}
}
