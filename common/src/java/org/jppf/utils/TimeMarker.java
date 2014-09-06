/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

/**
 * A simple utility class to measure the elapsed time between two points in the code.
 * Typicaly it is used as follows:
 * <pre>TimeMarker marker = new TimeMarker().start();
 * // ... execute some code ...
 * long elapsed = marker.stop().getLastElapsed();
 * System.out.println("elapsed time: " + marker.getLastElapsedAsString());</pre>
 * @author Laurent Cohen
 */
public class TimeMarker {
  /**
   * Holds the start of a new time measurement.
   */
  private long start;
  /**
   * Holds the elapsed time for the last measurement.
   */
  private long lastElapsed;
  /**
   * Holds the sum of all performed measurements.
   */
  private long totalElapsed;
  /**
   * Holds the count of performed measurements.
   */
  private int count;

  /**
   * Start a new measurement.
   * @return this {@code TimeMarker}.
   */
  public TimeMarker start() {
    start = System.nanoTime();
    return this;
  }

  /**
   * Stop the current measurement.
   * @return this {@code TimeMarker}.
   */
  public TimeMarker stop() {
    lastElapsed = System.nanoTime() - start;
    totalElapsed += lastElapsed;
    count++;
    return this;
  }

  /**
   * Get the elapsed time for the last measurement.
   * @return the elapsed time as a {@code long} value.
   */
  public long getLastElapsed() {
    return lastElapsed;
  }

  /**
   * Get the total elapsed time for all measurements.
   * @return the elapsed time as a {@code long} value.
   */
  public long getTotalElapsed() {
    return totalElapsed;
  }

  /**
   * Get the elapsed time for the last measurement formatted as hh:mm:ss.nnn
   * @return a string representation of the last elapsed time.
   */
  public String getLastElapsedAsString() {
    return StringUtils.toStringDuration(lastElapsed / 1_000_000L);
  }

  /**
   * Get the total elapsed time formatted as hh:mm:ss.nnn
   * @return a string representation of the total elapsed time.
   */
  public String getTotalElapsedAsString() {
    return StringUtils.toStringDuration(totalElapsed / 1_000_000L);
  }

  /**
   * Get the count of measurements.
   * @return the count as an {@code int} value.
   */
  public int getCount() {
    return count;
  }
}
