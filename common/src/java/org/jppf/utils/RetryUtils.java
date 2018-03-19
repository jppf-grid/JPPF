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

import java.util.concurrent.Callable;

import org.slf4j.*;

/**
 * Utility methods for retrying a callback that throws an exception, until it succeeds or
 * either a maximum number of attempts is reached or a timeout expires.
 * @author Laurent Cohen
 * @since 6.0
 */
public class RetryUtils {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RetryUtils.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Execute the specified {@code Callable} in a retry loop.
   * @param <T> the return type of the {@code Callable}.
   * @param maxTries the maximum number of attempts to execute the {@code Callable}.
   * @param retryDelay the delay between two attempts.
   * @param callable the {@code Callable} to execute.
   * @return the return value of the first successful execution of the {@code Callable}.
   * @throws Exception if after maxRetries the {@code Callable} threw an exception.
   */
  public static <T> T runWithRetry(final int maxTries, final long retryDelay, final Callable<T> callable) throws Exception {
    if (maxTries <= 0) throw new IllegalArgumentException("maxTries must be > 0");
    if (retryDelay <= 0L) throw new IllegalArgumentException("retryDelay must be > 0");
    if (callable == null) return null;
    int tryCount = 0;
    Exception lastException = null;
    while (tryCount < maxTries) {
      try {
        return callable.call();
      } catch (final Exception e) {
        tryCount++;
        lastException = e;
        if (tryCount < maxTries) {
          if (debugEnabled) log.debug(String.format("Got exception at attempt %d/%d, retrying in %,d ms: %s", tryCount, maxTries, retryDelay, ExceptionUtils.getMessage(e)));
          Thread.sleep(retryDelay);
        }
      }
    }
    throw lastException;
  }
  /**
   * Execute the specified {@code Callable} in a retry loop.
   * @param <T> the return type of the {@code Callable}.
   * @param timeout the maximum tim during which to attempt, in millis.
   * @param retryDelay the delay between two attempts.
   * @param callable the {@code Callable} to execute.
   * @return the return value of the first successful execution of the {@code Callable}.
   * @throws Exception if after maxRetries the {@code Callable} threw an exception.
   */
  public static <T> T runWithRetryTimeout(final long timeout, final long retryDelay, final Callable<T> callable) throws Exception {
    if (timeout <= 0L) throw new IllegalArgumentException("timeout must be > 0");
    if (retryDelay <= 0L) throw new IllegalArgumentException("retryDelay must be > 0");
    if (callable == null) return null;
    int tryCount = 0;
    Exception lastException = null;
    final long start = System.currentTimeMillis();
    long elapsed;
    while ((elapsed = System.currentTimeMillis() - start) < timeout) {
      try {
        return callable.call();
      } catch (final Exception e) {
        tryCount++;
        lastException = e;
        if (elapsed < timeout) {
          if (debugEnabled) log.debug(String.format("Got exception at attempt %,d, after %,d ms, retrying in %,d ms: %s", tryCount, elapsed, retryDelay, ExceptionUtils.getMessage(e)));
          Thread.sleep(retryDelay);
        }
      }
    }
    throw lastException;
  }
}
