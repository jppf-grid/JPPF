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

import java.io.Serializable;

/**
 * Interface for predefined JPPF properties expected to handle a specific balue type.
 * @param <T> the type of the value of this property.
 * @author Laurent Cohen
 * @since 5.2
 */
public interface JPPFProperty<T> extends Serializable {
  /**
   * Get the name of this property.
   * @return the property's name.
   */
  String getName();

  /**
   * Get the default value of this property.
   * @return the default value;
   */
  T getDefaultValue();

  /**
   * Get the aliases for this property, that is, other names it may be known as such as legacy names from prior versions.
   * @return an array of the aliases for this property, possibly empty.
   */
  String[] getAliases();

  /**
   * Convert the specified value into the type of values handled by this property.
   * @param value the property value to convert.
   * @return the value converted to the type of this property.
   */
  T valueOf(String value);

  /**
   * Convert the specified value to a string.
   * @param value the property value to convert.
   * @return a String representation of the value.
   */
  String toString(final T value);

  /**
   * Return the class object for the type of values of this property.
   * @return a {@link Class} instance.
   */
  Class<T> valueType();

  /**
   * Get a description of this property.
   * @return the description as a string.
   */
  String getDocumentation();
}
