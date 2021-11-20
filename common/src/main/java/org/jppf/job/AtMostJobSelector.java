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
 * Performs a "at most" comparison between a job metadata value or a job name and an other comparable value.
 * @author Laurent Cohen
 * @since 6.2
 */
public class AtMostJobSelector extends AbstractBinaryComparisonJobSelector<Comparable<?>> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this selector with a value to compare job names with.
   * @param operand the operand to compare with job names.
   */
  public AtMostJobSelector(final String operand) {
    this(null, operand);
  }

  /**
   * Initialize this selector with a job metadata key and a value to compare with.
   * @param key the metadata key of the value to compare with.
   * @param operand the operand to compare with.
   */
  public AtMostJobSelector(final Object key, final Comparable<?> operand) {
    super(key, operand);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public boolean accepts(final JPPFDistributedJob job) {
    final Comparable value = getComparableValueOrName(job);
    if (value == null) return false;
    return value.compareTo(operand) <= 0;
  }
}
