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

/**
 * Convenience class for collecting time or size statistics.
 * Instances of this class are thread-safe.
 */
public abstract class AbstractJPPFSnapshot implements JPPFSnapshot {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Label for this snapshot, used in the {@link #toString()} method.
   * @exclude
   */
  protected final String label;
  /**
   * The total cumulated values.
   * @exclude
   */
  protected double total = 0d;
  /**
   * The most recent value.
   * @exclude
   */
  protected double latest = 0d;
  /**
   * The minimum value.
   * @exclude
   */
  protected double min = Double.POSITIVE_INFINITY;
  /**
   * The maximum value.
   * @exclude
   */
  protected double max = 0d;
  /**
   * The average value.
   * @exclude
   */
  protected double avg = 0d;
  /**
   * Count of values added to this snapshot.
   * @exclude
   */
  protected long valueCount = 0L;

  /**
   * Initialize this time snapshot with a specified title.
   * @param label the title for this snapshot.
   * @exclude
   */
  public AbstractJPPFSnapshot(final String label) {
    this.label = label;
  }

  /**
   * Make a copy of this time snapshot object.
   * @param ts a new snapshot into which values will be copied
   * @return a <code>TimeSnapshot</code> instance.
   * @exclude
   */
  public synchronized AbstractJPPFSnapshot copy(final AbstractJPPFSnapshot ts) {
    ts.total = total;
    ts.latest = latest;
    ts.min = min;
    ts.max = max;
    ts.avg = avg;
    ts.valueCount = valueCount;
    return ts;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public synchronized void reset() {
    total = 0d;
    latest = 0d;
    min = Double.POSITIVE_INFINITY;
    max = 0d;
    avg = 0d;
    valueCount = 0L;
  }

  /**
   * Assign the value of {@code getLatest()} to {@code max}.
   * @exclude
   */
  public abstract void assignLatestToMax();

  @Override
  public synchronized String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(JPPFStatisticsHelper.getLocalizedLabel(this)).append(": type=").append(getClass().getSimpleName());
    sb.append(": total=").append(total);
    sb.append(", latest=").append(latest);
    sb.append(", min=").append(min);
    sb.append(", max=").append(max);
    sb.append(", avg=").append(avg);
    sb.append(", valueCount=").append(valueCount);
    return sb.toString();
  }

  @Override
  public synchronized double getTotal() {
    return total;
  }

  @Override
  public synchronized double getLatest() {
    return latest;
  }

  @Override
  public synchronized double getMin() {
    if (Double.compare(min, Double.POSITIVE_INFINITY) == 0) return latest;
    else return min;
  }

  @Override
  public synchronized double getMax() {
    return max;
  }

  @Override
  public synchronized double getAvg() {
    return avg;
  }

  @Override
  public synchronized String getLabel() {
    return label;
  }

  @Override
  public synchronized long getValueCount() {
    return valueCount;
  }
}
