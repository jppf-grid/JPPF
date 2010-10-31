/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

import org.jppf.JPPFException;
import org.jppf.client.JPPFJob;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
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
	 * 
	 */
	private AtomicReference<JPPFJob> currentJobRef = new AtomicReference<JPPFJob>(null);
	/**
	 * The time at which we started to count for the the timeout.
	 */
	private long start = 0L;
	/**
	 * Time elapsed since the start.
	 */
	private long elapsed = 0L;

	/**
	 * Default constructor.
	 * @param executor the JPPFExecutorService whose tasks are batched.
	 */
	BatchHandler(JPPFExecutorService executor)
	{
		this.executor = executor;
		currentJobRef.set(createJob());
	}

	/**
	 * Get the minimum number of tasks that must be submitted before they are sent to the server.
	 * @return the batch size as an int.
	 */
	int getBatchSize()
	{
		return batchSize;
	}

	/**
	 * Set the minimum number of tasks that must be submitted before they are sent to the server.
	 * @param batchSize the batch size as an int.
	 */
	void setBatchSize(int batchSize)
	{
		this.batchSize = batchSize;
	}

	/**
	 * Get the maximum time to wait before the next batch of tasks is to be sent for execution.
	 * @return the timeout as a long.
	 */
	long getBatchTimeout()
	{
		return batchTimeout;
	}

	/**
	 * Set the maximum time to wait before the next batch of tasks is to be sent for execution.
	 * @param batchTimeout the timeout as a long.
	 */
	void setBatchTimeout(long batchTimeout)
	{
		this.batchTimeout = batchTimeout;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		JPPFJob job = null;
		while (!isStopped())
		{
			while (!isStopped() && ((job = nextJob()) == null))
			{
				if (batchTimeout > 0)
				{
					long n = batchTimeout - elapsed;
					if (n < 1L) n = 1L;
					goToSleep(n);
				}
				else goToSleep();
				elapsed = System.currentTimeMillis() - start;
			}
			if (isStopped()) break;
			try
			{
				currentJobRef.set(createJob());
				if (debugEnabled) log.debug("submitting job " + job.getId());
				executor.submitJob(job);
			}
			catch (Exception e)
			{
				log.error(e.getMessage(), e);
			}
			elapsed = System.currentTimeMillis() - start;
		}
	}

	/**
	 * Return the next job to submit if one is ready.
	 * @return a JPPFJob instance, or null if no job is ready to be sent.
	 */
	private JPPFJob nextJob()
	{
		JPPFJob job = currentJobRef.get();
		if (job.getTasks() == null) return null;
		int size = job.getTasks().size();
		if (size == 0) return null;
		boolean ok = false;
		if (((batchTimeout > 0L) && (elapsed >= batchTimeout)) ||
				((batchSize > 0) && (size >= batchSize)) ||
				((batchSize <= 0) && (batchTimeout <= 0L)))
		{
			synchronized(this)
			{
				ok = true;
				start = System.currentTimeMillis();
				elapsed = 0;
			}
		}
		return ok ? job : null;
	}

	/**
	 * Submit a task for execution.
	 * @param <T> the type of results.
	 * @param task the task to submit.
	 * @return a {@link Future} representing pending completion of the task.
	 */
	<T> Future<T> addTask(Callable<T> task)
	{
		if (debugEnabled) log.debug("submitting one Callable Task");
		JPPFJob job = currentJobRef.get();
		Future<T> future = null;
		synchronized(this)
		{
			try
			{
				FutureResultCollector collector = (FutureResultCollector) job.getResultListener();
				JPPFTask jppfTask = job.addTask(task);
				future = new JPPFTaskFuture<T>(collector, jppfTask.getPosition());
			}
			catch (JPPFException e)
			{
				log.error(e.getMessage(), e);
			}
			wakeUp();
		}
		return future;
	}

	/**
	 * Submit a task for execution.
	 * @param task the task to submit.
	 * @return a {@link Future} representing pending completion of the task.
	 */
	Future<Object> addTask(JPPFTask task)
	{
		if (debugEnabled) log.debug("submitting one JPPFTask");
		JPPFJob job = currentJobRef.get();
		Future<Object> future = null;
		synchronized(this)
		{
			try
			{
				job.addTask(task);
				FutureResultCollector collector = (FutureResultCollector) job.getResultListener();
				future = new JPPFTaskFuture<Object>(collector, task.getPosition());
			}
			catch (JPPFException e)
			{
				log.error(e.getMessage(), e);
			}
			wakeUp();
		}
		return future;
	}

	/**
	 * Submit a list of tasks for execution.
	 * @param <T> the type of the results.
	 * @param tasks the tasks to submit.
	 * @return a pair representing the reuslt collector used in the current job, along with the position of the first task. 
	 */
	<T> Pair<FutureResultCollector, Integer> addTasks(Collection<? extends Callable<T>> tasks)
	{
		if (debugEnabled) log.debug("submitting " + tasks.size() + " Callable Tasks");
		Pair<FutureResultCollector, Integer> pair = null;
		synchronized(this)
		{
			JPPFJob job = currentJobRef.get();
			FutureResultCollector collector = (FutureResultCollector) job.getResultListener();
			try
			{
				int start = job.getTasks().size();
				for (Callable<?> task: tasks) job.addTask(task);
			}
			catch (JPPFException e)
			{
				log.error(e.getMessage(), e);
			}
			pair = new Pair(collector, start);
			wakeUp();
		}
		return pair;
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
}
