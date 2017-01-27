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
package org.jppf.utils.stats;

import java.io.Serializable;
import java.util.*;

import org.jppf.utils.collections.CollectionUtils;

/**
 * Convenience class for collecting time or size statistics.
 * Instances of this class are thread-safe.
 */
public interface JPPFSnapshot extends Serializable {
  /**
   * Add a set of aggregated values to this snapshot.
   * @param accumulatedValues the accumulated sum of the values to add.
   * @param count the number of values in the accumalated values.
   * @exclude
   */
  void addValues(final double accumulatedValues, final long count);

  /**
   * Make a copy of this time snapshot object.
   * @return a <code>TimeSnapshot</code> instance.
   * @exclude
   */
  JPPFSnapshot copy();

  /**
   * Reset all counters to their initial values.
   * @exclude
   */
  void reset();

  /**
   * Get the total cumulated sum of the values.
   * @return the cumulated values as a double.
   */
  double getTotal();

  /**
   * Get the latest observed value.
   * @return the latest value as a double.
   */
  double getLatest();

  /**
   * Get the smallest observed value.
   * @return the minimum value as a double.
   */
  double getMin();

  /**
   * Get the peak observed value.
   * @return the peak value double.
   */
  double getMax();

  /**
   * Get the average value.
   * @return the average value as a double.
   */
  double getAvg();

  /**
   * Get the label for this snapshot.
   * @return the label as a string.
   */
  String getLabel();

  /**
   * Get the count of values added to this snapshot.
   * @return the count of values as a long.
   */
  long getValueCount();

  /**
   * Base class for filters including or excluding snpashots based on a provided set of labels.
   * @exclude
   */
  public abstract static class LabelBasedFilter implements JPPFStatistics.Filter {
    /**
     * The set of labels to exclude.
     */
    protected final Set<String> labels;
    /**
     * Determines whether to include or exclude the snapshots.
     */
    protected final boolean includeFlag;

    /**
     * Initialize with the specified array of labels.
     * @param includeFlag determines whether to include or exclude the snapshots.
     * @param labels the labels to exclude.
     */
    protected LabelBasedFilter(final boolean includeFlag, final String... labels) {
      this.includeFlag = includeFlag;
      this.labels = CollectionUtils.set(labels);
    }

    /**
     * Initialize with the specified collection of labels.
     * @param includeFlag determines whether to include or exclude the snapshots.
     * @param labels the labels to exclude.
     */
    protected LabelBasedFilter(final boolean includeFlag, final Collection<String> labels) {
      this.includeFlag = includeFlag;
      this.labels = new HashSet<>(labels);
    }

    @Override
    public boolean accept(final JPPFSnapshot snapshot) {
      boolean b = labels.contains(snapshot.getLabel());
      return includeFlag ? b : !b;
    }
  }

  /**
   * Filter including snpashots based on a provided set of labels.
   * @exclude
   */
  public static class LabelIncludingFilter extends LabelBasedFilter {
    /**
     * Initialize with the specified array of labels.
     * @param labels the labels to include.
     */
    public LabelIncludingFilter(final String... labels) {
      super(true, labels);
    }

    /**
     * Initialize with the specified collection of labels.
     * @param labels the labels to include.
     */
    public LabelIncludingFilter(final Collection<String> labels) {
      super(true, labels);
    }
  }

  /**
   * Filter excluding snpashots based on a provided set of labels.
   * @exclude
   */
  public static class LabelExcludingFilter extends LabelBasedFilter {
    /**
     * Initialize with the specified array of labels.
     * @param labels the labels to exclude.
     */
    public LabelExcludingFilter(final String... labels) {
      super(false, labels);
    }

    /**
     * Initialize with the specified collection of labels.
     * @param labels the labels to exclude.
     */
    public LabelExcludingFilter(final Collection<String> labels) {
      super(false, labels);
    }
  }
}
