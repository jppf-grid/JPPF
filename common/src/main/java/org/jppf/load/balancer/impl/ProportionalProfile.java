/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import java.util.concurrent.atomic.AtomicReference;

import org.jppf.load.balancer.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Parameters profile for a proportional bundler.
 * @author Laurent Cohen
 */
public class ProportionalProfile extends AbstractLoadBalancingProfile {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ProportionalProfile.class);
  /**
   * Determines whether debugging level is set for logging.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * A default profile with default parameter values.
   */
  private static AtomicReference<ProportionalProfile> defaultProfile = new AtomicReference<>(new ProportionalProfile());
  /**
   * The maximum size of the performance samples cache.
   */
  private int performanceCacheSize = 2000;
  /**
   * The proportionality factor.
   */
  private int proportionalityFactor = 1;
  /**
   * The initial bundle size to use when the performance cache is empty, to bootstrap the algorithm.
   */
  private int initialSize = 10;
  /**
   * The initial value of the mean execution time, used to bootstrap the algorithm.
   */
  private double initialMeanTime = 1e9d;
  /**
   * The name of this profile.
   */
  private String name = "unknown";

  /**
   * Initialize this profile with default parameters.
   */
  public ProportionalProfile() {
    if (debugEnabled) log.debug("in default constructor");
  }

  /**
   * Initialize this profile with values read from the configuration file.
   * @param config contains a mapping of the profile parameters to their value.
   */
  public ProportionalProfile(final TypedProperties config) {
    if (debugEnabled) log.debug("in constructor without profile name");
    performanceCacheSize = config.getInt("performanceCacheSize", 2000);
    proportionalityFactor = config.getInt("proportionalityFactor", 1);
    initialSize = config.getInt("initialSize", 10);
    initialMeanTime = config.getDouble("initialMeanTime", 1e9d);
    if (initialMeanTime < Double.MIN_VALUE) initialMeanTime = Double.MIN_VALUE;
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
   * @param performanceCacheSize the cache size as an int.
   */
  public void setPerformanceCacheSize(final int performanceCacheSize) {
    this.performanceCacheSize = performanceCacheSize;
  }

  /**
   * Get the proportionality factor.
   * @return the factor as an int.
   */
  public int getProportionalityFactor() {
    return proportionalityFactor;
  }

  /**
   * Set the proportionality factor.
   * @param proportionalityFactor the factor as an int.
   */
  public void setProportionalityFactor(final int proportionalityFactor) {
    this.proportionalityFactor = proportionalityFactor;
  }

  /**
   * Get the default profile with default parameter values.
   * @return a <code>ProportionalTuneProfile</code> singleton instance.
   */
  public static ProportionalProfile getDefaultProfile() {
    return defaultProfile.get();
  }

  /**
   * Get the initial bundle size to use when the performance cache is empty.
   * @return the initial size as an int.
   */
  public int getInitialSize() {
    return initialSize;
  }

  /**
   * Set the initial bundle size to use when the performance cache is empty.
   * @param initialSize the initial size as an int.
   */
  public void setInitialSize(final int initialSize) {
    this.initialSize = initialSize;
  }

  /**
   * Get the initial value of the mean execution time, used to bootstrap the algorithm.
   * @return the initial mean time as a double.
   */
  public double getInitialMeanTime() {
    return initialMeanTime;
  }

  /**
   * Set the initial value of the mean execution time, used to bootstrap the algorithm.
   * @param initialMeanTime the initial mean time as a double.
   */
  public void setInitialMeanTime(final double initialMeanTime) {
    this.initialMeanTime = initialMeanTime;
  }

  /**
   * Return a string representation of this profile.
   * @return this profile represented as a string value.
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("profileName=").append(name);
    sb.append(", performanceCacheSize=").append(performanceCacheSize);
    sb.append(", proportionalityFactor=").append(proportionalityFactor);
    sb.append(", initialSize=").append(initialSize);
    sb.append(", initialMeanTime=").append(initialMeanTime);
    return sb.toString();
  }
}
