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

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.DateTimeUtils;

/**
 * Implementatation of an {@link ExecutorService} wrapper around a {@link JPPFClient}.
 * @author Laurent Cohen
 */
public class JPPFExecutorService implements ExecutorService, FutureResultCollectorListener
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFExecutorService.class);
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
	 * Count of jobs created by this executor service.
	 */
	private static AtomicLong jobCount = new AtomicLong(0);

	/**
	 * Initialize this executor service with the specified JPPF client.
	 * @param client the {@link JPPFClient} to use for job submission.
	 */
	public JPPFExecutorService(JPPFClient client)
	{
		this.client = client;
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
		return invokeAll(tasks, 0L, TimeUnit.MILLISECONDS);
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
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException
	{
		if (shuttingDown.get()) throw new RejectedExecutionException("Shutdown has already been requested");
		if (timeout < 0) throw new IllegalArgumentException("timeout cannot be negative");
		long start = System.currentTimeMillis();
		long millis = DateTimeUtils.toMillis(timeout, unit);
		if (debugEnabled) log.debug("timeout in millis: " + millis);
		JPPFJob job = createJob();
		FutureResultCollector collector = (FutureResultCollector) job.getResultListener();
		List<Future<T>> futureList = new ArrayList<Future<T>>(tasks.size());
		try
		{
			int position = 0;
			for (Callable<T> task: tasks)
			{
				if (task == null) throw new NullPointerException("a task cannot be null");
				JPPFTask jppfTask = addToJob(job, task);
				futureList.add(new JPPFTaskFuture<T>(collector, position++));
			}
			collector.setTaskCount(job.getTasks().size());
			submitJob(job);
			long elapsed = System.currentTimeMillis() - start;
			if ((millis == 0) || (elapsed < millis)) collector.waitForResults(millis == 0 ? 0 : millis - elapsed);
			elapsed = System.currentTimeMillis() - start;
			if (debugEnabled) log.debug("elapsed=" + elapsed);
			handleFutureList(futureList);
		}
		catch(InterruptedException e)
		{
			handleFutureList(futureList);
			throw e;
		}
		catch(Exception e)
		{
			throw new RejectedExecutionException(e);
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
			return invokeAny(tasks, 0L, TimeUnit.MILLISECONDS);
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
	 * @throws TimeoutException - if the given timeout elapses before any task successfully completes.
	 * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection)
	 * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException
	{
		List<Future<T>> futureList = invokeAll(tasks, timeout, unit);
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
		JPPFJob job = createJob();
		Future<T> future = null;
		try
		{
			JPPFTask jppfTask = addToJob(job, task);
			FutureResultCollector collector = (FutureResultCollector) job.getResultListener();
			future = new JPPFTaskFuture<T>(collector, jppfTask.getPosition());
			submitJob(job);
		}
		catch(Exception e)
		{
			throw new RejectedExecutionException(e);
		}
		return future;
	}

	/**
	 * Submits a Runnable task for execution and returns a Future representing that task.
	 * @param task the task to execute.
	 * @return a future representing the status of the task completion.
	 * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable)
	 */
	public Future<?> submit(Runnable task)
	{
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
		return terminated.get();
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
		terminated.compareAndSet(false, jobMap.isEmpty());
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
		terminated.compareAndSet(false, jobMap.isEmpty());
		waitForTerminated(0L);
		return null;
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
		collector.addListener(this);
		return job;
	}

	/**
	 * Add the specified task to the specified job.
	 * @param job the job to add a task to.
	 * @param object the task to add to the job.
	 * @return the task eventually wrapped in a {@link JPPFTask} instance.
	 * @throws JPPFException if any error occurs.
	 */
	private JPPFTask addToJob(JPPFJob job, Object object) throws JPPFException
	{
		return job.addTask(object);
	}

	/**
	 * Submit the specified job for execution on the grid.
	 * @param job the job to submit.
	 * @throws Exception if any error occurs.
	 */
	private void submitJob(JPPFJob job) throws Exception
	{
		client.submit(job);
		jobMap.put(job.getJobUuid(), job);
	}

	/**
	 * Wait until this executor has terminated, or the specified timeout has expired, whichever happens first.
	 * @param timeout the maximum time to wait, zero means indefinite time.
	 */
	private void waitForTerminated(long timeout)
	{
		long elapsed = 0L;
		long start = System.currentTimeMillis();
		while (!terminated.get() && ((timeout == 0L) || (elapsed < timeout)))
		{
			synchronized (this)
			{
				try
				{
					wait(timeout == 0L ? 0L : timeout - elapsed);
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
	 * Callable wrapper around a Runnable.
	 * @param <V> the type of result.
	 */
	private static class RunnableWrapper<V> implements Callable<V>, Serializable
	{
		/**
		 * The runnable to execute.
		 */
		private Runnable runnable = null;
		/**
		 * The result to return.
		 */
		private V result = null;

		/**
		 * Initialize this callable with the specified parameters.
		 * @param runnable the runnable to execute.
		 * @param result he result to return.
		 */
		public RunnableWrapper(Runnable runnable, V result)
		{
			this.runnable = runnable;
			this.result = result;
		}

		/**
		 * Execute the task.
		 * @return the result specified in the constructor.
		 * @see java.util.concurrent.Callable#call()
		 */
		public V call()
		{
			runnable.run();
			return result;
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
		jobMap.remove(jobUuid);
		if (shuttingDown.get() && jobMap.isEmpty()) setTerminated();
	}
}
