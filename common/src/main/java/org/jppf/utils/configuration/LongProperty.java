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

package org.jppf.utils.configuration;

/**
 * Implementation of {@link JPPFProperty} for {@code long} properties.
 * @author Laurent Cohen
 * @since 5.2
 */
public class LongProperty extends NumberProperty<Long> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this property with the specified name and default value.
   * @param name the name of this property.
   * @param defaultValue the default value of this property, used when the property is not defined.
   * @param aliases other names that may be given to this property (e.g. older names from previous versions).
   */
  public LongProperty(final String name, final Long defaultValue, final String...aliases) {
    super(name, defaultValue, aliases);
  }

  /**
   * Initialize this property with the specified name and default value.
   * @param name the name of this property.
   * @param defaultValue the default value of this property, used when the proeprty is not defined.
   * @param aliases other names that may be given to this property (e.g. older names from previous versions).
   * @param minValue the minimum accepted value for this property.
   * @param maxValue the maximum accepted value for this property.
   */
  public LongProperty(final String name, final Long defaultValue, final Long minValue, final Long maxValue, final String... aliases) {
    super(name, defaultValue, minValue, maxValue, aliases);
  }

  @Override
  public Long valueOf(final String value) {
    return validate(Double.valueOf(sanitizeValue(value)).longValue());
  }

  @Override
  public Class<Long> valueType() {
    return long.class;
  }
}
