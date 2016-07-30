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

import org.jppf.load.balancer.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Bundler based on a reinforcement learning algorithm.
 * @deprecated use {@link RL2Profile} with {@link RL2Bundler} instead.
 * @author Laurent Cohen
 */
public class RLBundler extends AbstractAdaptiveBundler<RLProfile> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RLBundler.class);
  /**
   * Determines whether debugging level is set for logging.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The incrementation step of the action.
   */
  private static final int STEP = 1;
  /**
   * Action to take.
   */
  private int action = STEP;
  /**
   * Bounded memory of the past performance updates.
   */
  private BundleDataHolder dataHolder = null;

  /**
   * Creates a new instance with the specified parameters.
   * @param profile the parameters of the algorithm, grouped as a performance analysis profile.
   */
  public RLBundler(final RLProfile profile) {
    super(profile);
    if (debugEnabled) log.debug(
      String.format("Bundler #%d: using RL algorithm, initial size=%d, performanceVariationThreshold=%f", bundlerNumber, bundleSize, profile.getPerformanceVariationThreshold()));
    this.dataHolder = new BundleDataHolder(profile.getPerformanceCacheSize());
    this.action = profile.getMaxActionRange();
  }

  /**
   * set the current size of bundle.
   * @param bundleSize the bundle size as an int value.
   */
  public void setBundleSize(final int bundleSize) {
    this.bundleSize = bundleSize;
  }

  /**
   * This method computes the bundle size based on the new state of the server.
   * @param size the number of tasks executed.
   * @param totalTime the time in nanoseconds it took to execute the tasks.
   */
  @Override
  public void feedback(final int size, final double totalTime) {
    if (size <= 0) return;
    dataHolder.addSample(new BundlePerformanceSample(totalTime / size, size));
    computeBundleSize();
  }

  /**
   * Compute the new bundle size.
   */
  protected void computeBundleSize() {
    double diff = dataHolder.getPreviousMean() - dataHolder.getMean();
    double threshold = profile.getPerformanceVariationThreshold() * dataHolder.getPreviousMean();
    if (action == 0) action = (int) -Math.signum(diff);
    if ((diff < -threshold) || (diff > threshold)) action = (int) Math.signum(action) * (int) Math.round(diff / threshold);
    else action = 0;
    if (debugEnabled) log.debug("bundler #" + getBundlerNumber() + ": diff=" + diff + ", threshold=" + threshold + ", action=" + action);
    int maxActionRange = profile.getMaxActionRange();
    if (action > maxActionRange) action = maxActionRange;
    else if (action < -maxActionRange) action = -maxActionRange;
    bundleSize += action;
    //int max = Math.max(1, maxSize());
    int max = maxSize();
    if (bundleSize > max) bundleSize = max;
    if (bundleSize <= 0) bundleSize = 1;
  }

  @Override
  public void setup() {
  }

  @Override
  public void dispose() {
    super.dispose();
    dataHolder = null;
  }
}
