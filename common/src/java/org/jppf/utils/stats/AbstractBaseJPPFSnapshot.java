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
public abstract class AbstractBaseJPPFSnapshot implements JPPFSnapshot {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Label for this snapshot, used in the {@link #toString()} method.
   */
  final String label;
  /**
   * The total cumulated values.
   */
  double total;
  /**
   * The creation time of this snapshot, as the result of calling {@code System.nanoTime()}.
   */
  final transient long creationNanos;
  /**
   * The last update time of this snapshot, as the result of calling {@code System.nanoTime() - creationTimeNanos}.
   */
  long updateNanos;

  /**
   * Initialize this time snapshot with a specified title.
   * @param label the title for this snapshot.
   */
  AbstractBaseJPPFSnapshot(final String label) {
    this.label = label;
    this.creationNanos = System.nanoTime();
  }

  @Override
  public synchronized double getTotal() {
    return total;
  }

  @Override
  public synchronized String getLabel() {
    return label;
  }

  @Override
  public synchronized long getLastUpdateNanos() {
    return updateNanos;
  }

  /**
   * Update the value of {@link #updateNanos}.
   */
  void computeUpdateNanos() {
    updateNanos = System.nanoTime() - creationNanos;
  }
}
