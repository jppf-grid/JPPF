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

import java.util.Arrays;

import org.jppf.utils.LocalizationUtils;

/**
 * Abstract implementation of the {@link JPPFProperty} interface.
 * @param <T> the type of the value of this property.
 * @author Laurent Cohen
 * @since 5.2
 */
abstract class AbstractJPPFProperty<T> implements JPPFProperty<T> {
  /**
   * Location of the localization resource bundles.
   */
  private  static final String I18N_BASE = "org.jppf.utils.configuration.i18n.JPPFProperties";
  /**
   * The name of this property.
   */
  private final String name;
  /**
   * Other names that may be given to this property (e.g. older names from previous versions).
   */
  private final String[] aliases;
  /**
   * The default value of this property.
   */
  private final T defaultValue;
  /**
   * The possible values for this property, if any.
   */
  private T[] possibleValues;

  /**
   * Initialize this property with the specified name and default value.
   * @param name the name of this property.
   * @param defaultValue the default value of this property, used when the proeprty is not defined.
   * @param aliases other names that may be given to this property (e.g. older names from previous versions).
   */
  public AbstractJPPFProperty(final String name, final T defaultValue, final String...aliases) {
    this.name = name;
    this.defaultValue = defaultValue;
    this.aliases = aliases;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public T getDefaultValue() {
    return defaultValue;
  }

  @Override
  public String[] getAliases() {
    return aliases;
  }

  @Override
  public String toString(final T value) {
    return (value == null) ? null : value.toString();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("name=").append(name);
    sb.append(", default=").append(defaultValue);
    sb.append(", aliases=").append(Arrays.asList(aliases));
    sb.append(", description=").append(getDocumentation());
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the possible values for this property, if any is defined.
   * @return an array of the possible values.
   */
  public T[] getPossibleValues() {
    return possibleValues;
  }

  /**
   * Set the possible values for this property.
   * @param possibleValues an array of the possible values.
   * @return this property.
   */
  public JPPFProperty<T> setPossibleValues(final T... possibleValues) {
    this.possibleValues = possibleValues;
    return this;
  }

  @Override
  public String getDocumentation() {
    return LocalizationUtils.getLocalized(I18N_BASE, name + ".doc");
  }
}
