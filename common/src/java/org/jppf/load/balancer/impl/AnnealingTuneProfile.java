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

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.jppf.load.balancer.AbstractLoadBalancingProfile;
import org.jppf.utils.TypedProperties;

/**
 * This class implements the basis of a profile based on simulated annealing
 * jppf.load.balancing.profile. The possible move from the best known solution get smaller each
 * time it make a move.
 * This strategy let the algorithm explore the universe of bundle size with
 * an almost known end. Check method getDecreaseRatio about the maximum number
 * of changes.
 * 
 * @author Domingos Creado
 */
public class AnnealingTuneProfile extends AbstractLoadBalancingProfile {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * A default profile with default parameter values.
   */
  private static AtomicReference<AnnealingTuneProfile> defaultProfile = new AtomicReference<>(new AnnealingTuneProfile());
  /**
   * The initial bundle size to start from.
   */
  protected int size = 5;
  /**
   * The minimum number of samples that must be collected before an analysis is triggered.
   */
  protected long minSamplesToAnalyse = 500L;
  /**
   * The minimum number of samples to be collected before checking if the performance profile has changed.
   */
  protected long minSamplesToCheckConvergence = 300L;
  /**
   * The percentage of deviation of the current mean to the mean
   * when the system was considered stable.
   */
  protected double maxDeviation = 0.2d;
  /**
   * The maximum number of guesses of number generated that were already tested
   * for the algorithm to consider the current best solution stable.
   */
  protected int maxGuessToStable = 10;
  /**
   * This parameter defines the multiplicity used to define the range available to
   * random generator, as the maximum.
   */
  protected float sizeRatioDeviation = 1.5f;
  /**
   * This parameter defines how fast does it will stop generating random numbers.
   * This is essential to define what is the size of the universe will be explored.
   * Greater numbers make the algorithm stop sooner.
   * Just as example, if the best solution is between 0-100, the following might
   * occur:
   * <ul style="list-style-type: none; text-indent: -20px">
   * <li>1 => 5 max guesses</li>
   * <li>2 => 2 max guesses</li>
   * <li>0.5 => 9 max guesses</li>
   * <li>0.1 => 46 max guesses</li>
   * <li>0.05 => 96 max guesses</li>
   * </ul>
   * This expected number of guesses might not occur if the number of getMaxGuessToStable()
   * is short.
   */
  protected float decreaseRatio = 0.2f;

  /**
   * Initialize this profile with default values.
   */
  public AnnealingTuneProfile() {
  }

  /**
   * Initialize this profile with values read from the configuration file.
   * @param config contains a mapping of the profile parameters to their value.
   */
  public AnnealingTuneProfile(final TypedProperties config) {
    size = config.getInt("size", 5);
    minSamplesToAnalyse = config.getInt("minSamplesToAnalyse", 500);
    minSamplesToCheckConvergence = config.getInt("minSamplesToCheckConvergence", 300);
    maxDeviation = config.getDouble("maxDeviation", 0.2d);
    maxGuessToStable = config.getInt("maxGuessToStable", 10);
    sizeRatioDeviation = config.getFloat("sizeRatioDeviation", 1.5f);
    decreaseRatio = config.getFloat("decreaseRatio", 0.2f);
    
  }

  /**
   * Get the multiplicity used to define the range available to
   * random generator, as the maximum.
   * @return the multiplicity as a float value.
   */
  public float getSizeRatioDeviation() {
    return sizeRatioDeviation;
  }

  /**
   * Set the multiplicity used to define the range available to
   * random generator, as the maximum.
   * @param sizeRatioDeviation the multiplicity as a float value.
   */
  public void setSizeRatioDeviation(final float sizeRatioDeviation) {
    this.sizeRatioDeviation = sizeRatioDeviation;
  }

  /**
   * Get the decrease rate for this profile.
   * @return the decrease rate as a float value.
   */
  public float getDecreaseRatio() {
    return decreaseRatio;
  }

  /**
   * Set the decrease rate for this profile.
   * @param decreaseRatio the decrease rate as a float value.
   */
  public void setDecreaseRatio(final float decreaseRatio) {
    this.decreaseRatio = decreaseRatio;
  }

  /**
   * Generate a difference to be applied to the best known bundle size.
   * @param bestSize the known best size of bundle.
   * @param collectedSamples the number of samples that were already collected.
   * @param rnd a pseudo-random number generator.
   * @return an always positive diff to be applied to bundle size
   */
  public int createDiff(final int bestSize, final int collectedSamples, final Random rnd) {
    final double max = Math.max(Math.round(bestSize * (getSizeRatioDeviation() - 1.0f)), 1);
    if (max < 1.0d) return 1;
    return rnd.nextInt((int) max) + 1;
  }

  /**
   * This method implements the always decreasing policy of the algorithm.
   * The ratio define how fast this instance will stop generating random
   * numbers.
   * The calculation is performed as max * exp(-x * getDecreaseRatio()).
   * 
   * @param max the maximum value this algorithm will generate.
   * @param x a randomly generated bundle size increment.
   * @return an int value.
   */
  protected double expDist(final long max, final long x) {
    //return max * Math.exp(-x * getDecreaseRatio());
    return (double) max / (double) (x * decreaseRatio);
  }

  /**
   * Get the minimum number of samples that must be collected before an analysis is triggered.
   * @return the number of samples as a long value.
   */
  public long getMinSamplesToAnalyse() {
    return minSamplesToAnalyse;
  }

  /**
   * Set the minimum number of samples that must be collected before an analysis is triggered.
   * @param minSamplesToAnalyse the number of samples as a long value.
   */
  public void setMinSamplesToAnalyse(final long minSamplesToAnalyse) {
    this.minSamplesToAnalyse = minSamplesToAnalyse;
  }

  /**
   * Get the the minimum number of samples to be collected before
   * checking if the performance profile has changed.
   * @return the number of samples as a long value.
   */
  public long getMinSamplesToCheckConvergence() {
    return minSamplesToCheckConvergence;
  }

  /**
   * Set the the minimum number of samples to be collected before
   * checking if the performance profile has changed.
   * @param minSamplesToCheckConvergence the number of samples as a long value.
   */
  public void setMinSamplesToCheckConvergence(final long minSamplesToCheckConvergence) {
    this.minSamplesToCheckConvergence = minSamplesToCheckConvergence;
  }

  /**
   * Get the percentage of deviation of the current mean to the mean
   * when the system was considered stable.
   * @return the percentage of deviation as a double value.
   */
  public double getMaxDeviation() {
    return maxDeviation;
  }

  /**
   * Set the percentage of deviation of the current mean to the mean
   * when the system was considered stable.
   * @param maxDeviation the percentage of deviation as a double value.
   */
  public void setMaxDeviation(final double maxDeviation) {
    this.maxDeviation = maxDeviation;
  }

  /**
   * Get the maximum number of guesses of number generated that were already tested
   * for the algorithm to consider the current best solution stable.
   * @return the number of guesses as an int value.
   */
  public int getMaxGuessToStable() {
    return maxGuessToStable;
  }

  /**
   * Set the maximum number of guesses of number generated that were already tested
   * for the algorithm to consider the current best solution stable.
   * @param maxGuessToStable the number of guesses as an int value.
   */
  public void setMaxGuessToStable(final int maxGuessToStable) {
    this.maxGuessToStable = maxGuessToStable;
  }

  /**
   * Get the default profile with default parameter values.
   * @return a <code>AnnealingTuneProfile</code> singleton instance.
   */
  public static AnnealingTuneProfile getDefaultProfile() {
    return defaultProfile.get();
  }
}
