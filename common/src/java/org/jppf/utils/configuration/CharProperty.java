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

package org.jppf.utils.configuration;

/**
 * Implementation of {@link JPPFProperty} for {@code char} properties.
 * @author Laurent Cohen
 * @since 5.2
 */
public class CharProperty extends AbstractJPPFProperty<Character> {
  /**
   * Initialize this property with the specified name and default value.
   * @param name the name of this property.
   * @param defaultValue the default value of this property, used when the property is not defined.
   * @param aliases other names that may be given to this property (e.g. older names from previous versions).
   */
  public CharProperty(final String name, final Character defaultValue, final String...aliases) {
    super(name, defaultValue, aliases);

  }

  @Override
  public Character valueOf(final String value) {
    return (value == null) ? null : value.charAt(0);
  }

  @Override
  public Class<Character> valueType() {
    return char.class;
  }
}
