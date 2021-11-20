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
 * Performs an "contains" comparison between a job metadata value or a job name and an other comparable value.
 * @author Laurent Cohen
 * @since 6.2
 */
public class ContainsJobSelector extends AbstractBinaryComparisonJobSelector<String> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this selector with a value to compare job names with.
   * <br>This constructor is equivalent to calling {@link #ContainsJobSelector(Object, String) new ContainsJobSelector(null, substring)}.
   * @param substring the substring to look for.
   */
  public ContainsJobSelector(final String substring) {
    this(null, substring);
  }

  /**
   * Initialize this selector with a job metadata key and a value to compare with.
   * @param key the metadata key of the value to compare with.
   * @param substring the substring to look for.
   */
  public ContainsJobSelector(final Object key, final String substring) {
    super(key, substring);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public boolean accepts(final JPPFDistributedJob job) {
    final Object value = getValueOrName(job);
    if (value instanceof String) return ((String) value).contains(operand);
    return false;
  }
}
