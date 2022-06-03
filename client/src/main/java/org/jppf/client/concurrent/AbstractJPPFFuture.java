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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract superclass for all futures handled by a {@link JPPFExecutorService}.
 * @param <V> the type of result returned by this future.
 * @author Laurent Cohen
 * @exclude
 */
abstract class AbstractJPPFFuture<V> implements Future<V> {
  /**
   * The completion status of the task represented by this future.
   */
  protected AtomicBoolean done = new AtomicBoolean(false);
  /**
   * The cancellation status of the task represented by this future.
   */
  protected AtomicBoolean cancelled = new AtomicBoolean(false);
  /**
   * Determines whether the task execution timed out.
   */
  protected AtomicBoolean timedout = new AtomicBoolean(false);
  /**
   * The execution result.
   */
  protected V result;
  /**
   * An exception that may be raised by the execution of the task.
   */
  protected Throwable throwable;

  /**
   * Attempts to cancel execution of this task. This attempt will fail if the task has already completed,
   * already been cancelled, or could not be cancelled for some other reason.
   * If successful, and this task has not started when cancel is called, this task should never run.
   * @param mayInterruptIfRunning true if the thread executing this task should be interrupted;
   * otherwise, in-progress tasks are allowed to complete.
   * @return this method always returns false.
   */
  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    return false;
  }

  /**
   * Waits if necessary for the computation to complete, and then retrieves its result.
   * @return the computed result.
   * @throws InterruptedException if the current thread was interrupted while waiting.
   * @throws ExecutionException if the computation threw an exception.
   */
  @Override
  public V get() throws InterruptedException, ExecutionException {
    return null;
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
   */
  @Override
  public V get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return null;
  }

  /**
   * Determine whether this task was cancelled before it completed normally.
   * @return true if this task was cancelled before it completed normally.
   */
  @Override
  public boolean isCancelled() {
    return cancelled.get();
  }

  /**
   * Returns true if this task completed. Completion may be due to normal termination,
   * an exception, or cancellation. In all of these cases, this method will return true.
   * @return true if the task completed.
   */
  @Override
  public boolean isDone() {
    return done.get();
  }
}
