/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.classloader;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.JPPFUnsupportedOperationException;

/**
 * Abstract superclass for all futures handled by a {@link JPPFExecutorService}.
 * @param <V> the type of result returned by this future.
 * @author Laurent Cohen
 * @exclude
 */
public class ResourceFuture<V extends JPPFResourceWrapper> implements Future<V>
{
  /**
   * The completion status of the task represented by this future.
   */
  protected AtomicBoolean done = new AtomicBoolean(false);
  /**
   * The cancellation status of the task represented by this future.
   */
  protected AtomicBoolean cancelled = new AtomicBoolean(false);
  /**
   * The initial request.
   */
  protected final V request;
  /**
   * The execution result.
   */
  protected V response = null;
  /**
   * Lock for synchronization.
   */
  private final Object lock = new Object();

  /**
   * Initialize this future.
   */
  public ResourceFuture()
  {
    this.request = null;
  }

  /**
   * Initialize this future with the specified request.
   * @param request the orignal resource loading request.
   */
  public ResourceFuture(final V request)
  {
    this.request = request;
  }

  /**
   * Attempts to cancel execution of this task. This attempt will fail if the task has already completed,
   * already been cancelled, or could not be cancelled for some other reason.
   * If successful, and this task has not started when cancel is called, this task should never run.
   * @param mayInterruptIfRunning true if the thread executing this task should be interrupted;
   * otherwise, in-progress tasks are allowed to complete.
   * @return this method always returns false.
   * @see java.util.concurrent.Future#cancel(boolean)
   */
  @Override
  public boolean cancel(final boolean mayInterruptIfRunning)
  {
    cancelled.set(true);
    done.set(true);
    synchronized(lock)
    {
      lock.notifyAll();
    }
    return false;
  }

  /**
   * Waits if necessary for the computation to complete, and then retrieves its result.
   * @return the computed result.
   * @throws InterruptedException if the current thread was interrupted while waiting.
   * @throws ExecutionException if the computation threw an exception.
   * @see java.util.concurrent.Future#get()
   */
  @Override
  public V get() throws InterruptedException, ExecutionException
  {
    synchronized(lock)
    {
      lock.wait();
    }
    return response;
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
  public V get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
  {
    throw new ExecutionException(new JPPFUnsupportedOperationException(getClass().getSimpleName() + ".get(long, TimeUnit) is not supported"));
  }

  /**
   * Determine whether this task was cancelled before it completed normally.
   * @return true if this task was cancelled before it completed normally.
   * @see java.util.concurrent.Future#isCancelled()
   */
  @Override
  public boolean isCancelled()
  {
    return cancelled.get();
  }

  /**
   * Returns true if this task completed. Completion may be due to normal termination,
   * an exception, or cancellation. In all of these cases, this method will return true.
   * @return true if the task completed.
   * @see java.util.concurrent.Future#isDone()
   */
  @Override
  public boolean isDone()
  {
    return done.get();
  }

  /**
   * Set this future as done and store the result.
   * @param response the result to store.
   */
  public void setDone(final V response)
  {
    done.set(true);
    synchronized(lock)
    {
      this.response = response;
      lock.notifyAll();
    }
  }
}
