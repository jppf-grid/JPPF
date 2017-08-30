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

package org.jppf.node.policy;


/**
 * An execution policy rule that encapsulates a test of type <i>{@code a <= property_value_or_expression < b}</i>.
 * The test applies to numeric values only.
 * @author Laurent Cohen
 */
public class BetweenIE extends TrinaryNumericRule {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Define a comparison of type value between a and b with a included and b excluded.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the lower bound.
   * @param b the upper bound.
   */
  public BetweenIE(final String propertyNameOrExpression, final double a, final double b) {
    super(propertyNameOrExpression, a, b, 2);
  }

  /**
   * Define a comparison of type value between a and b with a included and b excluded.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the lower bound expression.
   * @param b the upper bound.
   */
  public BetweenIE(final String propertyNameOrExpression, final String a, final double b) {
    super(propertyNameOrExpression, a, b, 2);
  }

  /**
   * Define a comparison of type value between a and b with a included and b excluded.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the lower bound.
   * @param b the upper bound expression.
   */
  public BetweenIE(final String propertyNameOrExpression, final double a, final String b) {
    super(propertyNameOrExpression, a, b, 2);
  }

  /**
   * Define a comparison of type value between a and b with a included and b excluded.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the lower bound expression.
   * @param b the upper bound expression.
   */
  public BetweenIE(final String propertyNameOrExpression, final String a, final String b) {
    super(propertyNameOrExpression, a, b, 2);
  }

  @Override
  boolean accepts(final double value, final double a, final double  b) {
    return (value >= a) && (value < b);
  }
}
