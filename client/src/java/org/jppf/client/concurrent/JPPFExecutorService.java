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
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Implementation of an {@link ExecutorService} wrapper around a {@link JPPFClient}.
 * <p>This executor has two modes in which it functions:
 * <p>1) Standard mode: in this mode each task or set of tasks submitted via one of the
 * <code>invokeXXX()</code> or <code>submit()</code> methods is sent immediately to the server
 * in its own JPPF job.
 * <p>2) Batch mode: the <code>JPPFExecutorService</code> can be configured to only send tasks to the server
 * when a number of tasks, submitted via one of the <code>invokeXXX()</code> or <code>submit()</code> methods,
 * has been reached, or when a timeout specified in milliseconds has expired, or a combination of both.<br/>
 * This facility is designed to optimize the task execution throughput, especially when many individual tasks are submitted
 * using one of the <code>submit()</code> methods. This way, the tasks are sent to the server as a single job,
 * instead of one job per task, and the execution will fully benefit from the parallel features of the JPPF server, including
 * scheduling, load-balancing and parallel I/O.
 * <p>In batch mode, the following behavior is to be noted:
 * <ul>
 * <li>If both size-based and time-based batching are used, tasks will be sent whenever one of the two thresholds is reached.
 * Whenever this happens, both counters are reset. For instance, if the size-based threshold is reached, then the time-based counter
 * will be reset as well, and the timeout counting will start from 0 again</li>
 * <li>When a collection of tasks is submitted via one of the <code>invokeXXX()</code> methods, they are guaranteed
 * to be all sent together in the same JPPF job. This is the one exception to the batch size threshold.</li>
 * <li>If one of the treshold is changed while tasks are still pending execution, the behavior is unspecified</li>
 * </ul>
 * @see org.jppf.client.concurrent.JPPFExecutorService#setBatchSize(int)
 * @see org.jppf.client.concurrent.JPPFExecutorService#setBatchTimeout(long)
 * @author Laurent Cohen
 */
