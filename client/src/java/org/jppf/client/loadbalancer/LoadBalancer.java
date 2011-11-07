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
import org.jppf.client.event.JobEvent.Type;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.server.scheduler.bundle.proportional.ProportionalTuneProfile;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class is used to balance the number of tasks in an execution between local and remote execution.
 * It uses the proportional bundling algorithm, which is also used by the JPPF Driver.
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
	static final int LOCAL = 0;
	/**
	 * Index for remote bundler.
	 */
	static final int REMOTE = 1;
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
	 * @param jobSubmission the job submission to execute.
	 * @param connection the client connection for sending remote execution requests.
	 * @param localJob determines whether the job will be executed locally, at least partially.
	 * @throws Exception if an error is raised during execution.
	 */
	public void execute(final JobSubmission jobSubmission, final AbstractJPPFClientConnection connection, final boolean localJob) throws Exception
	{
		//if (isLocalEnabled() && !locallyExecuting.get())
		JPPFJob job = jobSubmission.getJob();
		if (isLocalEnabled() && localJob)
		{
			List<JPPFTask> tasks = job.getPendingTasks();
			if (connection != null)
			{
				if (debugEnabled) log.debug("mixed local and remote execution for job '" + job.getName() + '\'');
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
				if (debugEnabled) log.debug("bundlers[local=" + bundleSize[LOCAL] + ", remote=" + bundleSize[REMOTE] + "]");
				List<List<JPPFTask>> list = new ArrayList<List<JPPFTask>>(REMOTE - LOCAL + 1);
				int idx = 0;
				for (int i=LOCAL; i<=REMOTE; i++)
				{
					list.add(CollectionUtils.getAllElements(tasks, idx, bundleSize[i]));
					idx += bundleSize[i];
				}
				ExecutionThread[] threads = { new LocalExecutionThread(list.get(LOCAL), job, this), new RemoteExecutionThread(list.get(REMOTE), job, connection, this) };
				for (int i=LOCAL; i<=REMOTE; i++) threads[i].setContextClassLoader(Thread.currentThread().getContextClassLoader());
				job.fireJobEvent(Type.JOB_START);
				for (int i=LOCAL; i<=REMOTE; i++) threads[i].start();
				for (int i=LOCAL; i<=REMOTE; i++) threads[i].join();
				job.fireJobEvent(Type.JOB_END);
				for (int i=LOCAL; i<=REMOTE; i++) if (threads[i].getException() != null) throw threads[i].getException();
			}
			else
			{
				if (debugEnabled) log.debug("purely local execution for job '" + job.getName() + '\'');
				ExecutionThread localThread = new LocalExecutionThread(tasks, job, this);
				job.fireJobEvent(Type.JOB_START);
				localThread.run();
				job.fireJobEvent(Type.JOB_END);
				if (localThread.getException() != null) throw localThread.getException();
			}
		}
		else if (connection != null)
		{
			if (debugEnabled) log.debug("purely remote execution for job '" + job.getName() + '\'');
			ExecutionThread remoteThread = new RemoteExecutionThread(job.getPendingTasks(), job, connection, this);
			job.fireJobEvent(Type.JOB_START);
			remoteThread.run();
			job.fireJobEvent(Type.JOB_END);
			if (remoteThread.getException() != null) throw remoteThread.getException();
		}
		else throw new JPPFException("Null driver connection and local executor is "  + (localEnabled ? "busy" : "disabled"));
	}

	/**
	 * Compute the sum of the elements of an int array.
	 * @param array the input array.
	 * @return the result sum as an int value.
	 */
	private int sum(final int[] array)
	{
		int sum = 0;
		for (int anArray : array) sum += anArray;
		return sum;
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
	 * Specify whether local execution is enabled on this client.
	 * @param localEnabled <code>true</code> to enable local execution, <code>false</code> otherwise
	 */
	public void setLocalEnabled(final boolean localEnabled)
	{
		synchronized(this)
		{
			if (localEnabled && !this.localEnabled && (threadPool == null)) initLocal();
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
	public void setLocallyExecuting(final boolean locallyExecuting)
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

	/**
	 * Get the thread pool for local execution.
	 * @return an {@link ExecutorService} instance.
	 */
	public ExecutorService getThreadPool()
	{
		return threadPool;
	}

	/**
	 * Get the bundlers used to split the tasks between local and remote execution.
	 * @return an array of {@link Bundler} instances.
	 */
	public Bundler[] getBundlers()
	{
		return bundlers;
	}
}
