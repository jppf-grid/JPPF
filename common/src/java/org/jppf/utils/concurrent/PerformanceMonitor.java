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

import java.util.*;

/**
 *
 * @author Laurent Cohen
 */
class PerformanceMonitor {
  /**
   * The capacity of this performance monitor.
   */
  final int capacity;
  /**
   * The current number of values.
   */
  int nbValues;
  /**
   *
   */
  double total, mean, prevMean, variance, deviation, prevDeviation;
  /**
   * 
   */
  final Queue<Double> queue = new LinkedList<>();
  
  /**
   * 
   * @param capacity .
   */
  PerformanceMonitor(final int capacity) {
    if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
    this.capacity = capacity;
  }

  /**
   * Update with the specified value.
   * @param value the value to update with.
   */
  synchronized void update(final double value) {
    prevMean = mean;
    prevDeviation = deviation;
    if (nbValues >= capacity) {
      double oldest = queue.poll();
      total -= oldest;
      variance -= (mean - oldest) * (mean - oldest);
    } else nbValues++;
    queue.offer(value);
    total += value;
    mean = total / nbValues;
    variance += (mean - value) *  (mean - value);
    if (nbValues > 1) deviation = Math.sqrt(variance / (nbValues - 1d));
  }

  /**
   * @return the mean value.
   */
  synchronized double getMean() {
    return mean;
  }

  /**
   * @return the previous mean value.
   */
  public double getPrevMean() {
    return prevMean;
  }

  /**
   * @return the capacity of this performance monitor.
   */
  synchronized int getCapacity() {
    return capacity;
  }

  /**
   * @return the standard deviation.
   */
  synchronized double getDeviation() {
    return deviation;
  }

  /**
   * @return the previous standard deviation.
   */
  public double getPrevDeviation() {
    return prevDeviation;
  }

  /**
   * @return the sum of all values.
   */
  synchronized double getVariance() {
    return variance;
  }

  /**
   * @return the sum of all values.
   */
  synchronized double getTotal() {
    return total;
  }
}
