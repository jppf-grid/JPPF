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
 * @param <E> the type of operand to compare with.
 * @author Laurent Cohen
 */
abstract class AbstractBinaryComparisonJobSelector<E> implements JobSelector {
  /**
   * The key of the metadata value to compare with.
   */
  final Object key;
  /**
   * The value to compare the metadata value with.
   */
  final E operand;

  /**
   * 
   * @param key the metadata key of the value to compare with.
   * @param operand the operand to compare with.
   */
  public AbstractBinaryComparisonJobSelector(final Object key, final E operand) {
    this.key = key;
    this.operand = operand;
  }

  /**
   * 
   * @param job the job to lookup against.
   * @return the value specified by this selector, i.e. either the job name or a metadata value.
   */
  Object getValueOrName(final JPPFDistributedJob job) {
    return (key == null) ? job.getName() : job.getMetadata().getParameter(key);
  }

  /**
   * 
   * @param job the job to lookup against.
   * @return the value specified by this selector, i.e. either the job name or a metadata value.
   */
  Comparable<?> getComparableValueOrName(final JPPFDistributedJob job) {
    final Object value = getValueOrName(job);
    return (value instanceof Comparable) ? (Comparable<?>) value : null;
  }
}
