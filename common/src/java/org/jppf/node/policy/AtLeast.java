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
 * An execution policy rule that encapsulates a test of type <i>{@code property_value_or_expression >= a}</i>.
 * The test applies to numeric values only.
 * @author Laurent Cohen
 */
public class AtLeast extends BinaryNumericRule {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Define a comparison between the numeric value of a property and another numeric value.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the value to compare with.
   */
  public AtLeast(final String propertyNameOrExpression, final double a) {
    super(propertyNameOrExpression, a);
  }

  /**
   * Define a comparison between the numeric value of a property and another numeric value.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a an expression, possibly a literal, which resolves to a numeric value.
   */
  public AtLeast(final String propertyNameOrExpression, final String a) {
    super(propertyNameOrExpression, a);
  }

  @Override
  boolean accepts(final double a, final double b) {
    return a >= b;
  }

  @Override
  String getTag() {
    return "AtLeast";
  }
}
