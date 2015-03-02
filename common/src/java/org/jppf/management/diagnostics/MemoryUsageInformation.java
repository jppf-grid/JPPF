/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
import java.lang.management.MemoryUsage;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * A <code>Serializable</code> implementation of {@link MemoryUsage}.
 * @author Laurent Cohen
 */
public class MemoryUsageInformation implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Initial memory size.
   */
  private final long init;
  /**
   * Current memory size.
   */
  private final long committed;
  /**
   * Current used memory.
   */
  private final long used;
  /**
   * Maximum memory size.
   */
  private final long max;

  /**
   * Initialize this object from a {@link MemoryUsage} instance.
   * @param memUsage contains the memroy usage information to use.
   */
  public MemoryUsageInformation(final MemoryUsage memUsage)
  {
    this.init = memUsage.getInit();
    this.committed = memUsage.getCommitted();
    this.used = memUsage.getUsed();
    this.max = memUsage.getMax();
  }

  /**
   * Get the initial memory size.
   * @return the initial memory size.
   */
  public long getInit()
  {
    return init;
  }

  /**
   * Get the current memory size.
   * @return the current memory size.
   */
  public long getCommitted()
  {
    return committed;
  }

  /**
   * Get the used memory size.
   * @return the used memory size.
   */
  public long getUsed()
  {
    return used;
  }

  /**
   * Get the maximum memory size.
   * @return the maximum memory size.
   */
  public long getMax()
  {
    return max;
  }

  /**
   * Return the ratio of used memory over maximum available memory.
   * @return the ratio as a double value in the range [0, 1], or -1 if the maximum memory information is not available. 
   */
  public double getUsedRatio()
  {
    if (max <= 0) return -1d;
    return (double) used / (double) max;
  }

  @Override
  public String toString()
  {
    NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
    nf.setGroupingUsed(true);
    nf.setMinimumIntegerDigits(9);
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    sb.append("init=").append(nf.format(init));
    sb.append(", committed=").append(nf.format(committed));
    sb.append(", used=").append(nf.format(used));
    sb.append(", max=").append(nf.format(max));
    sb.append(']');
    return sb.toString();
  }
}
