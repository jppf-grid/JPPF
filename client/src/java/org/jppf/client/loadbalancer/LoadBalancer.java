/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.server.protocol.*;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.server.scheduler.bundle.proportional.ProportionalTuneProfile;
import org.jppf.utils.*;
import org.slf4j.*;

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
	private static Logger log = LoggerFactory.getLogger(LoadBalancer.class);
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
	 * Determines whether local execution has already been initialized.
	 */
	private boolean localInitialized = false;
	/**
	 * Thread pool for local execution.
	 */
	private ExecutorService threadPool = null;
	/**
	 * The bundlers used to split the tasks between local and remote execution.
	 */
	private Bundler[] bundlers = null;
	/**
	 * Determines whether this load balancer is currently executing tasks locally.
	 */
	private AtomicBoolean locallyExecuting = new AtomicBoolean(false);
	/**
	 * Lock used when determining if a job can be executed immediately.
	 */
	private final Object availableConnectionLock = new Object();

	/**
	 * Default constructor.
	 */
  @SuppressWarnings("unchecked")
	public LoadBalancer()
	{
		if (isLocalEnabled()) initLocal();
	}

  /**
   * Perform the required initialization for local execution.
   */
  private synchronized void initLocal()
  {
  	if (localInitialized) return;
		int n = Runtime.getRuntime().availableProcessors();
		int poolSize = JPPFConfiguration.getProperties().getInt("jppf.local.execution.threads", n);
		log.info("local execution enabled with " + poolSize + " processing threads");
		LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		threadPool = new ThreadPoolExecutor(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.MICROSECONDS, queue, new JPPFThreadFactory("LocalExec"));
		if (bundlers == null)
		{
			ProportionalTuneProfile profile = new ProportionalTuneProfile();
			profile.setPerformanceCacheSize(1000);
			profile.setProportionalityFactor(1);
			bundlers = new ClientProportionalBundler[2];
			bundlers[LOCAL] = new ClientProportionalBundler(profile);
			bundlers[REMOTE] = new ClientProportionalBundler(profile);
			for (Bundler b: bundlers) b.setup();
		}
		localInitialized = true;
  }
 
	/**
	 * Stop this load-balancer and cleanup any resource it uses.
	 */
	public void stop()
	{
		if (threadPool != null) threadPool.shutdownNow();
		localInitialized = false;
	}

	/**
	 * Perform the execution.
	 * @param job the execution to perform.
	 * @param connection the client connection for sending remote execution requests.
	 * @param localJob determines whether the job will be executed locally, at least partially.
	 * @throws Exception if an error is raised during execution.
	 */
	public void execute(JPPFJob job, AbstractJPPFClientConnection connection, boolean localJob) throws Exception
	{
		//if (isLocalEnabled() && !locallyExecuting.get())
		if (isLocalEnabled() && localJob)
		{
			List<JPPFTask> tasks = job.getTasks();
			if (connection != null)
			{
				int[] bundleSize = new int[2];
				synchronized(bundlers)
				{
					for (int i=LOCAL; i<=REMOTE; i++)
					{
						((ClientProportionalBundler) bundlers[i]).setMaxSize(tasks.size());
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
				if (debugEnabled) log.debug("bundlers[local=" + bundleSize[LOCAL] + ", remote=" + bundleSize[REMOTE] + ']');
				List<List<JPPFTask>> list = new ArrayList<List<JPPFTask>>();
				int idx = 0;
				for (int i=LOCAL; i<=REMOTE; i++)
				{
					list.add(CollectionUtils.getAllElements(tasks, idx, bundleSize[i]));
					idx += bundleSize[i];
				}
				ExecutionThread[] threads = { new LocalExecutionThread(list.get(LOCAL), job), new RemoteExecutionThread(list.get(REMOTE), job, connection) };
				for (int i=LOCAL; i<=REMOTE; i++) threads[i].setContextClassLoader(Thread.currentThread().getContextClassLoader());
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
				if (!job.isBlocking())
				{
					localThread.setContextClassLoader(Thread.currentThread().getContextClassLoader());
					localThread.start();
				}
				else
				{
					localThread.run();
					if (localThread.getException() != null) throw localThread.getException();
				}
			}
		}
		else if (connection != null)
		{
			ExecutionThread remoteThread = new RemoteExecutionThread(job.getTasks(), job, connection);
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
	private static int sum(int[] array)
	{
		int sum = 0;
        for (int anArray : array) sum += anArray;
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
		@Override
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
		@Override
        public void run()
		{
			try
			{
				long accTimeNanos = getAccTime();
				int accSize = JPPFConfiguration.getProperties().getInt("jppf.local.execution.accumulation.size", Integer.MAX_VALUE);
				long start = System.nanoTime();
				LinkedList<Future<JPPFTask>> futures = new LinkedList<Future<JPPFTask>>();
				for (JPPFTask task: tasks)
				{
					task.setDataProvider(job.getDataProvider());
					futures.add(threadPool.submit(new TaskWrapper(task), task));
				}
				int count = 0;
				List<JPPFTask> results = new LinkedList<JPPFTask>();
				while (!futures.isEmpty())
				{
					Future<JPPFTask> f = futures.peek();
					while ((f != null) && f.isDone() &&
						((count == 0) || ((System.nanoTime() - start < accTimeNanos) && (count < accSize))))
					{
						results.add(futures.poll().get());
						count++;
						f = futures.peek();
					}
					if (count > 0)
					{
						if (debugEnabled) log.debug("received " + count + " tasks from local executor" + ", first position=" + results.get(0).getPosition());
						TaskResultListener listener = job.getResultListener();
						if (listener != null)
						{
							synchronized(listener)
							{
								listener.resultsReceived(new TaskResultEvent(results));
							}
						}
						double elapsed = System.nanoTime() - start;
						bundlers[LOCAL].feedback(count, elapsed);
						start = System.nanoTime();
						results.clear();
						count = 0;
					}
					else if (f != null) f.get();
				}
			}
			catch(Exception e)
			{
				if (debugEnabled) log.debug(e.getMessage(), e);
				exception = e;
			}
			finally
			{
				setLocallyExecuting(false);
			}
		}

		/**
		 * Retrieve the accumulation time and convert it to nanoseconds.
		 * @return the accumulation time in nanoseconods.
		 */
		private long getAccTime()
		{
			long time = JPPFConfiguration.getProperties().getLong("jppf.local.execution.accumulation.time", Long.MAX_VALUE);
			char unitChar = JPPFConfiguration.getProperties().getChar("jppf.local.execution.accumulation.time.unit", 'n');
			TimeUnit unit;
			switch(unitChar)
			{
				case 'n':
					unit = TimeUnit.NANOSECONDS;
					break;
				case 'm':
					unit = TimeUnit.MILLISECONDS;
					break;
				case 's':
					unit = TimeUnit.SECONDS;
					break;
				case 'M':
					unit = TimeUnit.MINUTES;
					break;
				case 'h':
					unit = TimeUnit.HOURS;
					break;
				case 'd':
					unit = TimeUnit.DAYS;
					break;
				default:
					unit = TimeUnit.NANOSECONDS;
					break;
			}
			return unit.toNanos(time);
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
		private AbstractJPPFClientConnection connection = null;

		/**
		 * Initialize this execution thread for remote excution.
		 * @param tasks the tasks to execute.
		 * @param job the execution to perform.
		 * @param connection the connection to the driver to use.
		 */
		public RemoteExecutionThread(List<JPPFTask> tasks, JPPFJob job, AbstractJPPFClientConnection connection)
		{
			super(tasks, job);
			this.connection = connection;
		}

		/**
		 * Perform the execution.
		 * @see java.lang.Runnable#run()
		 */
		@Override
        public void run()
		{
			try
			{
				long start = System.nanoTime();
				int count = 0;
				boolean completed = false;
				JPPFJob newJob = createNewJob(job);
				for (JPPFTask task: tasks)
				{
					// needed as JPPFJob.addTask() resets the position
					int pos = task.getPosition();
					newJob.addTask(task);
					task.setPosition(pos);
				}
				while (!completed)
				{
					JPPFTaskBundle bundle = createBundle(newJob);
					connection.sendTasks(bundle, newJob);
					ClassLoader cl = connection.getClient().getRequestClassLoader(bundle.getRequestUuid());
					while (count < tasks.size())
					{
						Pair<List<JPPFTask>, Integer> p = connection.receiveResults(cl);
						int n = p.first().size();
						count += n;
						if (debugEnabled) log.debug("received " + n + " tasks from server" + (n > 0 ? ", first position=" + p.first().get(0).getPosition() : ""));
						TaskResultListener listener = newJob.getResultListener();
						if (listener != null)
						{
							synchronized(listener)
							{
								listener.resultsReceived(new TaskResultEvent(p.first()));
							}
						}
						else log.warn("result listener is null for job " + newJob);
					}
					completed = true;
				}

				if (localEnabled)
				{
					double elapsed = System.nanoTime() - start;
					bundlers[REMOTE].feedback(tasks.size(), elapsed);
				}
				if (connection != null) connection.getTaskServerConnection().setStatus(JPPFClientConnectionStatus.ACTIVE);
			}
			catch(Throwable t)
			{
				log.error(t.getMessage(), t);
				exception = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
				if (job.getResultListener() != null)
				{
					synchronized(job.getResultListener())
					{
						job.getResultListener().resultsReceived(new TaskResultEvent(t));
					}
				}
			}
		}

		/**
		 * Create a new job based on the initial one.
		 * @param job the initial job.
		 * @return a new {@link JPPFJob} with the same characteristics as the initial one, except for the tasks.
		 */
		private JPPFJob createNewJob(JPPFJob job)
		{
			JPPFJob newJob = new JPPFJob(job.getJobUuid());
			newJob.setDataProvider(job.getDataProvider());
			newJob.setJobSLA(job.getJobSLA());
			newJob.setJobMetadata(job.getJobMetadata());
			newJob.setBlocking(job.isBlocking());
			newJob.setResultListener(job.getResultListener());
			newJob.setId(job.getId());
			return newJob;
		}

		/**
		 * Create a task bundle for the specified job.
		 * @param job the job to use as a base.
		 * @return a JPPFTaskBundle instance.
		 */
		private JPPFTaskBundle createBundle(JPPFJob job)
		{
			String requestUuid = job.getJobUuid();
			JPPFTaskBundle bundle = new JPPFTaskBundle();
			bundle.setRequestUuid(requestUuid);
			ClassLoader cl = null;
			ClassLoader oldCl = null;
			if (!job.getTasks().isEmpty())
			{
				JPPFTask task = job.getTasks().get(0);
				cl = task.getClass().getClassLoader();
				connection.getClient().addRequestClassLoader(requestUuid, cl);
				if (log.isDebugEnabled()) log.debug("adding request class loader=" + cl + " for uuid=" + requestUuid);
			}
			return bundle;
		}
	}

	/**
	 * Determine whether local execution is enabled on this client.
	 * @return <code>true</code> if local execution is enabled, <code>false</code> otherwise.
	 */
	public boolean isLocalEnabled()
	{
		synchronized(this)
		{
			return localEnabled;
		}
	}

	/**
	 * Specifiy whether local execution is enabled on this client.
	 * @param localEnabled <code>true</code> to enable local execution, <code>false</code> otherwise
	 */
	public void setLocalEnabled(boolean localEnabled)
	{
		synchronized(this)
		{
			if (localEnabled && !this.localEnabled) initLocal();
			this.localEnabled = localEnabled;
		}
	}

	/**
	 * Determine whether this load balancer is currently executing a job locally.
	 * @return <code>true</code> if a local job is being executed, <code>false</code> otherwise.
	 */
	public boolean isLocallyExecuting()
	{
		synchronized(getAvailableConnectionLock())
		{
			return locallyExecuting.get();
		}
	}

	/**
	 * Specify whether this load balancer is currently executing a job locally.
	 * @param locallyExecuting <code>true</code> if a local job is being executed, <code>false</code> otherwise.
	 */
	public void setLocallyExecuting(boolean locallyExecuting)
	{
		synchronized(getAvailableConnectionLock())
		{
			this.locallyExecuting.set(locallyExecuting);
			if (debugEnabled) log.debug("set locallyExecuting to " + locallyExecuting);
		}
	}

	/**
	 * Lock used when determining if a job can be executed immediately.
	 * @return an <code>Object</code> instance.
	 */
	public Object getAvailableConnectionLock()
	{
		return availableConnectionLock;
	}
}
