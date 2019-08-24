/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.job;

import org.jppf.node.protocol.JPPFDistributedJob;

/**
 * Performs a comparison of type "strictly more than lower_bound and strictly less than upper_bound" between  a {@code Comparable} metadata value or a job name
 * and {@code Comparable} lower and upper bounds.
 * @author Laurent Cohen
 * @since 6.2
 */
public class BetweenEEJobSelector extends AbstractTernaryComparisonJobSelector<Comparable<?>> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this job selector with a metadata key and lower and upper bounds.
   * @param lower the lower bound to compare job names with.
   * @param upper the upper bound to compare job names with.
   */
  public BetweenEEJobSelector(final String lower, final String upper) {
    this(null, lower, upper);
  }

  /**
   * Initialize this job selector with a metadata key and lower and upper bounds.
   * @param key the metadata key of the value to compare with.
   * @param lower the lower bound to compare with.
   * @param upper the upper bound to compare with.
   */
  public BetweenEEJobSelector(final Object key, final Comparable<?> lower, final Comparable<?> upper) {
    super(key, lower, upper);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public boolean accepts(final JPPFDistributedJob job) {
    final Comparable value = getComparableValueOrName(job);
    if (value == null) return false;
    return (value.compareTo(operand) > 0) && (value.compareTo(operand2) < 0);
  }
}
