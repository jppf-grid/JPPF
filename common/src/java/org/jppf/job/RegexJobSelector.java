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

import java.util.regex.*;

import org.jppf.node.protocol.JPPFDistributedJob;

/**
 * Performs a regular expression match on either a job name or a job metadata value specified by its key.
 * If the specified metadata key is {@code null}, is not part of a job's metadata, or is not a string, then this selector's {@code accepts()} method will return {@code false}. 
 * @author Laurent Cohen
 * @since 6.2
 */
public class RegexJobSelector extends AbstractBinaryComparisonJobSelector<String> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The regex flags, as defined in {@link Pattern}.
   */
  private final int flags;
  /**
   * The compiled pattern.
   */
  private transient Pattern pattern;

  /**
   * Initialize this selector with a regular expression to match against job names.
   * <br>This constructor is equivalent to calling {@link #RegexJobSelector(Object, String, int) new RegexJobSelector(null, regex, 0)}.
   * @param regex the regular expression to match against.
   */
  public RegexJobSelector(final String regex) {
    this(null, regex, 0);
  }

  /**
   * Initialize this selector with a regular expression to match against job names.
   * <br>This constructor is equivalent to calling {@link #RegexJobSelector(Object, String, int) new RegexJobSelector(null, regex, flags)}.
   * @param regex the regular expression to match against.
   * @param flags the regex flags, as defined in {@link Pattern}.
   */
  public RegexJobSelector(final String regex, final int flags) {
    this(null, regex, flags);
  }

  /**
   * Initialize this selector with a job metadata key and a regular expression.
   * <br>This constructor is equivalent to calling {@link #RegexJobSelector(Object, String, int) new RegexJobSelector(key, regex, 0)}.
   * @param key the metadata key of the value to compare with.
   * @param regex the regular expression to match against.
   */
  public RegexJobSelector(final Object key, final String regex) {
    this(key, regex, 0);
  }

  /**
   * Initialize this selector with a job metadata key and a regular expression.
   * <br>This constructor will compile the regex into a {@link Pattern} instance, and throw one of the declared exceptions if that fails.
   * @param key the metadata key of the value to compare with.
   * @param regex the regular expression to match against.
   * @param flags the regex flags, as defined in {@link Pattern}.
   * @throws IllegalArgumentException if the flags contain undefined bits.
   * @throws PatternSyntaxException if the regex syntax is invalid.
   */
  public RegexJobSelector(final Object key, final String regex, final int flags) throws IllegalArgumentException, PatternSyntaxException {
    super(key, regex);
    this.flags = flags;
    Pattern.compile(regex, flags);
  }

  @Override
  public boolean accepts(final JPPFDistributedJob job) {
    if (pattern == null) pattern = Pattern.compile(operand, flags);
    final Object value = getValueOrName(job);
    if (value instanceof String) return pattern.matcher((String) value).matches();
    return false;
  }
}
