/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.management.diagnostics;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Locale;

import org.jppf.utils.StringUtils;

/**
 * This class represents a snapshot of the JVM health.
 * @author Laurent Cohen
 */
public class HealthSnapshot implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Ratio of used / max for heap memory.
   */
  double heapUsedRatio = -1d;
  /**
   * Used heap memory in bytes.
   */
  long heapUsed = -1L;
  /**
   * Ratio of used / max for non-heap memory.
   */
  double nonheapUsedRatio = -1d;
  /**
   * Used non-heap memory in bytes.
   */
  long nonheapUsed = -1L;
  /**
   * Determines whether a deadlock was detected.
   */
  boolean deadlocked = false;
  /**
   * Number of live threads in the JVM.
   */
  int liveThreads = -1;
  /**
   * The system load.
   */
  double cpuLoad = -1d;

  /**
   * Intiialize this snapshot with default values.
   */
  public HealthSnapshot()
  {
  }

  /**
   * Get the ratio of used / max for heap memory.
   * @return the ratio as a double value in the range [0, 1].
   */
  public double getHeapUsedRatio()
  {
    return heapUsedRatio;
  }

  /**
   * Get the ratio of used / max for non-heap memory.
   * @return the ratio as a double value in the range [0, 1].
   */
  public double getNonheapUsedRatio()
  {
    return nonheapUsedRatio;
  }

  /**
   * Determine whether a deadlock was detected.
   * @return <code>true</code> if a deadlock was dertected, <code>false</code> otherwise.
   */
  public boolean isDeadlocked()
  {
    return deadlocked;
  }

  /**
   * Get the used heap memory in bytes.
   * @return the heap used as a long.
   */
  public long getHeapUsed()
  {
    return heapUsed;
  }

  /**
   * Get the used non-heap memory in bytes.
   * @return the non-heap used as a long.
   */
  public long getNonheapUsed()
  {
    return nonheapUsed;
  }

  /**
   * Get the number of live threads in the JVM.
   * @return the number of threads as an int.
   */
  public int getLiveThreads()
  {
    return liveThreads;
  }

  /**
   * Get the cpu load.
   * @return the cpu load as a double.
   */
  public double getCpuLoad()
  {
    return cpuLoad;
  }

  @Override
  public String toString()
  {
    return "HealthSnapshot [heapUsedRatio=" + heapUsedRatio + ", heapUsed=" + heapUsed + ", nonheapUsedRatio="
        + nonheapUsedRatio + ", nonheapUsed=" + nonheapUsed + ", deadlocked=" + deadlocked + ", liveThreads="
        + liveThreads + ", cpuLoad=" + cpuLoad + "]";
  }

  /**
   * Get this snapshot in an easily readable format, according to the default locale.
   * @return a string representation of this snapshot.
   */
  public String toFormattedString()
  {
    return toFormattedString(Locale.getDefault());
  }

  /**
   * Get this snapshot in an easily readable format.
   * @param locale the locale to use for formatting.
   * @return a string representation of this snapshot.
   */
  public String toFormattedString(final Locale locale)
  {
    Locale l = locale == null ? Locale.US : locale;
    NumberFormat nf = NumberFormat.getNumberInstance(l);
    final double mb = 1024d * 1024d;
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    nf.setGroupingUsed(true);
    nf.setMaximumFractionDigits(1);
    nf.setMinimumFractionDigits(1);
    nf.setMinimumIntegerDigits(1);
    sb.append("heapUsedRatio=").append(format(100d*heapUsedRatio, nf)).append(" %");
    sb.append("; heapUsed=").append(format(heapUsed/mb, nf)).append(" MB");
    sb.append("; nonheapUsedRatio=").append(format(100d*nonheapUsedRatio, nf)).append(" %");
    sb.append("; nonheapUsed=").append(format(nonheapUsed/mb, nf)).append(" MB");
    sb.append("; deadlocked=").append(deadlocked);
    sb.append("; liveThreads=").append(liveThreads);
    sb.append("; cpuLoad=");
    if (cpuLoad < 0d) sb.append("  n/a ");
    else sb.append(format(100d*cpuLoad, nf)).append(" %");
    sb.append(']');
    return sb.toString();
  }

  /**
   * Format a value.
   * @param value the value to format.
   * @param nf the number format to use.
   * @return a formatted string.
   */
  private String format(final double value, final NumberFormat nf)
  {
    return StringUtils.padLeft(nf.format(value), ' ', 6);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp = Double.doubleToLongBits(cpuLoad);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + (deadlocked ? 1231 : 1237);
    result = prime * result + (int) (heapUsed ^ (heapUsed >>> 32));
    result = prime * result + liveThreads;
    result = prime * result + (int) (nonheapUsed ^ (nonheapUsed >>> 32));
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    HealthSnapshot other = (HealthSnapshot) obj;
    //if (Double.doubleToLongBits(cpuLoad) != Double.doubleToLongBits(other.cpuLoad)) return false;
    if (cpuLoad != other.cpuLoad) return false;
    if (deadlocked != other.deadlocked) return false;
    if (heapUsed != other.heapUsed) return false;
    if (liveThreads != other.liveThreads) return false;
    if (nonheapUsed != other.nonheapUsed) return false;
    return true;
  }
}
