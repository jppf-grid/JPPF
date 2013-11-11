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

/**
 * 
 * @author Laurent Cohen
 */
public class CumulativeSnapshot extends AbstractJPPFSnapshot
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this snapshot with a specified title.
   * @param label the title for this snapshot.
   */
  public CumulativeSnapshot(final String label)
  {
    super(label);
  }

  /**
   * Add a set of aggregated values to this snapshot.
   * @param accumulatedValues the accumulated sum of the values to add.
   * @param count the number of values in the accumalated values.
   */
  public synchronized void addValues(final double accumulatedValues, final long count)
  {
    total += accumulatedValues;
    if (count > 0L)
    {
      valueCount += count;
      latest += accumulatedValues;
      if (latest > max) max = latest;
      if (latest < min) min = latest;
      if (valueCount != 0d) avg = total / valueCount;
    }
  }

  @Override
  public JPPFSnapshot copy()
  {
    return copy(new CumulativeSnapshot(getLabel()));
  }
}
