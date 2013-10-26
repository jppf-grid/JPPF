/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import org.jppf.utils.CollectionUtils;

/**
 * Convenience class for collecting time or size statistics.
 * Instances of this class are thread-safe.
 */
public class JPPFSnapshot implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Label for this snapshot, used in the {@link #toString()} method.
   */
  private final String label;
  /**
   * The total cumulated values.
   */
  private double total = 0d;
  /**
   * The most recent value.
   */
  private double latest = 0d;
  /**
   * The minimum value.
   */
  private double min = Double.POSITIVE_INFINITY;
  /**
   * The maximum value.
   */
  private double max = 0d;
  /**
   * The average value.
   */
  private double avg = 0d;
  /**
   * Count of values added to this snapshot.
   */
  private long valueCount = 0L;
  /**
   * Determines whether updates are accumulated instead of simply stored as latest value.
   */
  private final boolean cumulative;

  /**
   * Initialize this time snapshot with a specified title.
   * @param label the title for this snapshot.
   */
  public JPPFSnapshot(final String label)
  {
    this(label, false);
  }

  /**
   * Initialize this time snapshot with a specified title.
   * @param label the title for this snapshot.
   * @param cumulative determines whether updates are accumulated instead of simply stored as latest value.
   * When true then <code>latest</code> is always equal to <code>total</code>.
   */
  public JPPFSnapshot(final String label, final boolean cumulative)
  {
    this.label = label;
    this.cumulative = cumulative;
  }

  /**
   * Add a set of aggregated values to this snapshot.
   * @param accumulatedValues the accumulated sum of the values to add.
   * @param count the number of values in the accumalated values.
   */
  synchronized void addValues(final double accumulatedValues, final long count)
  {
    total += accumulatedValues;
    if (count > 0L)
    {
      valueCount += count;
      if (cumulative) latest += accumulatedValues;
      else latest = (count == 1L) ? accumulatedValues : accumulatedValues / count;
      if (latest > max) max = latest;
      if (latest < min) min = latest;
      if (valueCount != 0d) avg = total / valueCount;
    }
  }

  /**
   * Make a copy of this time snapshot object.
   * @return a <code>TimeSnapshot</code> instance.
   */
  public synchronized JPPFSnapshot copy()
  {
    JPPFSnapshot ts = new JPPFSnapshot(label);
    ts.total = total;
    ts.latest = latest;
    ts.min = min;
    ts.max = max;
    ts.avg = avg;
    ts.valueCount = valueCount;
    return ts;
  }

  /**
   * Reset all counters to their initial values.
   */
  public synchronized void reset()
  {
    total = 0d;
    latest = 0d;
    min = Double.POSITIVE_INFINITY;
    max = 0d;
    avg = 0d;
    valueCount = 0L;
  }

  /**
   * Get a string representation of this stats object.
   * @return a string display the various stats values.
   * @see java.lang.Object#toString()
   */
  @Override
  public synchronized String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(label).append(": total=").append(total);
    sb.append(", latest=").append(latest);
    sb.append(", min=").append(min);
    sb.append(", max=").append(max);
    sb.append(", avg=").append(avg);
    sb.append(", valueCount=").append(valueCount);
    return sb.toString();
  }

  /**
   * Get the total cumulated time / size.
   * @return the total time as a long value.
   */
  public synchronized double getTotal()
  {
    return total;
  }

  /**
   * Get the minimum time / size observed.
   * @return the minimum time as a long value.
   */
  public synchronized double getLatest()
  {
    return latest;
  }

  /**
   * Get the smallest time / size observed.
   * @return the minimum time as a long value.
   */
  public synchronized double getMin()
  {
    if (Double.compare(min, Double.POSITIVE_INFINITY) == 0) return latest;
    else return min;
  }

  /**
   * Get the peak time / size.
   * @return the maximum time as a long value.
   */
  public synchronized double getMax()
  {
    return max;
  }

  /**
   * Get the average time / size.
   * @return the average time as a double value.
   */
  public synchronized double getAvg()
  {
    return avg;
  }

  /**
   * Get the label for this snapshot.
   * @return the label as a string.
   */
  public synchronized String getLabel()
  {
    return label;
  }

  /**
   * Get the count of values added to this snapshot.
   * @return the count as a long value.
   */
  public synchronized long getValueCount()
  {
    return valueCount;
  }

  /**
   * A filter interface for snapshots.
   */
  public interface Filter
  {
    /**
     * Determines whether the specified snapshot is accepted by this filter.
     * @param snapshot the snapshot to check.
     * @return <code>true</code> if the snapshot is accepted, <code>false</code> otherwise.
     */
    boolean accept(JPPFSnapshot snapshot);
  }

  /**
   * Base class for filters including or excluding snpashots based on a provided set of labels.
   */
  public abstract static class LabelBasedFilter implements Filter
  {
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
    protected LabelBasedFilter(final boolean includeFlag, final String...labels)
    {
      this.includeFlag = includeFlag;
      this.labels = CollectionUtils.set(labels);
    }

    /**
     * Initialize with the specified collection of labels.
     * @param includeFlag determines whether to include or exclude the snapshots.
     * @param labels the labels to exclude.
     */
    protected LabelBasedFilter(final boolean includeFlag, final Collection<String> labels)
    {
      this.includeFlag = includeFlag;
      this.labels = new HashSet(labels);
    }

    @Override
    public boolean accept(final JPPFSnapshot snapshot)
    {
      boolean b = labels.contains(snapshot.getLabel());
      return includeFlag ? b : !b;
    }
  }

  /**
   * Filter including snpashots based on a provided set of labels.
   */
  public static class LabelIncludingFilter extends LabelBasedFilter
  {
    /**
     * Initialize with the specified array of labels.
     * @param labels the labels to include.
     */
    public LabelIncludingFilter(final String...labels)
    {
      super(true, labels);
    }

    /**
     * Initialize with the specified collection of labels.
     * @param labels the labels to include.
     */
    public LabelIncludingFilter(final Collection<String> labels)
    {
      super(true, labels);
    }
  }

  /**
   * Filter excluding snpashots based on a provided set of labels.
   */
  public static class LabelExcludingFilter extends LabelBasedFilter
  {
    /**
     * Initialize with the specified array of labels.
     * @param labels the labels to exclude.
     */
    public LabelExcludingFilter(final String...labels)
    {
      super(false, labels);
    }

    /**
     * Initialize with the specified collection of labels.
     * @param labels the labels to exclude.
     */
    public LabelExcludingFilter(final Collection<String> labels)
    {
      super(false, labels);
    }
  }
}
