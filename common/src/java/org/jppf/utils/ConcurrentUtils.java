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

package org.jppf.utils;

import org.jppf.JPPFTimeoutException;

/**
 * A set of utility methods to facilitate concurrent and multithreaded rpogramming.
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
    long timeout = (millis > 0L) ? millis : Long.MAX_VALUE;
    boolean fulfilled = false;
    final long start = System.nanoTime();
    synchronized(monitor) {
      while (!(fulfilled = condition.evaluate()) && ((System.nanoTime() - start) / 1_000_000L < timeout)) monitor.goToSleep(sleepInterval);
    }
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
    return awaitCondition(condition, millis, false);
  }

  /**
   * Wait until the specified condition is fulfilled, or the timeout expires, whichever happens first.
   * This method waits for 1 millisecond each time the condition check fails and until the condition is fulfilled or the timeout expires.
   * @param condition the condition to check.
   * @param millis the milliseconds part of the timeout. A value of zero means an infinite timeout.
   * @param throwExceptionOnTImeout whether to raise an exception if the timeout expires.
   * @return true if the condition is {@code null} or was fulfilled before the timeout expired, {@code false} otherwise.
   * @throws IllegalArgumentException if the millis are negative.
   * @throws JPPFTimeoutException if the timeout expires.
   */
  public static boolean awaitCondition(final Condition condition, final long millis, final boolean throwExceptionOnTImeout) throws IllegalArgumentException, JPPFTimeoutException {
    if (condition == null) return true;
    if (millis < 0L) throw new IllegalArgumentException("millis cannot be negative");
    long timeout = millis > 0L ? millis : Long.MAX_VALUE;
    long start = System.nanoTime();
    ThreadSynchronization monitor = new ThreadSynchronization() { };
    boolean fulfilled = false;
    long elapsed = 0L;
    while (!(fulfilled = condition.evaluate()) && ((elapsed = (System.nanoTime() - start) / 1_000_000L) < timeout)) {
      monitor.goToSleep(1L);
    }
    if ((elapsed > timeout) && throwExceptionOnTImeout) throw new JPPFTimeoutException(String.format("exceeded timeout of %,d", timeout));
    return fulfilled;
  }

  /**
   * Wait until the specified condition is fulfilled.
   * @param condition the condition to check.
   * @return true whenever the condition is {@code null} or gets fulfilled, {@code false} otherwise.
   */
  public static boolean awaitCondition(final Condition condition) {
    return awaitCondition(condition, 0L);
  }

  /**
   * This interface represents a condition to evaluate to either {@code true} or {@code false}.
   */
  public interface Condition {
    /**
     * Evaluate this condition.
     * @return {@code true} if the condition is fulfilled, {@code false} otherwise.
     */
    boolean evaluate();
  }
}
