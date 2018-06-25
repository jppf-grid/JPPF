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

package org.jppf.utils.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.*;

import org.jppf.JPPFTimeoutException;

/**
 * A set of utility methods to facilitate concurrent and multithreaded programming.
 * @author Laurent Cohen
 * @since 5.0
 */
public final class ConcurrentUtils {
  /**
   * Instantiation is not permitted.
   */
  private ConcurrentUtils() {
  }

  /**
   * Wait until the specified condition is fulfilled, or the timeout expires, whichever happens first.
   * The specified monitor may be notified at any time during the execution of this method, at which time it will check the condition again.
   * @param monitor the monitor to wait for.
   * @param condition the condition to check.
   * @param millis the milliseconds part of the timeout. A value of zero means an infinite timeout.
   * @return true if the condition is {@code null} or was fulfilled before the timeout expired, {@code false} otherwise.
   * @throws IllegalArgumentException if the millis or nanos are negative, or if the nanos are greater than 999999.
   */
  public static boolean awaitCondition(final ThreadSynchronization monitor, final Condition condition, final long millis) throws IllegalArgumentException {
    return awaitCondition(monitor, condition, millis, 1L);
  }

  /**
   * Wait until the specified condition is fulfilled, or the timeout expires, whichever happens first.
   * The specified monitor may be notified at any time during the execution of this method, at which time it will check the condition again.
   * @param monitor the monitor to wait for.
   * @param condition the condition to check.
   * @param millis the milliseconds part of the timeout. A value of zero means an infinite timeout.
   * @param sleepInterval how long to wait between evaluations of the condition, in millis.
   * @return true if the condition is {@code null} or was fulfilled before the timeout expired, {@code false} otherwise.
   * @throws IllegalArgumentException if the millis or nanos are negative, or if the nanos are greater than 999999.
   */
  public static boolean awaitCondition(final ThreadSynchronization monitor, final Condition condition, final long millis, final long sleepInterval) throws IllegalArgumentException {
    if (monitor == null) throw new IllegalArgumentException("monitor cannot be null");
    if (sleepInterval < 0L) throw new IllegalArgumentException("sleepInterval must be > 0");
    if (condition == null) return true;
    if (millis < 0L) throw new IllegalArgumentException("millis cannot be negative");
    final long timeout = (millis > 0L) ? millis : Long.MAX_VALUE;
    boolean fulfilled = false;
    final long start = System.nanoTime();
    synchronized(monitor) {
      while (!(fulfilled = condition.evaluate()) && ((System.nanoTime() - start) / 1_000_000L < timeout)) monitor.goToSleep(sleepInterval);
    }
    return fulfilled;
  }

  /**
   * Wait until the specified condition is fulfilled, or the timeout expires, whichever happens first.
   * The specified monitor may be notified at any time during the execution of this method, at which time it will check the condition again.
   * @param condition the condition to check.
   * @param millis the milliseconds part of the timeout. A value of zero means an infinite timeout.
   * @param sleepInterval how long to wait between evaluations of the condition, in millis.
   * @param throwExceptionOnTImeout whether to raise an exception if the timeout expires.
   * @return true if the condition is {@code null} or was fulfilled before the timeout expired, {@code false} otherwise.
   * @throws IllegalArgumentException if the millis or nanos are negative, or if the nanos are greater than 999999.
   * @throws JPPFTimeoutException if the timeout expires and {@code throwExceptionOnTImeout} is {@code true}.
   */
  public static boolean awaitCondition(final Condition condition, final long millis, final long sleepInterval, final boolean throwExceptionOnTImeout)
    throws IllegalArgumentException, JPPFTimeoutException {
    if (sleepInterval < 0L) throw new IllegalArgumentException("sleepInterval must be > 0");
    if (condition == null) return true;
    if (millis < 0L) throw new IllegalArgumentException("millis cannot be negative");
    final long timeout = (millis > 0L) ? millis : Long.MAX_VALUE;
    boolean fulfilled = false;
    final long start = System.nanoTime();
    long elapsed = 0L;
    while (!(fulfilled = condition.evaluate()) && ((elapsed = (System.nanoTime() - start) / 1_000_000L) < timeout)) {
      try {
        Thread.sleep(sleepInterval);
      } catch (@SuppressWarnings("unused") final InterruptedException e) {
      }
    }
    if (throwExceptionOnTImeout && (elapsed > timeout)) throw new JPPFTimeoutException(String.format("exceeded timeout of %,d ms", timeout));
    return fulfilled;
  }

