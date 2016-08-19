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

package org.jppf.utils.stats;

/**
 * In this implementation, {@code getLatest()} is computed as the average of the latest set of values that were added, or the latest value if only one was added.
 * @author Laurent Cohen
 */
public class NonCumulativeSnapshot extends AbstractJPPFSnapshot
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
  public NonCumulativeSnapshot(final String label)
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
      latest = (count == 1L) ? accumulatedValues : accumulatedValues / count;
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
    valueCount = 0L;
    avg = 0d;
    min = (latest < min) ? latest : Double.POSITIVE_INFINITY;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public JPPFSnapshot copy()
  {
    return copy(new NonCumulativeSnapshot(getLabel()));
  }
}
