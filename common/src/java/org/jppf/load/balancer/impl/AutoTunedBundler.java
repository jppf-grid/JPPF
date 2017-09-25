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

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.*;

import org.jppf.load.balancer.*;
import org.jppf.load.balancer.persistence.PersistentState;
import org.slf4j.*;

/**
 * This class implements a self tuned bundle size algorithm. It starts using the
 * bundle size defined in property file and starts changing it to find a better
 * performance. The algorithm starts making The algorithm waits for some
 * execution to get a mean execution time, and them make a change in bundle size
 * Each time the change is done, it is done over a smaller range randomly
 * selected (like Monte Carlo algorithm).
 * 
 * @author Domingos Creado
 * @author Laurent Cohen
 */
public class AutoTunedBundler extends AbstractAdaptiveBundler<AnnealingTuneProfile> implements PersistentState {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AutoTunedBundler.class);
  /**
   * Determines whether trace level is set for logging.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Used to compute a pseudo-random increment to the bundle size, as part of a Monte Carlo random walk
   * towards a good solution.
   */
  private Random rnd = new Random(System.nanoTime());
  /**
   * The state of this undler.
   */
  private final BundlerState state;
  /**
   * Lock used to synchronize access to the load-balancer state.
   */
  private final Lock lock = new ReentrantLock();

  /**
   * Creates a new instance with the initial size of bundle as the start size.
   * @param profile the parameters of the auto-tuning algorithm,
   * grouped as a performance analysis profile.
   */
  public AutoTunedBundler(final AnnealingTuneProfile profile) {
    super(profile);
    this.state = new BundlerState();
    state.bundleSize = profile.size;
    if (state.bundleSize < 1) state.bundleSize = 1;
  }

  @Override
  public int getBundleSize() {
    lock.lock();
    try {
      return state.bundleSize;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void dispose() {
    lock.lock();
    try {
      state.samplesMap.clear();
    } finally {
      lock.unlock();
    }
  }

  /**
   * This method performs the actual bundle size computation, based on current and past
   * performance data.<br>
   * Depending on the the performance samples and profile parameters, the following actions
   * may be triggered in this method:
   * <ul>
   * <li>samples collection (unconditional)</li>
   * <li>detection of performance profile changes, if not currently being done</li>
   * <li>when a performance profile change is detected, recompute the bundle size.</li>
   * </ul>
   * @param bundleSize bundle size of the new performance sample.
   * @param time total execution time of the new sample.
   */
  @Override
  public void feedback(final int bundleSize, final double time) {
    if (traceEnabled) {
      log.trace("Bundler#" + bundlerNumber + ": Got sample with bundleSize=" + bundleSize + " and totalTime=" + time);
    }
    // retrieving the record of the bundle size
    PerformanceSample bundleSample;
    lock.lock();
    try {
      bundleSample = state.samplesMap.get(bundleSize);
      if (bundleSample == null) {
        bundleSample = new PerformanceSample();
        state.samplesMap.put(bundleSize, bundleSample);
      }
      long samples = bundleSample.samples + bundleSize;
      bundleSample.mean = (time + bundleSample.samples * bundleSample.mean) / samples;
      bundleSample.samples = samples;
      if (samples > profile.getMinSamplesToAnalyse()) {
        performAnalysis();
        if (traceEnabled) log.trace("Bundler#" + bundlerNumber + ": bundle size = " + bundleSize);
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Recompute the bundle size after a performance profile change has been detected.
   */
  private void performAnalysis() {
    double stableMean = 0;
    int bestSize = searchBestSize();
    int max = maxSize();
    if ((max > 0) && (bestSize > max)) bestSize = max;
    int counter = 0;
    while (counter < profile.getMaxGuessToStable()) {
      int diff = profile.createDiff(bestSize, state.samplesMap.size(), rnd);
      if (diff < bestSize) {
        // the second part is there to ensure the size is > 0
        if (rnd.nextBoolean()) diff = -diff;
      }
      state.bundleSize = bestSize + diff;
      if (state.samplesMap.get(state.bundleSize) == null) {
        if (traceEnabled) log.trace("Bundler#" + bundlerNumber + ": The next bundle size that will be used is " + state.bundleSize);
        return;
      }
      counter++;
    }

    state.bundleSize = Math.max(1, bestSize);
    PerformanceSample sample = state.samplesMap.get(state.bundleSize);
    if (sample != null) {
      stableMean = sample.mean;
      state.samplesMap.clear();
      state.samplesMap.put(state.bundleSize, sample);
    }
    if (traceEnabled) log.trace("Bundler#" + bundlerNumber + ": The bundle size converged to " + state.bundleSize + " with the mean execution of " + stableMean);
  }

  /**
   * Lookup the best bundle size in the current samples map.
   * @return the best bundle size as an int value.
   */
  private int searchBestSize() {
    int bestSize = 0;
    double minorMean = Double.POSITIVE_INFINITY;
    for (Integer size : state.samplesMap.keySet()) {
      PerformanceSample sample = state.samplesMap.get(size);
      if (sample.mean < minorMean) {
        bestSize = size;
        minorMean = sample.mean;
      }
    }
    if (traceEnabled) log.trace("Bundler#" + bundlerNumber + ": best size found = " + bestSize);
    return bestSize;
  }

  @Override
  public Object getState() {
    lock.lock();
    try {
      return state;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void setState(final Object persistedState) {
    BundlerState other = (BundlerState) persistedState;
    lock.lock();
    try {
      state.bundleSize = other.bundleSize;
      state.samplesMap = other.samplesMap;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Lock getStateLock() {
    return lock;
  }

  /**
   * Holds the state of this bundler for persistence.
   */
  private static class BundlerState implements Serializable {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * The current bundle size.
     */
    private int bundleSize = 1;
    /**
     * A map of performance samples, sorted by increasing bundle size.
     */
    private Map<Integer, PerformanceSample> samplesMap = new HashMap<>();
  }
}
