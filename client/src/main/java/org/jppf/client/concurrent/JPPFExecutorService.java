/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
import org.slf4j.*;

/**
 * Implementation of an {@link ExecutorService} wrapper around a {@link JPPFClient}.
 * <p>This executor has two modes in which it functions:
 * <p>1) Standard mode: in this mode each task or set of tasks submitted via one of the
 * <code>invokeXXX()</code> or <code>submit()</code> methods is sent immediately to the server in its own JPPF job.
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
 * <li>If one of the threshold is changed while tasks are still pending execution, the behavior is unspecified</li>
 * </ul>
 * @see org.jppf.client.concurrent.JPPFExecutorService#setBatchSize(int)
 * @see org.jppf.client.concurrent.JPPFExecutorService#setBatchTimeout(long)
 * @author Laurent Cohen
 */
public class JPPFExecutorService extends JobListenerAdapter implements ExecutorService {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFExecutorService.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Count of instances of this class, use to name the batch handler thread.
   */
  private static final AtomicInteger instanceCount = new AtomicInteger(0);
  /**
   * The {@link JPPFClient} to which tasks executions are delegated.
   */
  JPPFClient client;
  /**
   * Maintains a list of the jobs submitted by this executor.
   */
  private final Map<String, JPPFJob> jobMap = new Hashtable<>();
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
  private BatchHandler batchHandler;

  /**
   * Initialize this executor service with the specified JPPF client.
   * @param client the {@link JPPFClient} to use for job submission.
   */
  public JPPFExecutorService(final JPPFClient client) {
    this(client, 0, 0L);
  }

  /**
   * Initialize this executor service with the specified JPPF client, batch size and batch tiemout.
   * @param client the {@link JPPFClient} to use for job submission.
   * @param batchSize the minimum number of tasks that must be submitted before they are sent to the server.
   * @param batchTimeout the maximum time to wait before the next batch of tasks is to be sent for execution.
   */
  public JPPFExecutorService(final JPPFClient client, final int batchSize, final long batchTimeout) {
    if (debugEnabled) log.debug("new {} with batchSize={}, batchTimeout={}, client={}", getClass().getSimpleName(), batchSize, batchTimeout, client);
    this.client = client;
    batchHandler = new BatchHandler(this, batchSize, batchTimeout);
    ThreadUtils.startThread(batchHandler, "BatchHandler-" + instanceCount.incrementAndGet());
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
   */
  @Override
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
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
   */
  @Override
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
    if (shuttingDown.get()) throw new RejectedExecutionException("Shutdown has already been requested");
    if (timeout < 0) throw new IllegalArgumentException("timeout cannot be negative");
    final long start = System.nanoTime();
    final long millis = TimeUnit.MILLISECONDS.equals(unit) ? timeout : DateTimeUtils.toMillis(timeout, unit);
    if (debugEnabled) log.debug("timeout in millis: " + millis);
    final Pair<JPPFJob, Integer> pair = batchHandler.addTasks(tasks);
    final JPPFJob job = pair.first();
    int position = pair.second();
    final List<Future<T>> futureList = new ArrayList<>(tasks.size());
    for (final Callable<T> task : tasks) {
      if (task == null) throw new NullPointerException("a task cannot be null");
      final JPPFTaskFuture<T> future = new JPPFTaskFuture<>(job, position);
      futureList.add(future);
      final long elapsed = (System.nanoTime() - start) / 1_000_000L;
      try {
        future.getResult(millis - elapsed);
      } catch (@SuppressWarnings("unused") final TimeoutException ignore) {
      }
      position++;
    }
    return futureList;
  }

