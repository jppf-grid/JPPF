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
 * This bundler implementation computes bundle sizes proportional to the mean execution
 * time for each node to the power of n, where n is an integer value specified in the configuration file as "proportionality factor".<br>
 * The scope of this bundler is all nodes, which means that it computes the size for all nodes.<br>
 * The mean execution time is computed as a moving average over a number of tasks, specified in the bundling
 * algorithm profile configuration as &quot;minSamplesToAnalyse&quot;<br>
 * This algorithm is well suited for relatively small networks (a few dozen nodes at most). It generates an overhead
 * every time the performance data for a node is updated. In the case of a small network, this overhead is not
 * large enough to impact the overall performance significantly.
 * @author Laurent Cohen
 */
public class ProportionalBundler extends AbstractAdaptiveBundler<ProportionalProfile> implements PersistentState {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ProportionalBundler.class);
  /**
   * Determines whether debug level is set for logging.
   */
  private static boolean debugEnabled = log.isTraceEnabled();
  /**
   * Determines whether trace level is set for logging.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Mapping of individual bundler to corresponding performance data - global.
   */
  private static final Set<ProportionalBundler> bundlers = new HashSet<>();
  /**
   * The state of this bundler.
   */
  private final BundlerState state;
  /**
   * Lock used to synchronize access to the load-balancer state.
   */
  private final Lock lock = new ReentrantLock();

  /**
   * Creates a new instance with the initial size of bundle as the start size.
   * @param profile the parameters of the auto-tuning algorithm, grouped as a performance analysis profile.
   */
  public ProportionalBundler(final ProportionalProfile profile) {
    super(profile);
    state = new BundlerState();
    state.performanceCache = new PerformanceCache(profile.getPerformanceCacheSize(), profile.getInitialMeanTime());
    state.bundleSize = profile.getInitialSize();
    if (state.bundleSize < 1) state.bundleSize = 1;
    if (debugEnabled) log.debug("Bundler#" + bundlerNumber + ": Using proportional bundle size - the initial size is " + state.bundleSize + ", profile: " + profile);
  }

  /**
   * Get local mapping of individual bundler to corresponding performance data.
   * @return a {@code Set<AbstractProportionalBundler>}.
   */
  protected final Set<ProportionalBundler> getBundlers() {
    return bundlers;
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

  /**
   * Set the current size of bundle.
   * @param size the bundle size as an int value.
   */
  public void setBundleSize(final int size) {
    lock.lock();
    try {
      state.bundleSize = size <= 0 ? 1 : size;
    } finally {
      lock.unlock();
    }
  }

  /**
   * This method delegates the bundle size calculation to the singleton instance of <code>SimpleBundler</code>.
   * @param size the number of tasks executed.
   * @param time the time in nanoseconds it took to execute the tasks.
   */
  @Override
  public void feedback(final int size, final double time) {
    if (traceEnabled) log.trace("Bundler#" + bundlerNumber + ": new performance sample [size=" + size + ", time=" + (long) time + ']');
    if (size <= 0) return;
    PerformanceSample sample = new PerformanceSample(time / size, size);
    synchronized (bundlers) {
      lock.lock();
      try {
        state.performanceCache.addSample(sample);
      } finally {
        lock.unlock();
      }
      computeBundleSizes();
    }
  }

  /**
   * Perform context-independent initializations.
   */
  @Override
  public void setup() {
    synchronized (bundlers) {
      bundlers.add(this);
    }
  }

  /**
   * Release the resources used by this bundler.
   */
  @Override
  public void dispose() {
    super.dispose();
    synchronized (bundlers) {
      bundlers.remove(this);
    }
    lock.lock();
    try {
      state.performanceCache.clear();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Update the bundler sizes.
   */
  private void computeBundleSizes() {
    synchronized (bundlers) {
      double maxMean = Double.NEGATIVE_INFINITY;
      double minMean = Double.POSITIVE_INFINITY;
      ProportionalBundler minBundler = null;
      double meanSum = 0d;
      double m;
      for (ProportionalBundler b : bundlers) {
        b.getStateLock().lock();
        try {
          m = b.state.performanceCache.getMean();
        } finally {
          b.getStateLock().unlock();
        }
        if (m > maxMean) maxMean = m;
        if (m < minMean) {
          minMean = m;
          minBundler = b;
        }
        meanSum += normalize(m);
      }
      int max = maxSize();
      int sum = 0;
      double p;
      for (ProportionalBundler b : bundlers) {
        b.getStateLock().lock();
        try {
          p = normalize(b.state.performanceCache.getMean()) / meanSum;
        } finally {
          b.getStateLock().unlock();
        }
        int size = Math.max(1, (int) (p * max));
        if (size >= max) size = max - 1;
        b.setBundleSize(size);
        sum += size;
      }
      if ((sum < max) && (minBundler != null)) {
        int size = minBundler.getBundleSize();
        minBundler.setBundleSize(size + (max - sum));
      }
      if (traceEnabled) {
        StringBuilder sb = new StringBuilder();
        sb.append("bundler info:\n");
        getStateLock().lock();
        try {
        sb.append("  minMean=").append(minMean).append(", maxMean=").append(maxMean).append(", maxSize=").append(max).append('\n');
        } finally {
          getStateLock().unlock();
        }
        for (ProportionalBundler b : bundlers) {
          b.getStateLock().lock();
          try {
            sb.append("  bundler #").append(b.getBundlerNumber()).append(" : bundleSize=").append(b.getBundleSize()).append(", ");
            sb.append(b.state.performanceCache).append('\n');
          } finally {
            b.getStateLock().unlock();
          }
        }
        log.trace(sb.toString());
      }
    }
  }

  /**
   *
   * @param x .
   * @return .
   */
  private double normalize(final double x) {
    double r = 1.0d;
    for (int i=0; i<profile.getProportionalityFactor(); i++) r *= x;
    return 1.0d / r;
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
  public void setState(final Object o) {
    BundlerState other = (BundlerState) o;
    lock.lock();
    try {
      state.bundleSize = other.bundleSize;
      state.performanceCache = other.performanceCache;
      computeBundleSizes();
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
     * Bounded memory of the past performance updates.
     */
    private PerformanceCache performanceCache;
  }
}