public class JPPFExecutorService implements ExecutorService, FutureResultCollectorListener
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFExecutorService.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The {@link JPPFClient} to which tasks executions are delegated.
	 */
	private JPPFClient client = null;
	/**
	 * Maintains a list of the jobs submitted by this executor.
	 */
	private Map<String, JPPFJob> jobMap = new Hashtable<String, JPPFJob>();
	/**
	 * Determines whether a shutdown has been requested.
	 */
	private AtomicBoolean shuttingDown = new AtomicBoolean(false);
	/**
	 * Determines whether this executor has been terminated.
	 */
	private AtomicBoolean terminated = new AtomicBoolean(false);
	/**
	 * Handles the batching of tasks.
	 */
	private BatchHandler batchHandler = null;

	/**
	 * Initialize this executor service with the specified JPPF client.
	 * @param client the {@link JPPFClient} to use for job submission.
	 */
	public JPPFExecutorService(JPPFClient client)
	{
		this.client = client;
		batchHandler = new BatchHandler(this);
		new Thread(batchHandler, "BatchHandler").start();
	}

	/**
	 * Executes the given tasks, returning a list of Futures holding their status and results when all complete.
	 * @param <T> the type of results returned by the tasks.
	 * @param tasks the tasks to execute.
	 * @return a list of Futures representing the tasks, in the same sequential order as produced by the 
	 * iterator for the given task list, each of which has completed. 
	 * @throws InterruptedException if interrupted while waiting, in which case unfinished tasks are cancelled.
	 * @throws NullPointerException if tasks or any of its elements are null.
	 * @throws RejectedExecutionException if any task cannot be scheduled for execution.
	 * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection)
	 */
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException
	{
		return invokeAll(tasks, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	}

	/**
	 * Executes the given tasks, returning a list of Futures holding their status and results 
	 * when all complete or the timeout expires, whichever happens first.
	 * @param <T> the type of results returned by the tasks.
	 * @param tasks the tasks to execute.
	 * @param timeout the maximum time to wait.
	 * @param unit the time unit of the timeout argument.
	 * @return a list of Futures representing the tasks, in the same sequential order as produced by the 
	 * iterator for the given task list, each of which has completed. 
	 * @throws InterruptedException if interrupted while waiting, in which case unfinished tasks are cancelled.
	 * @throws NullPointerException if tasks or any of its elements are null.
	 * @throws RejectedExecutionException if any task cannot be scheduled for execution.
	 * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException
	{
		if (shuttingDown.get()) throw new RejectedExecutionException("Shutdown has already been requested");
		if (timeout < 0) throw new IllegalArgumentException("timeout cannot be negative");
		long start = System.currentTimeMillis();
		long millis = TimeUnit.MILLISECONDS.equals(unit) ? timeout : DateTimeUtils.toMillis(timeout, unit);
		if (debugEnabled) log.debug("timeout in millis: " + millis);
		Pair<FutureResultCollector, Integer> pair = batchHandler.addTasks(tasks);
		FutureResultCollector collector = pair.first();
		int position = pair.second();
		List<Future<T>> futureList = new ArrayList<Future<T>>(tasks.size());
		for (Callable<T> task: tasks)
		{
			if (task == null) throw new NullPointerException("a task cannot be null");
			JPPFTaskFuture future = new JPPFTaskFuture<T>(collector, position);
			futureList.add(future);
			long elapsed = System.currentTimeMillis() - start;
			future.getResult(millis - elapsed);
			position++;
		}
		return futureList;
	}

	/**
	 * Ensure that all futures in the specified list that have not completed are marked as cancelled.
	 * @param <T> the type of results held by each future.
	 * @param futureList the list of futures to handle.
	 */
	private <T> void handleFutureList(List<Future<T>> futureList)
	{
		for (Future<T> f: futureList)
		{
			if (!f.isDone())
			{
				JPPFTaskFuture<T> future = (JPPFTaskFuture<T>) f;
				future.setDone();
				future.setCancelled();
			}
		}
	}

	/**
	 * Executes the given tasks, returning the result of one that has completed successfully (i.e., without throwing an exception), if any do.
	 * Upon normal or exceptional return, tasks that have not completed are cancelled.
	 * @param <T> the type of results returned by the tasks.
	 * @param tasks the tasks to execute.
	 * @return the result returned by one of the tasks.
	 * @throws InterruptedException if interrupted while waiting. 
	 * @throws NullPointerException if tasks or any of its elements are null. 
	 * @throws IllegalArgumentException if tasks empty.
	 * @throws ExecutionException if no task successfully completes.
	 * @throws RejectedExecutionException if tasks cannot be scheduled for execution.
	 * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection)
	 */
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException
	{
		try
		{
			return invokeAny(tasks, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		}
		catch(TimeoutException e)
		{
			return null;
		}
	}

	/**
	 * Executes the given tasks, returning the result of one that has completed successfully (i.e., without throwing an exception),
	 * if any do before the given timeout elapses. Upon normal or exceptional return, tasks that have not completed are cancelled.
	 * @param <T> the type of results returned by the tasks.
	 * @param tasks the tasks to execute.
	 * @param timeout the maximum time to wait.
	 * @param unit the time unit of the timeout argument.
	 * @return the result returned by one of the tasks.
	 * @throws InterruptedException if interrupted while waiting. 
	 * @throws NullPointerException if tasks or any of its elements are null. 
	 * @throws IllegalArgumentException if tasks empty.
	 * @throws ExecutionException if no task successfully completes.
	 * @throws RejectedExecutionException if tasks cannot be scheduled for execution.
	 * @throws TimeoutException if the given timeout elapses before any task successfully completes.
	 * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection)
	 * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException
	{
		List<Future<T>> futureList = invokeAll(tasks, timeout, unit);
		handleFutureList(futureList);
		for (Future<T> f: futureList)
		{
			if (f.isDone() && !f.isCancelled()) return f.get();
		}
		return null;
	}

	/**
	 * Submit a value-returning task for execution and returns a Future representing the pending results of the task. 
	 * @param <T> the type of result returned by the task.
	 * @param task the task to execute.
	 * @return a Future representing pending completion of the task.
	 * @see java.util.concurrent.ExecutorService#submit(java.util.concurrent.Callable)
	 */
	public <T> Future<T> submit(Callable<T> task)
	{
		if (shuttingDown.get()) throw new RejectedExecutionException("Shutdown has already been requested");
		return batchHandler.addTask(task);
	}

	/**
	 * Submits a Runnable task for execution and returns a Future representing that task.
	 * @param task the task to execute.
	 * @return a future representing the status of the task completion.
	 * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable)
	 */
	public Future<?> submit(Runnable task)
	{
		if (task instanceof JPPFTask)
		{
			if (shuttingDown.get()) throw new RejectedExecutionException("Shutdown has already been requested");
			return batchHandler.addTask((JPPFTask) task);
		}
		return submit(new RunnableWrapper<Object>(task, null));
	}

	/**
	 * Submits a Runnable task for execution and returns a Future representing that task that will upon completion return the given result.
	 * @param <T> the type of result returned by the task.
	 * @param task the task to execute.
	 * @param result the result to return .
	 * @return a Future representing pending completion of the task, and whose get() method will return the given result upon completion. 
	 * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable, java.lang.Object)
	 */
	public <T> Future<T> submit(Runnable task, T result)
	{
		return submit(new RunnableWrapper<T>(task, result));
	}

	/**
	 * Executes the given command at some time in the future.
	 * The command may execute in a new thread, in a pooled thread, or in the calling thread, at the discretion of the Executor implementation. 
	 * @param command the command to execute.
	 * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
	 */
	public void execute(Runnable command)
	{
		submit(command);
	}

	/**
	 * Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs,
	 * or the current thread is interrupted, whichever happens first.
	 * @param timeout the maximum time to wait.
	 * @param unit the time unit of the timeout argument.
	 * @return true if this executor terminated and false if the timeout elapsed before termination.
	 * @throws InterruptedException if interrupted while waiting.
	 * @see java.util.concurrent.ExecutorService#awaitTermination(long, java.util.concurrent.TimeUnit)
	 */
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
	{
		long millis = DateTimeUtils.toMillis(timeout, unit);
		waitForTerminated(millis);
		return isTerminated();
	}

	/**
	 * Determine whether this executor has been shut down.
	 * @return true if this executor has been shut down, false otherwise.
	 * @see java.util.concurrent.ExecutorService#isShutdown()
	 */
	public boolean isShutdown()
	{
		return shuttingDown.get();
	}

	/**
	 * Determine whether all tasks have completed following shut down.
	 * Note that isTerminated is never true unless either shutdown or shutdownNow was called first. 
	 * @return true if all tasks have completed following shut down.
	 * @see java.util.concurrent.ExecutorService#isTerminated()
	 */
	public boolean isTerminated()
	{
		return terminated.get();
	}

	/**
	 * Set the terminated status for this executor.
	 * @see java.util.concurrent.ExecutorService#isTerminated()
	 */
	private void setTerminated()
	{
		terminated.set(true);
		synchronized(this)
		{
			notifyAll();
		}
	}

	/**
	 * Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 */
	public void shutdown()
	{
		shuttingDown.set(true);
		synchronized(jobMap)
		{
			if (debugEnabled) log.debug("normal shutdown requested, " + jobMap.size() + " jobs pending");
			terminated.compareAndSet(false, jobMap.isEmpty());
		}
		batchHandler.close();
	}

	/**
	 * Attempts to stop all actively executing tasks, halts the processing of waiting tasks,
	 * and returns a list of the tasks that were awaiting execution.<br>
	 * This implementation simply waits for all submitted tasks to terminate, due to the complexity of stopping remote tasks.
	 * @return a list of tasks that never commenced execution. 
	 * @see java.util.concurrent.ExecutorService#shutdownNow()
	 */
	public List<Runnable> shutdownNow()
	{
		shuttingDown.set(true);
		synchronized(jobMap)
		{
			if (debugEnabled) log.debug("immediate shutdown requested, " + jobMap.size() + " jobs pending");
			//terminated.compareAndSet(false, jobMap.isEmpty());
			jobMap.clear();
		}
		setTerminated();
		batchHandler.close();
		waitForTerminated(Long.MAX_VALUE);
		return null;
	}

	/**
	 * Submit the specified job for execution on the grid.
	 * @param job the job to submit.
	 * @throws Exception if any error occurs.
	 */
	void submitJob(JPPFJob job) throws Exception
	{
		if (debugEnabled) log.debug("submitting job '" + job.getId() + "' with " + job.getTasks().size() + " tasks");
		client.submit(job);
		synchronized(jobMap)
		{
			jobMap.put(job.getJobUuid(), job);
		}
	}

	/**
	 * Wait until this executor has terminated, or the specified timeout has expired, whichever happens first.
	 * @param timeout the maximum time to wait, zero means indefinite time.
	 */
	private void waitForTerminated(long timeout)
	{
		long elapsed = 0L;
		long start = System.currentTimeMillis();
		while (!isTerminated() && (elapsed < timeout))
		{
			synchronized (this)
			{
				try
				{
					wait(timeout - elapsed);
				}
				catch(InterruptedException e)
				{
					log.error(e.getMessage(), e);
				}
				elapsed = System.currentTimeMillis() - start;
			}
		}
	}

	/**
	 * Called when all results from a job have been received.
	 * @param event the event object.
	 * @see org.jppf.client.concurrent.FutureResultCollectorListener#resultsComplete(org.jppf.client.concurrent.FutureResultCollectorEvent)
	 */
	public void resultsComplete(FutureResultCollectorEvent event)
	{
		String jobUuid = event.getCollector().getJobUuid();
		synchronized(jobMap)
		{
			jobMap.remove(jobUuid);
			if (isShutdown() && jobMap.isEmpty()) setTerminated();
		}
	}

	/**
	 * Get the minimum number of tasks that must be submitted before they are sent to the server.
	 * @return the batch size as an int.
	 */
	public int getBatchSize()
	{
		return batchHandler.getBatchSize();
	}

	/**
	 * Set the minimum number of tasks that must be submitted before they are sent to the server.
	 * @param batchSize the batch size as an int.
	 */
	public void setBatchSize(int batchSize)
	{
		batchHandler.setBatchSize(batchSize);
	}

	/**
	 * Get the maximum time to wait before the next batch of tasks is to be sent for execution.
	 * @return the timeout as a long.
	 */
	public long getBatchTimeout()
	{
		return batchHandler.getBatchTimeout();
	}

	/**
	 * Set the maximum time to wait before the next batch of tasks is to be sent for execution.
	 * @param batchTimeout the timeout as a long.
	 */
	public void setBatchTimeout(long batchTimeout)
	{
		batchHandler.setBatchTimeout(batchTimeout);
	}
}