  /**
   * Ensure that all futures in the specified list that have not completed are marked as cancelled.
   * @param <T> the type of results held by each future.
   * @param futureList the list of futures to handle.
   */
  private static <T> void handleFutureList(final List<Future<T>> futureList) {
    for (final Future<T> f : futureList) {
      if (!f.isDone()) {
        final JPPFTaskFuture<T> future = (JPPFTaskFuture<T>) f;
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
   */
  @Override
  public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    try {
      return invokeAny(tasks, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    } catch (@SuppressWarnings("unused") final TimeoutException e) {
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
   */
  @Override
  public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    final List<Future<T>> futureList = invokeAll(tasks, timeout, unit);
    handleFutureList(futureList);
    for (final Future<T> f : futureList) {
      if (f.isDone() && !f.isCancelled()) return f.get();
    }
    return null;
  }

  /**
   * Submit a value-returning task for execution and returns a Future representing the pending results of the task.
   * @param <T> the type of result returned by the task.
   * @param task the task to execute.
   * @return a Future representing pending completion of the task.
   */
  @Override
  public <T> Future<T> submit(final Callable<T> task) {
    if (shuttingDown.get()) throw new RejectedExecutionException("Shutdown has already been requested");
    if (task instanceof Task<?>) return batchHandler.addTask((Task<?>) task, (T) null);
    return batchHandler.addTask(task);
  }

  /**
   * Submits a Runnable task for execution and returns a Future representing that task.
   * @param task the task to execute.
   * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable)
   */
  @Override
  public Future<?> submit(final Runnable task) {
    if (shuttingDown.get()) throw new RejectedExecutionException("Shutdown has already been requested");
    if (task instanceof Task<?>) return batchHandler.addTask((Task<?>) task, (Object) null);
    return batchHandler.addTask(task, (Object) null);
  }

  /**
   * Submits a Runnable task for execution and returns a Future representing that task that will upon completion return the given result.
   * @param <T> the type of result returned by the task.
   * @param task the task to execute.
   * @param result the result to return .
   * @return a Future representing pending completion of the task, and whose get() method will return the given result upon completion.
   */
  @Override
  public <T> Future<T> submit(final Runnable task, final T result) {
    if (shuttingDown.get()) throw new RejectedExecutionException("Shutdown has already been requested");
    if (task instanceof Task<?>) return batchHandler.addTask((Task<?>) task, result);
    return batchHandler.addTask(task, result);
  }

  /**
   * Executes the given command at some time in the future.
   * The command may execute in a new thread, in a pooled thread, or in the calling thread, at the discretion of the Executor implementation.
   * @param command the command to execute.
   * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
   */
  @Override
  public void execute(final Runnable command) {
    submit(command);
  }

  /**
   * Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs,
   * or the current thread is interrupted, whichever happens first.
   * @param timeout the maximum time to wait.
   * @param unit the time unit of the timeout argument.
   * @return true if this executor terminated and false if the timeout elapsed before termination.
   * @throws InterruptedException if interrupted while waiting.
   */
  @Override
  public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
    final long millis = DateTimeUtils.toMillis(timeout, unit);
    waitForTerminated(millis);
    return isTerminated();
  }

  /**
   * Determine whether this executor has been shut down.
   * @return true if this executor has been shut down, false otherwise.
   */
  @Override
  public boolean isShutdown() {
    return shuttingDown.get();
  }

  /**
   * Determine whether all tasks have completed following shut down.
   * Note that isTerminated is never true unless either shutdown or shutdownNow was called first.
   * @return true if all tasks have completed following shut down.
   */
  @Override
  public boolean isTerminated() {
    return terminated.get();
  }

  /**
   * Set the terminated status for this executor.
   */
  private void setTerminated() {
    terminated.set(true);
    synchronized (this) {
      notifyAll();
    }
  }

  /**
   * Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
   */
  @Override
  public void shutdown() {
    shuttingDown.set(true);
    synchronized (jobMap) {
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
   */
  @Override
  public List<Runnable> shutdownNow() {
    shuttingDown.set(true);
    synchronized (jobMap) {
      if (debugEnabled) log.debug("immediate shutdown requested, " + jobMap.size() + " jobs pending");
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
  void submitJob(final JPPFJob job) throws Exception {
    if (debugEnabled) log.debug("submitting job '" + job.getName() + "' with " + job.getJobTasks().size() + " tasks");
    client.submitAsync(job);
    synchronized (jobMap) {
      jobMap.put(job.getUuid(), job);
    }
  }

  /**
   * Wait until this executor has terminated, or the specified timeout has expired, whichever happens first.
   * @param timeout the maximum time to wait, zero means indefinite time.
   */
  private void waitForTerminated(final long timeout) {
    long elapsed = 0L;
    final long maxWait = timeout <= 0L ? Long.MAX_VALUE : timeout;
    final long start = System.nanoTime();
    while (!isTerminated() && (elapsed < maxWait)) {
      synchronized (this) {
        try {
          wait(timeout - elapsed);
        } catch (final InterruptedException e) {
          log.error(e.getMessage(), e);
        }
        elapsed = (System.nanoTime() - start) / 1_000_000L;
      }
    }
  }

  /**
   * Called when all results from a job have been received.
   * @param event the event object.
   * @exclude
   */
  @Override
  public void jobReturned(final JobEvent event) {
    final String jobUuid = event.getJob().getUuid();
    synchronized (jobMap) {
      jobMap.remove(jobUuid);
      if (isShutdown() && jobMap.isEmpty()) setTerminated();
    }
  }

  /**
   * Get the minimum number of tasks that must be submitted before they are sent to the server.
   * @return the batch size as an int.
   */
  public int getBatchSize() {
    return batchHandler.getBatchSize();
  }

  /**
   * Set the minimum number of tasks that must be submitted before they are sent to the server.
   * @param batchSize the batch size as an int.
   * @return this executor service, for method chaining.
   */
  public JPPFExecutorService setBatchSize(final int batchSize) {
    if (debugEnabled) log.debug("setting batchSize = {}", batchSize);
    batchHandler.setBatchSize(batchSize);
    return this;
  }

  /**
   * Get the maximum time to wait before the next batch of tasks is to be sent for execution.
   * @return the timeout as a long.
   */
  public long getBatchTimeout() {
    return batchHandler.getBatchTimeout();
  }

  /**
   * Set the maximum time to wait before the next batch of tasks is to be sent for execution.
   * @param batchTimeout the timeout as a long.
   * @return this executor service, for method chaining.
   */
  public JPPFExecutorService setBatchTimeout(final long batchTimeout) {
    if (debugEnabled) log.debug("setting batchTimeout = {}", batchTimeout);
    batchHandler.setBatchTimeout(batchTimeout);
    return this;
  }

  /**
   * Get the configuration for this executor service.
   * @return an {@link ExecutorServiceConfiguration} instance.
   */
  public ExecutorServiceConfiguration getConfiguration() {
    return batchHandler.getConfig();
  }

  /**
   * Reset the configuration for this executor service to a blank state.
   * @return an {@link ExecutorServiceConfiguration} instance.
   */
  public ExecutorServiceConfiguration resetConfiguration() {
    return batchHandler.resetConfig();
  }
}
