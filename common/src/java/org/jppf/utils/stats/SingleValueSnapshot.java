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
 * Instances of this class simply accumulate the values provided via calls to {@link #addValues(double, long)}.
 * The min, max, avg and latest values are never computed and thus have a constant value of 0.
 * <p>This is intended for monitoring snapshots that only handle one value and to avoid excessive overhead during
 * serialization and deserialization.
 * @author Laurent Cohen
 */
public class SingleValueSnapshot extends AbstractBaseJPPFSnapshot {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this time snapshot with a specified title.
   * @param label the title for this snapshot.
   * @exclude
   */
  public SingleValueSnapshot(final String label) {
    super(label);
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public synchronized void addValues(final double accumulatedValues, final long count) {
    computeUpdateNanos();
    total += accumulatedValues;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public synchronized JPPFSnapshot copy() {
    final SingleValueSnapshot svs = new SingleValueSnapshot(label);
    svs.total = total;
    return svs;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public synchronized void reset() {
    computeUpdateNanos();
    total = 0d;
  }

  @Override
  public long getValueCount() {
    return 0;
  }

  @Override
  public double getLatest() {
    return 0;
  }

  @Override
  public double getMin() {
    return 0;
  }

  @Override
  public double getMax() {
    return 0;
  }

  @Override
  public double getAvg() {
    return 0;
  }

  @Override
  public synchronized String toString() {
    return new StringBuilder(JPPFStatisticsHelper.getLocalizedLabel(this)).append(": type=").append(getClass().getSimpleName()).append(", total=").append(total).toString();
  }
}
