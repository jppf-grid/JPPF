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

import java.util.*;

import org.jppf.load.balancer.*;
import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * Bundler based on a reinforcement learning algorithm.
 * @author Laurent Cohen
 */
public class RL2Bundler extends AbstractAdaptiveBundler<RL2Profile> {
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
   * Bounded memory of the past performance updates.
   */
  private final BundleDataHolder dataHolder;
  /**
   * PRNG used to chose random bundle sizes during the learning phase.
   */
  private final Random rand = new Random(System.nanoTime());
  /**
   * The states sorted by ascending bundle size.
   */
  private SortedMap<Integer, State> statesBySize = new TreeMap<>();
  /**
   * The states sorted by ascending mean execution time.
   */
  private CollectionSortedMap<Double, State> statesByTime = new SetSortedMap<>();
  /**
   * The maximum allowed bundle size.
   */
  private int maxSize = 1;
  /**
   * 
   */
  private final String name;

  /**
   * Creates a new instance with the specified parameters.
   * @param profile the parameters of the algorithm, grouped as a performance analysis profile.
   */
  public RL2Bundler(final RL2Profile profile) {
    super(profile);
    this.name = "bundler #" + bundlerNumber + ": ";
    if (debugEnabled) log.debug(format("RL2 algorithm, initial size=%d, profile=%s", bundleSize, profile));
    this.dataHolder = new BundleDataHolder(profile.getPerformanceCacheSize());
  }

  @Override
  public void feedback(final int size, final double totalTime) {
    //if (traceEnabled) log.trace(format("feedback size=%,d, totalTime=%f", size, totalTime));
    if (size <= 0) return;
    dataHolder.addSample(new BundlePerformanceSample(totalTime / size, size));
    computeBundleSize(size, totalTime);
  }

  /**
   * Compute the new bundle size base on the specified performance feedback.
   * @param size the size of the task s bundle that was returned.
   * @param totalTime the total round-trip time of the bundle between the driver and the node.
   */
  protected void computeBundleSize(final int size, final double totalTime) {
    double diff = (dataHolder.getPreviousMean() - dataHolder.getMean()) / dataHolder.getPreviousMean();
    // if a negative difference in performance is beyond the configured threshold,
    // assume the performance profile has changed and re-learn it from scratch
    if (diff < -profile.getPerformanceVariationThreshold()) {
    //if (Math.abs(diff) > profile.getPerformanceVariationThreshold()) {
      if (debugEnabled) log.debug(format("resetting states (diff=%,f, threshold=%,f)", diff, profile.getPerformanceVariationThreshold()));
      statesBySize.clear();
      statesByTime.clear();
    }
    State state = statesBySize.get(bundleSize);
    if (state == null) {
      state = new State();
      state.size = size;
      state.mean = totalTime / size;
      state.total = state.mean;
      state.count = 1;
      statesBySize.put(bundleSize, state);
    } else {
      statesByTime.removeValue(state.mean, state);
      double mean = totalTime / size;
      //if (mean < state.mean) state.mean = mean;
      state.count++;
      state.total += mean;
      state.mean = state.total / state.count;
    }
    statesByTime.putValue(state.mean, state);
    boolean choseRandom = true;
    int nbStates = statesBySize.size();
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
      } while (statesBySize.get(n) != null);
      bundleSize = n;
    } else {
      double key = statesByTime.firstKey();
      List<State> list = new ArrayList<>(statesByTime.getValues(key));
      int listSize = list.size();
      int idx = (listSize == 1) ? 0 : rand.nextInt(list.size());
      bundleSize = list.get(idx).size;
    }
    if (traceEnabled) log.trace(format("bundleSize=%,4d, nbStates=%,4d, choseRandom=%5b, p=%1.3f, feedback=(size=%,4d, totalTIme=%,10d)",
      bundleSize, nbStates, choseRandom, p, size, (long) totalTime));
  }

  @Override
  public void setJob(final JPPFDistributedJob job) {
    super.setJob(job);
    int n = job.getTaskCount();
    maxSize = (int) Math.round(n * profile.getMaxRelativeSize());
    if (maxSize < 1) maxSize = 1;
    if (!statesBySize.isEmpty() && (statesBySize.lastKey() > maxSize)) {
      Map<Integer, State> map = new HashMap<>(statesBySize.tailMap(maxSize + 1));
      for (Map.Entry<Integer, State> entry: map.entrySet()) {
        State state = statesBySize.remove(entry.getKey());
        if (state != null) statesByTime.removeValue(state.mean, state);
      }
    }
    if (statesBySize.isEmpty()) bundleSize = 1 + rand.nextInt(maxSize);
  }

  @Override
  public void setup() {
  }

  @Override
  public void dispose() {
    super.dispose();
    dataHolder.clear();
    statesByTime.clear();
    statesBySize.clear();
  }

  /**
   * Format the specified message.
   * @param format the parametrized message format.
   * @param params the paramaters.
   * @return a formatted string with this bundler's name as prefix.
   */
  private String format(final String format, final Object...params) {
    String s = name + format;
    return String.format(s, params);
  }

  /**
   * Instances of tis class represent the bundler state after a bundle has been applied to
   * a job and the results have been received.
   */
  private static class State {
    /**
     * Number of tasks.
     */
    public int size;
    /**
     * Mean task execution size.
     */
    public double mean;
    /**
     * 
     */
    public double total;
    /**
     * 
     */
    public int count;
  }
}
