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

package org.jppf.load.balancer.impl;

import org.jppf.load.balancer.*;
import org.slf4j.*;

/**
 * Bundler based on a reinforcement learning algorithm.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractRLBundler extends AbstractAdaptiveBundler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractRLBundler.class);
  /**
   * Determines whether debugging level is set for logging.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The incrementation step of the action.
   */
  private static final int STEP = 1;
  /**
   * Action to take.
   */
  protected int action = STEP;
  /**
   * Bounded memory of the past performance updates.
   */
  protected BundleDataHolder dataHolder = null;
  /**
   * The previous bundle size.
   */
  protected int prevBundleSize = 1;

  /**
   * Creates a new instance with the specified parameters profile.
   * @param profile the parameters of the algorithm grouped as a performance analysis profile.
   */
  public AbstractRLBundler(final LoadBalancingProfile profile)
  {
    super(profile);
    log.info("Bundler#" + bundlerNumber + ": Using Reinforcement Learning bundle size");
    log.info("Bundler#" + bundlerNumber + ": The initial size is " + bundleSize +
        ", performanceVariationThreshold = " + ((RLProfile) profile).getPerformanceVariationThreshold());
    this.dataHolder = new BundleDataHolder(((RLProfile) profile).getPerformanceCacheSize());
    this.action = ((RLProfile) profile).getMaxActionRange();
  }

  /**
   * set the current size of bundle.
   * @param bundleSize - the bundle size as an int value.
   */
  public void setBundleSize(final int bundleSize)
  {
    this.bundleSize = bundleSize;
  }

  /**
   * This method computes the bundle size based on the new state of the server.
   * @param size the number of tasks executed.
   * @param totalTime the time in nanoseconds it took to execute the tasks.
   * @see org.jppf.load.balancer.AbstractBundler#feedback(int, double)
   */
  @Override
  public void feedback(final int size, final double totalTime)
  {
    if (size <= 0) return;
    BundlePerformanceSample sample = new BundlePerformanceSample(totalTime / size, size);
    dataHolder.addSample(sample);
    computeBundleSize();
  }
 
  /**
   * Compute the new bundle size.
   */
  protected void computeBundleSize()
  {
    double d = dataHolder.getPreviousMean() - dataHolder.getMean();
    double threshold = ((RLProfile) profile).getPerformanceVariationThreshold() * dataHolder.getPreviousMean();
    prevBundleSize = bundleSize;
    if (action == 0) action = (int) -Math.signum(d);
    if ((d < -threshold) || (d > threshold))
    {
      action = (int) Math.signum(action) * (int) Math.round(d / threshold);
    }
    else action = 0;
    if (debugEnabled) log.debug("bundler #" + getBundlerNumber() + ": d = " + d + ", threshold = " + threshold + ", action = " + action);
    int maxActionRange = ((RLProfile) profile).getMaxActionRange();
    if (action > maxActionRange) action = maxActionRange;
    else if (action < -maxActionRange) action = -maxActionRange;
    bundleSize += action;
    //int max = Math.max(1, maxSize());
    int max = maxSize();
    if (bundleSize > max) bundleSize = max;
    if (bundleSize <= 0) bundleSize = 1;
  }

  /**
   * Compute the new bundle size.
   */
  protected void computeBundleSize2()
  {
    double d = dataHolder.getPreviousMean() - dataHolder.getMean();
    double threshold = ((RLProfile) profile).getPerformanceVariationThreshold() * dataHolder.getPreviousMean();
    prevBundleSize = bundleSize;
    if (action == 0) action = (int) -Math.signum(d);
    if ((d < -threshold) || (d > threshold))
    {
      action = (int) Math.signum(action) * (int) Math.round(d / threshold);
    }
    else action = 0;
    if (debugEnabled) log.debug("bundler #" + getBundlerNumber() + ": d = " + d + ", threshold = " + threshold + ", action = " + action);
    int maxActionRange = ((RLProfile) profile).getMaxActionRange();
    if (action > maxActionRange) action = maxActionRange;
    else if (action < -maxActionRange) action = -maxActionRange;
    bundleSize += action;
    //int max = Math.max(1, maxSize());
    int max = maxSize();
    if (bundleSize > max) bundleSize = max;
    if (bundleSize <= 0) bundleSize = 1;
  }

  /**
   * This method computes the bundle size based on the new state of the server.
   * @param size the number of tasks executed.
   * @param totalTime the time in nanoseconds it took to execute the tasks.
   * @see org.jppf.load.balancer.AbstractBundler#feedback(int, double)
   */
  public void feedback2(final int size, final double totalTime)
  {
    if (size <= 0) return;
    BundlePerformanceSample sample = new BundlePerformanceSample(totalTime / size, size);
    dataHolder.addSample(sample);

    double d = dataHolder.getPreviousMean() - dataHolder.getMean();
    double threshold = ((RLProfile) profile).getPerformanceVariationThreshold() * dataHolder.getPreviousMean();
    prevBundleSize = bundleSize;
    if (d < -threshold)
    {
      action += (int) Math.signum(action) * STEP;
    }
    else if (d > threshold)
    {
      //action = (int) -Math.signum(action) * Math.max(STEP, Math.abs(action/2));
      action = (int) -Math.signum(action) * STEP;
    }
    //else action = (int) -Math.signum(d) * (int) Math.signum(action) * STEP;
    else action = STEP;
    int maxActionRange = ((RLProfile) profile).getMaxActionRange();
    if (action > maxActionRange) action = maxActionRange;
    else if (action < -maxActionRange) action = -maxActionRange;
    bundleSize += action;
    //int max = Math.max(1, maxSize());
    int max = maxSize();
    if (bundleSize > max) bundleSize = max;
    if (bundleSize <= 0) bundleSize = 1;
    if (debugEnabled)
    {
      StringBuilder sb = new StringBuilder();
      sb.append("bundler #").append(getBundlerNumber()).append(" : size=").append(getBundleSize());
      sb.append(", ").append(getDataHolder());
      log.debug(sb.toString());
    }
  }

  /**
   * Perform context-independent initializations.
   * @see org.jppf.load.balancer.AbstractBundler#setup()
   */
  @Override
  public void setup()
  {
  }

  /**
   * Release the resources used by this bundler.
   * @see org.jppf.load.balancer.AbstractBundler#dispose()
   */
  @Override
  public void dispose()
  {
    dataHolder = null;
  }

  /**
   * Get the bounded memory of the past performance updates.
   * @return a BundleDataHolder instance.
   */
  public BundleDataHolder getDataHolder()
  {
    return dataHolder;
  }
}
