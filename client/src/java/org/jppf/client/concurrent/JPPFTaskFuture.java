/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.util.concurrent.*;

import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Implementation of a future handled by a {@link JPPFExecutorService}.
 * @param <V> the type of the result for the future.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFTaskFuture<V> extends AbstractJPPFFuture<V> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFTaskFuture.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The collector that contains the task.
   */
  private final JPPFJob job;
  /**
   * The position of the task in the job.
   */
  private final int position;

  /**
   * Initialize this future with the specified parameters.
   * @param job the collector that receives the results from the server.
   * @param position the position of the task in the job.
   */
  public JPPFTaskFuture(final JPPFJob job, final int position) {
    this.job = job;
    this.position = position;
  }

  /**
   * Returns true if this task completed. Completion may be due to normal termination,
   * an exception, or cancellation. In all of these cases, this method will return true.
   * @return true if the task completed.
   * @see org.jppf.client.concurrent.AbstractJPPFFuture#isDone()
   */
  @Override
  public boolean isDone() {
    return done.get();
  }

  /**
   * Waits if necessary for the computation to complete, and then retrieves its result.
   * @return the computed result.
   * @throws InterruptedException if the current thread was interrupted while waiting.
   * @throws ExecutionException if the computation threw an exception.
   * @see java.util.concurrent.Future#get()
   */
  @Override
  public V get() throws InterruptedException, ExecutionException {
    V v = null;
    try {
      v = get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    } catch(final TimeoutException e) {
      if (debugEnabled) log.debug("wait timed out, but it shouldn't have", e);
    }
    return v;
  }

  /**
   * Waits if necessary for at most the given time for the computation
   * to complete, and then retrieves its result, if available.
   * @param timeout the maximum time to wait.
   * @param unit the time unit of the timeout argument.
   * @return the computed result.
   * @throws InterruptedException if the current thread was interrupted while waiting.
   * @throws ExecutionException if the computation threw an exception.
   * @throws TimeoutException if the wait timed out.
   * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
   */
  @Override
  public V get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    final long millis = TimeUnit.MILLISECONDS.equals(unit) ? timeout : DateTimeUtils.toMillis(timeout, unit);
    getResult(millis);
    if (timedout.get()) throw new TimeoutException("wait timed out");
    else if (throwable != null) throw new ExecutionException(throwable);
    return result;
  }

  /**
   * Wait until the execution is complete, or the specified timeout has expired, whichever happens first.
   * @param timeout the maximum time to wait.
   * @throws TimeoutException if the wait timed out.
   */
  @SuppressWarnings("unchecked")
  void getResult(final long timeout) throws TimeoutException {
    if (!isDone()) {
      Task<?> task = null;
      task = (timeout > 0) ? job.getResults().waitForTask(position, timeout) : job.getResults().getResultTask(position);
      setDone();
      if (task == null) {
        setCancelled();
        timedout.set(timeout > 0);
        if (timeout > 0) throw new TimeoutException();
      } else {
        result = (V) task.getResult();
        throwable = task.getThrowable();
      }
    }
  }

  /**
   * Mark the task as done.
   */
  void setDone() {
    done.compareAndSet(false, true);
  }

  /**
   * Mark the task as cancelled.
   */
  void setCancelled() {
    cancelled.set(true);
  }

  /**
   * Get the task associated with this future.
   * @return a {@link Task} instance.
   */
  public Task<?> getTask() {
    //TODO: why can the result task be null? to investigate
    final Task<?> task = job.getResults().getResultTask(position);
    return task != null ? task : job.getJobTasks().get(position);
  }

  /**
   * Get the position of the task in the job.
   * @return the position as an integer value. 
   */
  int getPosition() {
    return position;
  }

  /**
   * Get the job that holds the task.
   * @return  a {@link JPPFJob} instance.
   */
  public JPPFJob getJob() {
    return job;
  }
}