  /**
   * Wait until the specified condition is fulfilled, or the timeout expires, whichever happens first.
   * This method waits for 1 millisecond each time the condition check fails and until the condition is fulfilled or the timeout expires.
   * @param condition the condition to check.
   * @param millis the milliseconds part of the timeout. A value of zero means an infinite timeout.
   * @return true if the condition is {@code null} or was fulfilled before the timeout expired, {@code false} otherwise.
   * @throws IllegalArgumentException if the millis are negative.
   */
  public static boolean awaitCondition(final Condition condition, final long millis) throws IllegalArgumentException {
    return awaitCondition(condition, millis, 10L, false);
  }

  /**
   * Wait until the specified condition is fulfilled.
   * @param condition the condition to check.
   * @return true whenever the condition is {@code null} or gets fulfilled, {@code false} otherwise.
   */
  public static boolean awaitCondition(final Condition condition) {
    return awaitCondition(condition, 0L, 10L, false);
  }

  /**
   * Wait until the specified condition is fulfilled, or the timeout expires, whichever happens first.
   * This method waits for 1 millisecond each time the condition check fails and until the condition is fulfilled or the timeout expires.
   * @param condition the condition to check.
   * @param millis the milliseconds part of the timeout. A value of zero means an infinite timeout.
   * @param throwExceptionOnTImeout whether to raise an exception if the timeout expires.
   * @return true if the condition is {@code null} or was fulfilled before the timeout expired, {@code false} otherwise.
   * @throws IllegalArgumentException if the millis are negative.
   * @throws JPPFTimeoutException if the timeout expires and {@code throwExceptionOnTImeout} is {@code true}.
   */
  public static boolean awaitCondition(final Condition condition, final long millis, final boolean throwExceptionOnTImeout) throws IllegalArgumentException, JPPFTimeoutException {
    return awaitCondition(condition, millis, 10L, throwExceptionOnTImeout);
  }

  /**
   * Wait until the specified condition is fulfilled, or the timeout expires, whichever happens first.
   * This method waits for 1 millisecond each time the condition check fails and until the condition is fulfilled or the timeout expires.
   * @param condition the condition to check.
   * @param millis the milliseconds part of the timeout. A value of zero means an infinite timeout.
   * @param throwExceptionOnTImeout whether to raise an exception if the timeout expires.
   * @return true if the condition is {@code null} or was fulfilled before the timeout expired, {@code false} otherwise.
   * @throws IllegalArgumentException if the millis are negative.
   * @throws JPPFTimeoutException if the timeout expires and {@code throwExceptionOnTImeout} is {@code true}.
   */
  public static boolean awaitInterruptibleCondition(final Condition condition, final long millis, final boolean throwExceptionOnTImeout)
    throws IllegalArgumentException, JPPFTimeoutException {
    return awaitInterruptibleCondition(condition, millis, 1L, throwExceptionOnTImeout);
  }

