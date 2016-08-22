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

package org.jppf.load.balancer.impl;

import org.jppf.load.balancer.AbstractLoadBalancingProfile;
import org.jppf.utils.TypedProperties;

/**
 * Parameters profile for a proportional bundler.
 * @deprecated use {@link RL2Profile} with {@link RL2Bundler} instead.
 * @author Laurent Cohen
 */
public class RLProfile extends AbstractLoadBalancingProfile {
  /**
   * The maximum size of the performance samples cache.
   */
  private int performanceCacheSize = 2000;
  /**
   * Variation of the mean execution time that triggers a change in bundle size.
   */
  private double performanceVariationThreshold = 0.0001d;
  /**
   * The absolute value of the maximum increase of the the bundle size.
   */
  private int maxActionRange = 50;

  /**
   * Initialize this profile with values read from the specified configuration.
   * @param config contains a mapping of the profile parameters to their value.
   */
  public RLProfile(final TypedProperties config) {
    performanceCacheSize = config.getInt("performanceCacheSize", 2000);
    performanceVariationThreshold = config.getDouble("performanceVariationThreshold", 0.001);
    maxActionRange = config.getInt("maxActionRange", 50);
  }

  /**
   * Get the maximum size of the performance samples cache.
   * @return the cache size as an int.
   */
  public int getPerformanceCacheSize() {
    return performanceCacheSize;
  }

  /**
   * Set the maximum size of the performance samples cache.
   * @param performanceCacheSize - the cache size as an int.
   */
  public void setPerformanceCacheSize(final int performanceCacheSize) {
    this.performanceCacheSize = performanceCacheSize;
  }

  /**
   * Get the variation of the mean execution time that triggers a change in bundle size.
   * @return the variation as a double value.
   */
  public double getPerformanceVariationThreshold() {
    return performanceVariationThreshold;
  }

  /**
   * Get the variation of the mean execution time that triggers a change in bundle size.
   * @param performanceVariationThreshold - the variation as a double value.
   */
  public void setPerformanceVariationThreshold(final double performanceVariationThreshold) {
    this.performanceVariationThreshold = performanceVariationThreshold;
  }

  /**
   * Get the absolute value of the maximum increase of the the bundle size.
   * @return the value as an int.
   */
  public int getMaxActionRange() {
    return maxActionRange;
  }

  /**
   * Get the absolute value of the maximum increase of the the bundle size.
   * @param maxActionRange - the value as an int.
   */
  public void setMaxActionRange(final int maxActionRange) {
    this.maxActionRange = maxActionRange;
  }
}
