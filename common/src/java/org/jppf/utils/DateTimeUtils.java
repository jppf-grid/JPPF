/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.util.concurrent.TimeUnit;

/**
 * Utility methods for date and time manipulation and conversion.
 * @author Laurent Cohen
 */
public final class DateTimeUtils {
  /**
   * Convert the specified duration expressed in the specified time unit into milliseconds.
   * If the unit is smaller than a millisecond (either {@link java.util.concurrent.TimeUnit#NANOSECONDS TimeUnit.NANOSECONDS} or
   * {@link java.util.concurrent.TimeUnit#MICROSECONDS TimeUnit.MICROSECONDS}), the result will be rounded to the closest millisecond.
   * @param time the duration to convert.
   * @param unit the unit in which the duration is expressed.
   * @return the duration converted to milliseconds.
   */
  public static long toMillis(final long time, final TimeUnit unit) {
    long millis = TimeUnit.MILLISECONDS.equals(unit) ? time : TimeUnit.MILLISECONDS.convert(time, unit);
    long remainder = 0L;
    if (TimeUnit.NANOSECONDS.equals(unit)) {
      remainder = time % 1000000L;
      if (remainder >= 500000L) millis++;
    } else if (TimeUnit.MICROSECONDS.equals(unit)) {
      remainder = time % 1000L;
      if (remainder >= 500L) millis++;
    }
    return millis;
  }

  /**
   * Return the time in miliis elapsed since the start given in nanos.
   * @param startNanos the start value.
   * @return the elapsed time coverted to millis.
   */
  public static long elapsedFrom(final long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000L;
  }
}
