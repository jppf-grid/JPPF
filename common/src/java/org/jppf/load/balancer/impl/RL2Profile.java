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

package org.jppf.load.balancer.impl;

import org.jppf.load.balancer.AbstractLoadBalancingProfile;
import org.jppf.utils.TypedProperties;

/**
 * Parameters profile for the "rl2" algorithm.
 * @author Laurent Cohen
 */
public class RL2Profile extends AbstractLoadBalancingProfile {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The maximum size of the performance samples cache.
   */
  private final int performanceCacheSize;
  /**
   * Variation of the mean execution time that triggers a reset of the bundler.
   */
  private final double performanceVariationThreshold;
  /**
   * Number of known states from which to start computing a probablity that the next action will be chsen randomly.
   */
  private final int minSamples;
  /**
   * Number of known states from which the learning phase is considered complete.
   */
  private final int maxSamples;
  /**
   * The maximum bundle size, expressed as a fraction in the range ]0.0, 1.0], of the current job size.
   */
  private final double maxRelativeSize;

  /**
   * Initialize this profile with values read from the specified configuration.
   * @param config contains a mapping of the profile parameters to their value.
   */
  public RL2Profile(final TypedProperties config) {
    int intValue = config.getInt("performanceCacheSize", 2000);
    performanceCacheSize = (intValue <= 0) ? 2000 : intValue;
    double doubleValue = config.getDouble("performanceVariationThreshold", 0.75d);
    performanceVariationThreshold = (doubleValue <= 0d) ? 0.75d : doubleValue;
    intValue = config.getInt("minSamples", 10);
    minSamples = (intValue <= 0) ? 10 : intValue;
    intValue = config.getInt("maxSamples", minSamples);
    maxSamples = (intValue < minSamples) ? minSamples : intValue;
    doubleValue = config.getDouble("maxRelativeSize", 0.5d);
    maxRelativeSize =  ((doubleValue <= 0d) || (doubleValue > 1d)) ? 0.5d : doubleValue;
  }

  /**
   * Get the variation of the mean execution time that triggers a reset of the bundler.
   * @return the variation threshold as a {@code double} value..
   */
  public double getPerformanceVariationThreshold() {
    return performanceVariationThreshold;
  }

  /**
   * Get the maximum size of the performance samples cache.
   * @return the cache size as an {@code int}.
   */
  public int getPerformanceCacheSize() {
    return performanceCacheSize;
  }

  /**
   * Get the number of known states from which to start computing a probablity that the next action will be chsen randomly.
   * @return the min samples size as an {@code int}.
   */
  public int getMinSamples() {
    return minSamples;
  }

  /**
   * Get the number of known states from which the learning phase is considered complete.
   * @return the min samples size as an {@code int}.
   */
  public int getMaxSamples() {
    return maxSamples;
  }

  /**
   * Get the maximum bundle size, expressed as a fraction in the range ]0.0, 1.0], of the current job size.
   * @return the max relative size as a {@code double} value between 0 and 1.
   */
  public double getMaxRelativeSize() {
    return maxRelativeSize;
  }

  @Override
  public String toString() {
    return String.format("%s[VariationThreshold=%f, minSamples=%d, maxSamples=%d, maxRelativeSize=%f]",
      getClass().getSimpleName(), performanceVariationThreshold, minSamples, maxSamples, maxRelativeSize);
  }
}
