/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
package org.jppf.utils;

import java.io.Serializable;

/**
 * Convenience class for collecting time or size statistics.
 */
public class StatsSnapshot implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Title for this snapshot, used in the {@link #toString()} method.
   */
  private final String title;
  /**
   * The total cumulated time.
   */
  private double total = 0L;
  /**
   * The most recent time / size.
   */
  private double latest = 0L;
  /**
   * The minimum time / size.
   */
  private double min = Long.MAX_VALUE;
  /**
   * The maximum time / size.
   */
  private double max = 0L;
  /**
   * The average time / size.
   */
  private double avg = 0d;

  /**
   * Initialize this time snapshot with a specified title.
   * @param title the title for this snapshot.
   */
  public StatsSnapshot(final String title)
  {
    this.title = title;
  }

  /**
   * Called when a new time has been collected.
   * @param time the new time used to compute the new statistics of this time snapshot.
   * @param count the unit count to which the time applies.
   * @param totalCount the total unit count to which the time applies.
   */
  public void newValues(final long time, final long count, final long totalCount)
  {
    total += time;
    if (count > 0)
    {
      latest = time/count;
      if (latest > max) max = latest;
      if (latest < min) min = latest;
      if (totalCount > 0) avg = (double) total / (double) totalCount;
    }
  }

  /**
   * Called when a new time has been collected.
   * @param updateCount the number of units in the update.
   * @param totalUpdates the total number of updates since the start, not including the current update.
   */
  public void newValues(final long updateCount, final long totalUpdates)
  {
    total += updateCount;
    latest = updateCount;
    if (latest > max) max = latest;
    if (latest < min) min = latest;
    avg = (double) total / (double) (totalUpdates + 1L);
  }

  /**
   * Make a copy of this time snapshot object.
   * @return a <code>TimeSnapshot</code> instance.
   */
  public StatsSnapshot copy()
  {
    StatsSnapshot ts = new StatsSnapshot(title);
    ts.setTotal(total);
    ts.setLatest(latest);
    ts.setMin(min);
    ts.setMax(max);
    ts.setAvg(avg);
    return ts;
  }

  /**
   * Get a string representation of this stats object.
   * @return a string display the various stats values.
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(title).append(": total=").append(total);
    sb.append(", latest=").append(latest);
    sb.append(", min=").append(min);
    sb.append(", max=").append(max);
    sb.append(", avg=").append(avg);
    return sb.toString();
  }

  /**
   * Set the total cumulated time / size.
   * @param total the total time as a long value.
   */
  public void setTotal(final double total)
  {
    this.total = total;
  }

  /**
   * Get the total cumulated time / size.
   * @return the total time as a long value.
   */
  public double getTotal()
  {
    return total;
  }

  /**
   * Set the most recent time / size observed.
   * @param latest the most recent time as a long value.
   */
  public void setLatest(final double latest)
  {
    this.latest = latest;
  }

  /**
   * Get the minimum time / size observed.
   * @return the minimum time as a long value.
   */
  public double getLatest()
  {
    return latest;
  }

  /**
   * Set the smallest time / size observed.
   * @param min the minimum time as a long value.
   */
  public void setMin(final double min)
  {
    this.min = min;
  }

  /**
   * Get the smallest time / size observed.
   * @return the minimum time as a long value.
   */
  public double getMin()
  {
    return min;
  }

  /**
   * Set the peak time / size.
   * @param max the maximum time as a long value.
   */
  public void setMax(final double max)
  {
    this.max = max;
  }

  /**
   * Get the peak time / size.
   * @return the maximum time as a long value.
   */
  public double getMax()
  {
    return max;
  }

  /**
   * Set the average time / size.
   * @param avg the average time as a double value.
   */
  public void setAvg(final double avg)
  {
    this.avg = avg;
  }

  /**
   * Get the average time / size.
   * @return the average time as a double value.
   */
  public double getAvg()
  {
    return avg;
  }
}
