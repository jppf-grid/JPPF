/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.utils.configuration;

import org.jppf.utils.StringUtils;

/**
 * Implementation of {@link JPPFProperty} for properties whose value is an array of space-separated {@code int}s.
 * @author Laurent Cohen
 * @since 6.0
 * @exclude
 */
public class IntArrayProperty extends AbstractJPPFProperty<int[]> {
  /**
   * Initialize this property with the specified name and default value.
   * @param name the name of this property.
   * @param defaultValue the default value of this property, used when the property is not defined.
   * @param aliases other names that may be given to this property (e.g. older names from previous versions).
   */
  public IntArrayProperty(final String name, final int[] defaultValue, final String...aliases) {
    super(name, defaultValue, aliases);
  }

  @Override
  public int[] valueOf(final String value) {
    return StringUtils.parseIntValues(value);
  }

  @Override
  public String toString(final int[] value) {
    if (value == null) return null;
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<value.length; i++) {
      if (i > 0) sb.append(" ");
      sb.append(value[i]);
    }
    return sb.toString();
  }

  @Override
  public Class<int[]> valueType() {
    return int[].class;
  }
}
