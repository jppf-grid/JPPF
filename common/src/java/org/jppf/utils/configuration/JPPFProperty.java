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

import java.io.Serializable;
import java.util.*;

/**
 * Interface for predefined JPPF properties expected to handle a specific value type.
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
   * Get a short label for this property.
   * @return the label as a string.
   */
  String getShortLabel();

  /**
   * Get a short label for this property.
   * @param locale the locale in which the short label of this property should be returned.
   * @return the label as a string.
   */
  String getShortLabel(Locale locale);

  /**
   * Get a description of this property.
   * @param locale the locale in which the documentation of this property should be returned.
   * @return the description as a string.
   */
  String getDocumentation(Locale locale);

  /**
   * Get a description of this property.
   * @return the description as a string.
   */
  String getDocumentation();

  /**
   * Get the set of tags that apply to this property.
   * @return a set of tags as strings.
   */
  Set<String> getTags();

  /**
   * Get the parmaeters specified in the name of property, if any.
   * Parameters are specified as in this example: {@code my.<param1>.property.<param2>.name = some value}.
   * @return an array of the parameter names, empty if there is no parameter.
   * @since 6.0
   */
  String[] getParameters();

  /**
   * Get the documeentation for the specified parmaeter, if it exists.
   * @param param the name of the parameter for which to find a description.
   * @return a string describing the parameter.
   * @since 6.0
   */
  String getParameterDoc(String param);

  /**
   * Get a string explaining why a property was deprecated and/or what to use instead.
   * @return a text documentation about this property's deprecation.
   */
  String getDeprecatedDoc();

  /**
   * Get a string explaining why a property was deprecated and/or what to use instead.
   * @param locale the locale in which the text should be returned.
   * @return a text documentation about this property's deprecation.
   */
  public String getDeprecatedDoc(Locale locale);

  /**
   * Resolve the name of the property by substituting the parameters names with actual values.
   * @param params the values of the parameters.
   * @return the new resolve name.
   * @since 6.0
   * @exclude
   */
  public String resolveName(String...params);

  /**
   * Resolve the specified alias for the property's name by substituting the parameters names with actual values.
   * @param alias the alias to resolve.
   * @param params the values of the parameters.
   * @return the new resolve name.
   * @since 6.0
   * @exclude
   */
  public String resolveName(String alias, String...params);

  /**
   * Determine whether this property is deprecated.
   * @return {@code true} if the property is deprecated, {@code false} otherwise.
   */
  boolean isDeprecated();

  /**
   * Specify whether this property is deprecated.
   * @param deprecated {@code true} if the property is deprecated, {@code false} otherwise.
   * @return this property, for method call chaining.
   * @exclude
   */
  JPPFProperty<T> setDeprecated(boolean deprecated);
}
