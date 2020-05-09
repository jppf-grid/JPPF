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

package org.jppf.node.policy;

import org.jppf.utils.PropertiesCollection;

/**
 * An execution policy rule that encapsulates a test of type <i>{@code property_value_or_expression not_equal_to a}</i>.
 * The test applies to numeric, string or boolean values or expressions.
 * @author Laurent Cohen
 */
public class NotEqual extends Equal {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Define an inequality comparison between the numeric value of a property and another numeric value.<br>
   * Note that if the value type is equal to {@link ValueType#STRING}, then the comparison will be case-insensitive.
   * To specify case sensitivity, use the {@link #Equal(String, boolean, String)} constructor instead.
   * @param valueType the type of value to which thie equality comparison applies. 
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the value to compare with.
   */
  public NotEqual(final ValueType valueType, final String propertyNameOrExpression, final String a) {
    super(valueType, propertyNameOrExpression, a);
  }

  /**
   * Define an inequality comparison between the numeric value of a property and another numeric value.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the value to compare with.
   */
  public NotEqual(final String propertyNameOrExpression, final double a) {
    super(propertyNameOrExpression, a);
  }

  /**
   * Define an inequality comparison between the string value of a property and another string value.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a string value.
   * @param ignoreCase determines if the comparison should ignore the string case.
   * @param a the value to compare with.
   */
  public NotEqual(final String propertyNameOrExpression, final boolean ignoreCase, final String a) {
    super(propertyNameOrExpression, ignoreCase, a);
  }

  /**
   * Define an inequality comparison between the boolean value of a property and another boolean value.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a boolean value.
   * @param a the value to compare with.
   */
  public NotEqual(final String propertyNameOrExpression, final boolean a) {
    super(propertyNameOrExpression, a);
  }

  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    return !super.accepts(info);
  }
}
