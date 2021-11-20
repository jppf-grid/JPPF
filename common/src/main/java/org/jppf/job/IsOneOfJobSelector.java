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

import java.util.*;

import org.jppf.node.protocol.JPPFDistributedJob;

/**
 * Performs an "is one of" comparison between a job metadata value or a job name and a set of other comparable values.
 * @author Laurent Cohen
 * @since 6.2
 */
public class IsOneOfJobSelector extends AbstractBinaryComparisonJobSelector<Set<?>> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this selector with an array of values to compare job names with.
   * <br>This constructor is equivalent to calling {@link #IsOneOfJobSelector(Object, Collection) new IsOneOfJobSelector(null, Arrays.asList(values))}.
   * @param values the alues to compare with.
   */
  public IsOneOfJobSelector(final String...values) {
    this(null, Arrays.asList(values));
  }

  /**
   * Initialize this selector with a collection of values to compare job names with.
   * <br>This constructor is equivalent to calling {@link #IsOneOfJobSelector(Object, Collection) new IsOneOfJobSelector(null, values)}.
   * @param values the alues to compare with.
   */
  public IsOneOfJobSelector(final Collection<String> values) {
    this(null, values);
  }

  /**
   * Initialize this selector with a job metadata key and an array of values to compare with.
   * <br>This constructor is equivalent to calling {@link #IsOneOfJobSelector(Object, Collection) new IsOneOfJobSelector(key, values)}.
   * @param key the metadata key of the value to compare with.
   * @param values the values to compare with.
   */
  public IsOneOfJobSelector(final Object key, final Object...values) {
    this(key, Arrays.asList(values));
  }

  /**
   * Initialize this selector with a job metadata key and a collection of values to compare with.
   * @param key the metadata key of the value to compare with.
   * @param values the values to compare with.
   */
  public IsOneOfJobSelector(final Object key, final Collection<?> values) {
    super(key, new HashSet<>(values));
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public boolean accepts(final JPPFDistributedJob job) {
    final Object value = getValueOrName(job);
    for (final Object o: operand) {
      if ((value == null) && (o == null)) return true;
      if ((value != null) && value.equals(o)) return true;
    }
    return false;
  }
}
