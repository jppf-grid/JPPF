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

package org.jppf.load.balancer;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Each instance of this class acts as a container for the performance data related to a node.
 * @author Laurent Cohen
 * @exclude
 */
public class PerformanceCache implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Holds the samples required for calculating the moving average.
   */
  private final LinkedList<PerformanceSample> samples = new LinkedList<>();
  /**
   * Current value of the moving average.
   */
  private double mean = 1000d;
  /**
   * Previous value of the moving average, generally before a new sample was added.
   */
  private double previousMean = 0d;
  /**
   * Current value of the moving average.
   */
  private double totalTime = 0d;
  /**
   * Current number of samples.
   */
  private long nbSamples = 1L;
  /**
   * Number of samples required to compute the moving average.
   */
  private int size;

  /**
   * Initialize this data holder with the maximum number of samples in the performance cache.
   * @param size the number of samples as an int.
   */
  public PerformanceCache(final int size) {
    this.size = size;
  }

  /**
   * Initialize this data holder with the maximum number of samples in the performance cache, and initial mean execution time.
   * @param size the number of samples as an int.
   * @param mean the initial mean execution time.
   */
  public PerformanceCache(final int size, final double mean) {
    this.size = size;
    this.mean = mean;
  }

  /**
   * Add the specified performance sample to the list of samples.
   * @param sample the performance sample to add.
   */
  public void addSample(final PerformanceSample sample) {
    while ((sample.samples + nbSamples > size) && !samples.isEmpty()) removeHeadSample();
    samples.add(sample);
    totalTime += sample.samples * sample.mean;
    nbSamples += sample.samples;
    computeMean();
  }

  /**
   * Remove the least recent sample from the list of samples.
   */
  private void removeHeadSample() {
    final PerformanceSample sample = samples.removeFirst();
    if (sample != null) {
      nbSamples -= sample.samples;
      totalTime -= sample.samples * sample.mean;
    }
  }

  /**
   * Compute the mean time.
   */
  private void computeMean() {
    if (nbSamples > 0) {
      previousMean = mean;
      mean = totalTime / nbSamples;
    }
  }

  /**
   * Get the computed mean execution time for the corresponding node.
   * @return the mean value as a double.
   */
  public double getMean() {
    return mean;
  }

  /**
   * Get the computed mean execution time for the corresponding node.
   * @param mean the mean value as a double.
   */
  public void setMean(final double mean) {
    this.mean = mean;
  }

  /**
   * Get the previously computed mean execution time for the corresponding node.
   * @return the previous mean value as a double.
   */
  public double getPreviousMean() {
    return previousMean;
  }

  /**
   * Get the number of samples required to compute the moving average.
   * @return the number of samples as an int.
   */
  public int getSize() {
    return size;
  }

  /**
   * Set the maximum number fo samples.
   * @param size the new maximum number of samples.
   */
  public void setSize(final int size) {
    this.size = size;
    while ((nbSamples > size) && (samples.size() > 1)) removeHeadSample();
  }

  /**
   * Get the current number of samples.
   * @return the number of samples as an int.
   */
  public long getNbSamples() {
    return nbSamples;
  }

  /**
   * Remove all entries in the performance samples list.
   */
  public void clear() {
    samples.clear();
  }

  @Override
  public String toString() {
    return new StringBuilder()
      .append("mean=").append(mean)
      .append(", previousMean=").append(previousMean)
      .append(", totalTime=").append(totalTime)
      .append(", nbSamples=").append(nbSamples)
      .append(", size=").append(size)
      .append(", samples.size()=").append(samples.size())
      .toString();
  }
}
