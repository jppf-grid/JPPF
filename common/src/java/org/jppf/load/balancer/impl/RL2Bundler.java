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
import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * Bundler based on a reinforcement learning algorithm.
 * @author Laurent Cohen
 */
public class RL2Bundler extends AbstractAdaptiveBundler<RL2Profile> implements PersistentState {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RL2Bundler.class);
  /**
   * Determines whether debugging level is set for logging.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether trace level is set for logging.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * PRNG used to chose random bundle sizes during the learning phase.
   */
  private final Random rand = new Random(System.nanoTime());
  /**
   * The maximum allowed bundle size.
   */
  private int maxSize = 1;
  /**
   * Name given to this bunlder for logging purposes.
   */
  private final String name;
  /**
   * The state of this bundler, encapsulated in a spearte class for persistence.
   */
  private final RL2State rl2State;
  /**
   * Lock used to synchronize access to the load-balancer state.
   */
  private final Lock lock = new ReentrantLock();

  /**
   * Creates a new instance with the specified parameters.
   * @param profile the parameters of the algorithm, grouped as a performance analysis profile.
   */
  public RL2Bundler(final RL2Profile profile) {
    super(profile);
    this.name = "bundler #" + bundlerNumber + ": ";
    rl2State = new RL2State();
    if (debugEnabled) log.debug(format("RL2 algorithm, initial size=%d, profile=%s", rl2State.bundleSize, profile));
    rl2State.performanceCache = new PerformanceCache(profile.getPerformanceCacheSize());
  }

  @Override
  public int getBundleSize() {
    lock.lock();
    try {
      return rl2State.bundleSize;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void feedback(final int size, final double totalTime) {
    //if (traceEnabled) log.trace(format("feedback size=%,d, totalTime=%f", size, totalTime));
    if (size <= 0) return;
    lock.lock();
    try {
      rl2State.performanceCache.addSample(new PerformanceSample(totalTime / size, size));
      computeBundleSize(size, totalTime);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Compute the new bundle size base on the specified performance feedback.
   * @param size the size of the task s bundle that was returned.
   * @param totalTime the total round-trip time of the bundle between the driver and the node.
   */
  private void computeBundleSize(final int size, final double totalTime) {
    final double diff = (rl2State.performanceCache.getPreviousMean() - rl2State.performanceCache.getMean()) / rl2State.performanceCache.getPreviousMean();
    // if a negative difference in performance is beyond the configured threshold,
    // assume the performance profile has changed and re-learn it from scratch
    if (diff < -profile.getPerformanceVariationThreshold()) {
      if (debugEnabled) log.debug(format("resetting states (diff=%,f, threshold=%,f)", diff, profile.getPerformanceVariationThreshold()));
      rl2State.statesBySize.clear();
      rl2State.statesByTime.clear();
    }
    State state = rl2State.statesBySize.get(rl2State.bundleSize);
    if (state == null) {
      state = new State();
      state.size = size;
      state.mean = totalTime / size;
      state.total = state.mean;
      state.count = 1;
      rl2State.statesBySize.put(rl2State.bundleSize, state);
    } else {
      rl2State.statesByTime.removeValue(state.mean, state);
      final double mean = totalTime / size;
      //if (mean < state.mean) state.mean = mean;
      state.count++;
      state.total += mean;
      state.mean = state.total / state.count;
    }
    rl2State.statesByTime.putValue(state.mean, state);
    boolean choseRandom = true;
    final int nbStates = rl2State.statesBySize.size();
    double p = 0d;
    // if nbStates < minSamples, keep building the set of states by chosing the next bundle size randomly
    if (nbStates < profile.getMinSamples()) choseRandom = true;
    // if minSamples <= nbStates < maxSamples, compute a decreasing probability to chose the next bundle size randomly
    else if (nbStates < profile.getMaxSamples()) {
      // probability to chose the bundle size randomly
      p = (profile.getMaxSamples() - nbStates) / (1d + (profile.getMaxSamples() - profile.getMinSamples()));
      choseRandom = rand.nextDouble() <= p;
      //if (debugEnabled) log.debug(format("p=%,f, choseRandom=%b, nbStates=%,d", p, choseRandom, nbStates));
    }
    // if nbStates >= maxSamples, use the state that produced the best performance (no random selection)
    else choseRandom = false;

    if (choseRandom && (nbStates < maxSize)) {
      int n = 0;
      do {
        n = 1 + rand.nextInt(maxSize);
      } while (rl2State.statesBySize.get(n) != null);
      rl2State.bundleSize = n;
    } else {
      final double key = rl2State.statesByTime.firstKey();
      final List<State> list = new ArrayList<>(rl2State.statesByTime.getValues(key));
      final int listSize = list.size();
      final int idx = (listSize == 1) ? 0 : rand.nextInt(list.size());
      rl2State.bundleSize = list.get(idx).size;
    }
    if (traceEnabled) log.trace(format("bundleSize=%,4d, nbStates=%,4d, choseRandom=%5b, p=%1.3f, feedback=(size=%,4d, totalTIme=%,10d)",
      rl2State.bundleSize, nbStates, choseRandom, p, size, (long) totalTime));
    if (debugEnabled) log.debug("RL2 new bundle size for {} is {}", name, rl2State.bundleSize);
  }

  @Override
  public void setJob(final JPPFDistributedJob job) {
    super.setJob(job);
    final int n = job.getTaskCount();
    maxSize = (int) Math.round(n * profile.getMaxRelativeSize());
    if (maxSize < 1) maxSize = 1;
    lock.lock();
    try {
      if (!rl2State.statesBySize.isEmpty() && (rl2State.statesBySize.lastKey() > maxSize)) {
        final Map<Integer, State> map = new HashMap<>(rl2State.statesBySize.tailMap(maxSize + 1));
        for (final Map.Entry<Integer, State> entry: map.entrySet()) {
          final State state = rl2State.statesBySize.remove(entry.getKey());
          if (state != null) rl2State.statesByTime.removeValue(state.mean, state);
        }
      }
      if (rl2State.statesBySize.isEmpty()) rl2State.bundleSize = 1 + rand.nextInt(maxSize);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void setup() {
  }

  @Override
  public void dispose() {
    super.dispose();
    lock.lock();
    try {
      rl2State.performanceCache.clear();
      rl2State.statesByTime.clear();
      rl2State.statesBySize.clear();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Format the specified message.
   * @param format the parametrized message format.
   * @param params the paramaters.
   * @return a formatted string with this bundler's name as prefix.
   */
  private String format(final String format, final Object...params) {
    final String s = name + format;
    return String.format(s, params);
  }

  /**
   * Instances of this class represent the bundler state after a bundle has been applied to
   * a job and the results have been received.
   */
  private static class State implements Serializable {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Number of tasks.
     */
    private int size;
    /**
     * Mean task execution time.
     */
    private double mean;
    /**
     *
     */
    private double total;
    /**
     *
     */
    private int count;
  }

  @Override
  public Object getState() {
    lock.lock();
    try {
      return rl2State;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void setState(final Object persistedState) {
    final RL2State other = (RL2State) persistedState;
    lock.lock();
    try {
      rl2State.bundleSize = other.bundleSize;
      rl2State.performanceCache = other.performanceCache;
      rl2State.statesBySize = other.statesBySize;
      rl2State.statesByTime = other.statesByTime;
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
  private static class RL2State implements Serializable {
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
    /**
     * The states sorted by ascending bundle size.
     */
    private SortedMap<Integer, State> statesBySize = new TreeMap<>();
    /**
     * The states sorted by ascending mean execution time.
     */
    private CollectionSortedMap<Double, State> statesByTime = new SetSortedMap<>();
  }
}
