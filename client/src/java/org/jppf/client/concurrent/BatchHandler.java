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

package org.jppf.client.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import org.jppf.JPPFException;
import org.jppf.client.JPPFJob;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class is a processor for tasks submitted via a {@link JPPFExecutorService}.
 * It handles both normal mode and batching mode, where the tasks throughput is streamlined
 * by specifying how many tasks should be sent to the grid, and a which intervals.
 * @author Laurent Cohen
 */
public class BatchHandler extends ThreadSynchronization implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(BatchHandler.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Count of jobs created by this executor service.
	 */
	private static AtomicLong jobCount = new AtomicLong(0);
	/**
	 * The minimum number of tasks that must be submitted before they are sent to the server.
	 */
	private int batchSize = 0;
	/**
	 * The maximum time to wait before the next batch of tasks is to be sent for execution.
	 */
	private long batchTimeout = 0L;
	/**
	 * The JPPFExecutorService whose tasks are batched.
	 */
	private JPPFExecutorService executor = null;
	/**
	 * The job to send for execution. If the reference is ont null, then the job is sent immediately.
	 */
	private AtomicReference<JPPFJob> currentJobRef = new AtomicReference<JPPFJob>(null);
	/**
	 * The next job being prepared. It will be assigned to <code>currentJobRef</code> when it is ready for execution,
	 * depending on the batching parameters.
	 */
	private AtomicReference<JPPFJob> nextJobRef = new AtomicReference<JPPFJob>(null);
	/**
	 * The time at which we started to count for the the timeout.
	 */
	private long start = 0L;
	/**
	 * Time elapsed since the start.
	 */
	private long elapsed = 0L;
	/**
	 * Used to synchronize access to <code>currentJobRef</code> and <code>nextJobRef</code>
	 */
	private ReentrantLock lock = new ReentrantLock(true);
	/**
	 * Represents a condtion to await for and corresponding to when <code>currentJobRef</code> is not null.
	 */
	private Condition jobReady = lock.newCondition();

	/**
	 * Default constructor.
	 * @param executor the JPPFExecutorService whose tasks are batched.
	 */
	BatchHandler(JPPFExecutorService executor)
	{
		this.executor = executor;
		nextJobRef.set(createJob());
	}

	/**
	 * Get the minimum number of tasks that must be submitted before they are sent to the server.
	 * @return the batch size as an int.
	 */
	synchronized int getBatchSize()
	{
		return batchSize;
	}

	/**
	 * Set the minimum number of tasks that must be submitted before they are sent to the server.
	 * @param batchSize the batch size as an int.
	 */
	synchronized void setBatchSize(int batchSize)
	{
		this.batchSize = batchSize;
	}

	/**
	 * Get the maximum time to wait before the next batch of tasks is to be sent for execution.
	 * @return the timeout as a long.
	 */
	synchronized long getBatchTimeout()
	{
		return batchTimeout;
	}

	/**
	 * Set the maximum time to wait before the next batch of tasks is to be sent for execution.
	 * @param batchTimeout the timeout as a long.
	 */
	synchronized void setBatchTimeout(long batchTimeout)
	{
		this.batchTimeout = batchTimeout;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		start = System.currentTimeMillis();
		while (!isStopped())
		{
			try
			{
				lock.lock();
				try
				{
					while (!isStopped() && (currentJobRef.get() == null))
					{
						if (batchTimeout > 0)
						{
							long n = batchTimeout - elapsed;
							if (n > 0) jobReady.await(n, TimeUnit.MILLISECONDS);
						}
						else jobReady.await();
						updateNextJob(false);
					}
					if (isStopped()) break;
					JPPFJob job = currentJobRef.get();
					if (debugEnabled) log.debug("submitting job " + job.getId() + " with " + job.getTasks().size() + " tasks");
					FutureResultCollector collector = (FutureResultCollector) job.getResultListener();
					collector.setTaskCount(job.getTasks().size());
					executor.submitJob(job);
					currentJobRef.set(null);
					elapsed = System.currentTimeMillis() - start;
				}
				finally
				{
					lock.unlock();
				}
			}
			catch (Exception e)
			{
				log.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Update the next job to submit if one is ready.
	 * @param sendSignal true if signal is to be sent, false otherwise.
	 */
	private void updateNextJob(boolean sendSignal)
	{
		JPPFJob job = nextJobRef.get();
		int size = 0;
		if (job.getTasks() == null) size = 0;
		else size = job.getTasks().size();
		if (batchTimeout > 0L) elapsed = System.currentTimeMillis() - start;
		if (size == 0)
		{
			if ((batchTimeout > 0L) && (elapsed >= batchTimeout)) resetTimeout();
			return;
		}
		if (((batchTimeout > 0L) && (elapsed >= batchTimeout)) ||
				((batchSize > 0) && (size >= batchSize)) ||
				((batchSize <= 0) && (batchTimeout <= 0L)))
		{
			currentJobRef.set(job);
			nextJobRef.set(createJob());
			resetTimeout();
			if (sendSignal) jobReady.signal();
		}
	}

	/**
	 * Reset the timeout counter.
	 */
	private void resetTimeout()
	{
		start = System.currentTimeMillis();
		elapsed = 0L;
	}

	/**
	 * Submit a task for execution.
	 * @param task the task to submit.
	 * @return a {@link Future} representing pending completion of the task.
	 */
	Future<Object> addTask(JPPFTask task)
	{
		lock.lock();
		try
		{
			if (debugEnabled) log.debug("submitting one JPPFTask");
			Future<Object> future = null;
			JPPFJob job = nextJobRef.get();
			try
			{
				FutureResultCollector collector = (FutureResultCollector) job.getResultListener();
				job.addTask(task);
				future = new JPPFTaskFuture<Object>(collector, task.getPosition());
			}
			catch (JPPFException e)
			{
				log.error(e.getMessage(), e);
				throw new RejectedExecutionException(e);
			}
			updateNextJob(true);
			return future;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Submit a task for execution.
	 * @param <T> the type of results.
	 * @param task the task to submit.
	 * @return a {@link Future} representing pending completion of the task.
	 */
	<T> Future<T> addTask(Callable<T> task)
	{
		lock.lock();
		try
		{
			if (debugEnabled) log.debug("submitting one Callable Task");
			Future<T> future = null;
			JPPFJob job = nextJobRef.get();
			try
			{
				FutureResultCollector collector = (FutureResultCollector) job.getResultListener();
				JPPFTask jppfTask = job.addTask(task);
				future = new JPPFTaskFuture<T>(collector, jppfTask.getPosition());
			}
			catch (JPPFException e)
			{
				log.error(e.getMessage(), e);
				throw new RejectedExecutionException(e);
			}
			updateNextJob(true);
			return future;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Submit a list of tasks for execution.
	 * @param <T> the type of the results.
	 * @param tasks the tasks to submit.
	 * @return a pair representing the result collector used in the current job, along with the position of the first task. 
	 */
  @SuppressWarnings("unchecked")
	<T> Pair<FutureResultCollector, Integer> addTasks(Collection<? extends Callable<T>> tasks)
	{
		lock.lock();
		try
		{
			if (debugEnabled) log.debug("submitting " + tasks.size() + " Callable Tasks");
			Pair<FutureResultCollector, Integer> pair = null;
			JPPFJob job = nextJobRef.get();
			FutureResultCollector collector = (FutureResultCollector) job.getResultListener();
			int start = 0;
			try
			{
				List<JPPFTask> jobTasks = job.getTasks();
				start = (jobTasks == null) ? 0 : jobTasks.size();
				for (Callable<?> task: tasks) job.addTask(task);
			}
			catch (JPPFException e)
			{
				log.error(e.getMessage(), e);
				throw new RejectedExecutionException(e);
			}
			pair = new Pair(collector, start);
			updateNextJob(true);
			return pair;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Create a new job with a FutureResultCollector a results listener.
	 * @return a {@link JPPFJob} instance.
	 */
	private JPPFJob createJob()
	{
		JPPFJob job = new JPPFJob();
		job.setId(getClass().getSimpleName() + " job " + jobCount.incrementAndGet());
		FutureResultCollector collector = new FutureResultCollector(0, job.getJobUuid());
		job.setResultListener(collector);
		job.setBlocking(false);
		collector.addListener(executor);
		return job;
	}

	/**
	 * Close this batch handler.
	 */
	void close()
	{
		setStopped(true);
		lock.lock();
		try
		{
			jobReady.signalAll();
		}
		finally
		{
			lock.unlock();
		}
	}
}
