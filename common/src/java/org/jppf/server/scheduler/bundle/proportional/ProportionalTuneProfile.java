/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.server.scheduler.bundle.proportional;

import java.util.concurrent.atomic.AtomicReference;

import org.jppf.server.scheduler.bundle.LoadBalancingProfile;
import org.jppf.server.scheduler.bundle.autotuned.AbstractAutoTuneProfile;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Parameters profile for a proportional bundler.
 * @author Laurent Cohen
 */
public class ProportionalTuneProfile extends AbstractAutoTuneProfile
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ProportionalTuneProfile.class);
  /**
   * Determines whether debugging level is set for logging.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * A default profile with default parameter values.
   */
  private static AtomicReference<ProportionalTuneProfile> defaultProfile = new AtomicReference<ProportionalTuneProfile>(new ProportionalTuneProfile());
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
  public ProportionalTuneProfile()
  {
    if (debugEnabled) log.debug("in default constructor");
  }

  /**
   * Initialize this profile with values read from the configuration file.
   * @param profileName name of the profile in the configuration file.
   */
  public ProportionalTuneProfile(final String profileName)
  {
    if (debugEnabled) log.debug("in constructor with profile name");
    this.name = profileName;
    String prefix = "strategy." + profileName + '.';
    TypedProperties props = JPPFConfiguration.getProperties();
    performanceCacheSize = props.getInt(prefix + "performanceCacheSize", 2000, 1, Integer.MAX_VALUE);
    proportionalityFactor = props.getInt(prefix + "proportionalityFactor", 1, 1, Integer.MAX_VALUE);
    initialSize = props.getInt(prefix + "initialSize", 10, 1, Integer.MAX_VALUE);
    initialMeanTime = props.getDouble(prefix + "initialMeanTime", 1e9d, Double.MIN_VALUE, Double.MAX_VALUE);
  }

  /**
   * Initialize this profile with values read from the configuration file.
   * @param config contains a mapping of the profile parameters to their value.
   */
  public ProportionalTuneProfile(final TypedProperties config)
  {
    if (debugEnabled) log.debug("in constructor without profile name");
    performanceCacheSize = config.getInt("performanceCacheSize", 2000);
    proportionalityFactor = config.getInt("proportionalityFactor", 1);
    initialSize = config.getInt("initialSize", 10);
    initialMeanTime = config.getDouble("initialMeanTime", 1e9d, Double.MIN_VALUE, Double.MAX_VALUE);
  }

  /**
   * Make a copy of this profile.
   * @return a new <code>AutoTuneProfile</code> instance.
   * @see org.jppf.server.scheduler.bundle.LoadBalancingProfile#copy()
   */
  @Override
  public LoadBalancingProfile copy()
  {
    ProportionalTuneProfile other = new ProportionalTuneProfile();
    other.setPerformanceCacheSize(performanceCacheSize);
    other.setProportionalityFactor(proportionalityFactor);
    other.setInitialSize(initialSize);
    other.setInitialMeanTime(initialMeanTime);
    return other;
  }

  /**
   * Get the maximum size of the performance samples cache.
   * @return the cache size as an int.
   */
  public int getPerformanceCacheSize()
  {
    return performanceCacheSize;
  }

  /**
   * Set the maximum size of the performance samples cache.
   * @param performanceCacheSize the cache size as an int.
   */
  public void setPerformanceCacheSize(final int performanceCacheSize)
  {
    this.performanceCacheSize = performanceCacheSize;
  }

  /**
   * Get the proportionality factor.
   * @return the factor as an int.
   */
  public int getProportionalityFactor()
  {
    return proportionalityFactor;
  }

  /**
   * Set the proportionality factor.
   * @param proportionalityFactor the factor as an int.
   */
  public void setProportionalityFactor(final int proportionalityFactor)
  {
    this.proportionalityFactor = proportionalityFactor;
  }

  /**
   * Get the default profile with default parameter values.
   * @return a <code>ProportionalTuneProfile</code> singleton instance.
   */
  public static ProportionalTuneProfile getDefaultProfile()
  {
    return defaultProfile.get();
  }

  /**
   * Get the initial bundle size to use when the performance cache is empty.
   * @return the initial size as an int.
   */
  public int getInitialSize()
  {
    return initialSize;
  }

  /**
   * Set the initial bundle size to use when the performance cache is empty.
   * @param initialSize the initial size as an int.
   */
  public void setInitialSize(final int initialSize)
  {
    this.initialSize = initialSize;
  }

  /**
   * Get the initial value of the mean execution time, used to bootstrap the algorithm.
   * @return the initial mean time as a double.
   */
  public double getInitialMeanTime()
  {
    return initialMeanTime;
  }

  /**
   * Set the initial value of the mean execution time, used to bootstrap the algorithm.
   * @param initialMeanTime the initial mean time as a double.
   */
  public void setInitialMeanTime(final double initialMeanTime)
  {
    this.initialMeanTime = initialMeanTime;
  }

  /**
   * Return a string representation of this profile.
   * @return this profile represented as a string value.
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("profileName=").append(name);
    sb.append(", performanceCacheSize=").append(performanceCacheSize);
    sb.append(", proportionalityFactor=").append(proportionalityFactor);
    sb.append(", initialSize=").append(initialSize);
    return sb.toString();
  }
}
