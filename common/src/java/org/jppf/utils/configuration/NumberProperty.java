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

/**
 * Implementation of {@link JPPFProperty} for numeric properties.
 * Instances of this class or its subclasses manage a minimum and maximum accepted value for numeric properties. 
 * @param <T> the type of the value of this property.
 * @author Laurent Cohen
 * @since 5.2
 */
abstract class NumberProperty<T extends Number> extends AbstractJPPFProperty<T> {
  /**
   * The minimum accepted value for this property.
   */
  final Comparable<T> minValue;
  /**
   * The maximum accepted value for this property.
   */
  final Comparable<T> maxValue;

  /**
   * Initialize this property with the specified name and default value.
   * @param name the name of this property.
   * @param defaultValue the default value of this property, used when the proeprty is not defined.
   * @param aliases other names that may be given to this property (e.g. older names from previous versions).
   */
  public NumberProperty(final String name, final T defaultValue, final String... aliases) {
    super(name, defaultValue, aliases);
    this.minValue = null;
    this.maxValue = null;
  }

  /**
   * Initialize this property with the specified name and default value.
   * @param name the name of this property.
   * @param defaultValue the default value of this property, used when the proeprty is not defined.
   * @param aliases other names that may be given to this property (e.g. older names from previous versions).
   * @param minValue the minimum accepted value for this property.
   * @param maxValue the maximum accepted value for this property.
   */
  @SuppressWarnings("unchecked")
  public NumberProperty(final String name, final T defaultValue, final T minValue, final T maxValue, final String... aliases) {
    super(name, defaultValue, aliases);
    if ((minValue == null) || (maxValue == null)) throw new IllegalArgumentException(String.format("min and max values cannot be nuul (name=%s; min=%s; max=%s", name, minValue, minValue));
    if (minValue.doubleValue() > maxValue.doubleValue()) throw new IllegalArgumentException(String.format("min value must be <= max value (name=%s; min=%s; max=%s)", name, minValue, minValue));
    this.minValue = (Comparable<T>) minValue;
    this.maxValue = (Comparable<T>) maxValue;
  }

  /**
   * Get the minimum accepted value for this property.
   * @return the minimum value.
   */
  @SuppressWarnings("unchecked")
  public T getMinValue() {
    return (T) minValue;
  }

  /**
   * Get the maximum accepted value for this property.
   * @return the maximum value.
   */
  @SuppressWarnings("unchecked")
  public T getMaxValue() {
    return (T) maxValue;
  }

  /**
   * Whether this property has a min and max values.
   * @return {@code true} if min and max values are defined, {@code false} otherwise.
   */
  private boolean hasMinAndMax() {
    return (minValue != null) && (maxValue != null);
  }

  /**
   * Validate a value with regards to min and max and return the default value if it doesn't fit in the range.
   * @param value the value to validate.
   * @return the default value if the value is < minValue or > maxValue, or the value itself otherwise.
   */
  @SuppressWarnings("unchecked")
  T validate(final T value) {
    Comparable<T> cv = (Comparable<T>) value;
    if ((value != null) && hasMinAndMax() && ((cv.compareTo((T) minValue) < 0) || (cv.compareTo((T) maxValue) > 0))) return getDefaultValue();
    return value;
  }
}
