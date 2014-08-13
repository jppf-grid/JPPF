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

package org.jppf.utils.stats;

/**
 * In this implementation, {@code getLatest()} is computed as the cumulated sum of all values added to the snapshot.
 * If values are only added, and not removed, then it will always return the same value as getTotal().
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
   * @exclude
   */
  public CumulativeSnapshot(final String label)
  {
    super(label);
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
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

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public synchronized void assignLatestToMax()
  {
    max = latest;
    total = latest;
    valueCount = 1L;
    avg = latest;
    min = 0d;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public JPPFSnapshot copy()
  {
    return copy(new CumulativeSnapshot(getLabel()));
  }
}