  /**
   * Wait until the specified condition is fulfilled, or the timeout expires, whichever happens first.
   * This method waits for 1 millisecond each time the condition check fails and until the condition is fulfilled or the timeout expires.
   * @param condition the condition to check.
   * @param millis the timeout in milliseconds. A value of zero means an infinite timeout.
   * @param retryInterval the time to wait between 2 evaluations of the condition, must be > 0.
   * @param throwExceptionOnTImeout whether to raise an exception if the timeout expires.
   * @return true if the condition is {@code null} or was fulfilled before the timeout expired, {@code false} otherwise.
   * @throws IllegalArgumentException if the millis are negative.
   * @throws JPPFTimeoutException if the timeout expires and {@code throwExceptionOnTImeout} is {@code true}.
   */
  public static boolean awaitInterruptibleCondition(final Condition condition, final long millis, final long retryInterval, final boolean throwExceptionOnTImeout)
    throws IllegalArgumentException, JPPFTimeoutException {
    if (condition == null) return true;
    if (millis < 0L) throw new IllegalArgumentException("millis cannot be negative");
    if (retryInterval <= 0L) throw new IllegalArgumentException("retyInterval must be > 0");
    final long timeout = millis > 0L ? millis : Long.MAX_VALUE;
    final CountDownLatch countDown = new CountDownLatch(1);
    final ThreadSynchronization monitor = new ThreadSynchronization() { };
    final AtomicBoolean fulfilled = new AtomicBoolean(false);
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        boolean ok = false;
        synchronized(monitor) {
          try {
            while (!(ok = condition.evaluate())) monitor.wait(retryInterval);
          } catch (@SuppressWarnings("unused") final Exception e) {
          }
        }
        fulfilled.set(ok);
        countDown.countDown();
      }
    };
    ;
    final Thread thread = ThreadUtils.startThread(r, "awaitInterruptibleCondition(" + condition + ")");
    try {
      countDown.await(timeout, TimeUnit.MILLISECONDS);
    } catch (@SuppressWarnings("unused") final InterruptedException e) {
      thread.interrupt();
      countDown.countDown();
    }
    if (fulfilled.get()) return true;
    if (throwExceptionOnTImeout) throw new JPPFTimeoutException(String.format("exceeded timeout of %,d", timeout));
    return false;
  }

  /**
   * Wait on the specified monitor for at most the specified tiemout or until the specified condition is fulfilled, waiting by increment of the specified wait time.
   * @param monitor the monitor to wait on.
   * @param condition the condition to check.
   * @param timeout the wait timeout.
   * @param waitTime the max wait time in the wait loop.
   * @param throwExceptionOnTImeout whether to raise an exception if the timeout expires.
   * @throws Exception if any error occurs.
   */
  public static void waitForMonitor(final Object monitor, final Condition condition, final long timeout, final long waitTime, final boolean throwExceptionOnTImeout) throws Exception {
    if (monitor == null) throw new IllegalArgumentException("monitor cannot be null");
    if (waitTime < 0L) throw new IllegalArgumentException("waitTime must be > 0");
    if (condition == null) return;
    if (timeout < 0L) throw new IllegalArgumentException("millis cannot be negative");
    final long millis = (timeout > 0L) ? timeout : Long.MAX_VALUE;
    long elapsed = 0L;
    final long start = System.nanoTime();
    synchronized(monitor) {
      while (!condition.evaluate() && ((elapsed = (System.nanoTime() - start) / 1_000_000L) < millis)) monitor.wait(Math.min(waitTime, Math.max(1, timeout - elapsed)));
    }
    if (throwExceptionOnTImeout && (elapsed >= millis)) throw new JPPFTimeoutException(String.format("exceeded timeout of %,d", millis));
  }

  /**
   * This interface represents a condition to evaluate to either {@code true} or {@code false}.
   */
  public static interface Condition {
    /**
     * Evaluate this condition.
     * @return {@code true} if the condition is fulfilled, {@code false} otherwise.
     */
    boolean evaluate();
  }

  /**
   * This abstrat class handles exceptions raised by its {@code evaluate()} method and returns {@code false} when it happens.
   */
  public static abstract class ConditionFalseOnException implements Condition {
    @Override
    public boolean evaluate() {
      try {
        return evaluateWithException();
      } catch (@SuppressWarnings("unused") final Exception e) {
        return false;
      }
    }

    /**
     * Evaluate an arbitrary condition.
     * @return true if the condition was met, false otherwise.
     * @throws Exception if any error occurs during the evaluation.
     */
    public abstract boolean evaluateWithException() throws Exception;
  }

  /**
   * Create a pool that directly hands off new tasks, creating new threads as required.
   * @param coreThreads the number of core threads.
   * @param ttl the threads' time-to -live in millis.
   * @param threadNamePrefix name prefix for created threads.
   * @return a new {@link ThreadPoolExecutor} instance.
   */
  public static ThreadPoolExecutor newDirectHandoffExecutor(final int coreThreads, final long ttl, final String threadNamePrefix) {
    final ThreadPoolExecutor executor = new ThreadPoolExecutor(coreThreads, Integer.MAX_VALUE, ttl, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new JPPFThreadFactory(threadNamePrefix));
    executor.allowCoreThreadTimeOut(false);
    executor.prestartAllCoreThreads();
    return executor;
  }

  /**
   * Create a pool that directly hands off new tasks, creating new threads as required.
   * @param coreThreads the number of core threads.
   * @param maxThreads the maximum number of threads.
   * @param ttl the threads' time-to -live in millis.
   * @param threadNamePrefix name prefix for created threads.
   * @return a new {@link ThreadPoolExecutor} instance.
   */
  public static ThreadPoolExecutor newDirectHandoffExecutor(final int coreThreads, final int maxThreads, final long ttl, final String threadNamePrefix) {
    final ThreadPoolExecutor executor = new ThreadPoolExecutor(coreThreads, maxThreads, ttl, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new JPPFThreadFactory(threadNamePrefix));
    executor.allowCoreThreadTimeOut(false);
    executor.prestartAllCoreThreads();
    return executor;
  }

  /**
   * Create a pool that directly hands off new tasks, creating new threads as required.
   * @param coreThreads the number of core threads.
   * @param queueSize the size of the bounded queue.
   * @param ttl the threads' time-to -live in millis.
   * @param threadNamePrefix name prefix for created threads.
   * @return a new {@link ThreadPoolExecutor} instance.
   */
  public static ThreadPoolExecutor newBoundedQueueExecutor(final int coreThreads, final int queueSize, final long ttl, final String threadNamePrefix) {
    final ThreadPoolExecutor executor = new ThreadPoolExecutor(
      coreThreads, Integer.MAX_VALUE, ttl, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(queueSize), new JPPFThreadFactory(threadNamePrefix));
    executor.allowCoreThreadTimeOut(false);
    executor.prestartAllCoreThreads();
    return executor;
  }

  /**
   * Create a fixed-size pool.
   * @param coreThreads the number of core threads.
   * @param threadNamePrefix name prefix for created threads.
   * @return a new {@link ThreadPoolExecutor} instance.
   */
  public static ThreadPoolExecutor newFixedExecutor(final int coreThreads, final String threadNamePrefix) {
    final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(coreThreads, new JPPFThreadFactory(threadNamePrefix));
    executor.setKeepAliveTime(15000L, TimeUnit.MILLISECONDS);
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }

  /**
   * Create a {@link JPPFThreadPool}.
   * @param coreThreads the number of core threads.
   * @param threadNamePrefix name prefix for created threads.
   * @return a new {@link JPPFThreadPool} instance.
   */
  public static ExecutorService newJPPFFixedThreadPool(final int coreThreads, final String threadNamePrefix) {
    final JPPFThreadPool executor = new JPPFThreadPool(coreThreads, new JPPFThreadFactory(threadNamePrefix));
    return executor;
  }

  /**
   * Create a {@link JPPFThreadPool}.
   * @param coreThreads the number of core threads.
   * @param maxThreads the maximum number of threads.
   * @param ttl the non-core threads' time-to-live in millis.
   * @param threadNamePrefix name prefix for created threads.
   * @return a new {@link JPPFThreadPool} instance.
   */
  public static ExecutorService newJPPFThreadPool(final int coreThreads, final int maxThreads, final long ttl, final String threadNamePrefix) {
    final JPPFThreadPool executor = new JPPFThreadPool(coreThreads, maxThreads, ttl, new JPPFThreadFactory(threadNamePrefix));
    return executor;
  }

  /**
   * Create a pool that directly hands off new tasks, creating new threads as required.
   * @param coreThreads the number of core threads.
   * @param maxThreads the maximum number of threads.
   * @param ttl the threads' time-to -live in millis.
   * @param threadNamePrefix name prefix for created threads.
   * @return a new {@link ThreadPoolExecutor} instance.
   */
  public static ExecutorService newJPPFDirectHandoffExecutor(final int coreThreads, final int maxThreads, final long ttl, final String threadNamePrefix) {
    return new JPPFThreadPool(coreThreads, maxThreads, ttl, new JPPFThreadFactory(threadNamePrefix), new SynchronousQueue<Runnable>());
  }

  /**
   * Factory method that allows changing the lock implementation without refactoring the code that uses it.
   * This method effectively calls {@link #newLock(String) newLock(null)}.
   * @return a new {@link Lock} instance.
   */
  public static Lock newLock() {
    return newLock(null);
  }

  /**
   * Factory method that allows changing the lock implementation without refactoring the code that uses it.
   * This method provides a name for the lock and will set it onto the lock if the implementation allows it, otherwise the name is just discarded.
   * @param name a name given to the created lock.
   * @return a new {@link Lock} instance.
   */
  public static Lock newLock(final String name) {
    return new ReentrantLock();
    //return new SynchronizedLock(name);
    //return new JPPFReentrantLock(name);
  }
}
