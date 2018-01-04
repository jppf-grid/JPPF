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

import org.jppf.utils.PropertiesCollection;

/**
 * An execution policy rule that encapsulates a test of type <i>{@literal property_value >= value}</i>.
 * The test applies to numeric values only.
 * @author Laurent Cohen
 */
abstract class BinaryNumericRule extends LeftOperandRule {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * A numeric value to compare with.
   */
  Expression<Double> expression;

  /**
   * Define a comparison between the numeric value of a property and another numeric value.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the value to compare with.
   */
  BinaryNumericRule(final String propertyNameOrExpression, final double a) {
    super(ValueType.NUMERIC, propertyNameOrExpression);
    this.expression = new NumericExpression(a);
  }

  /**
   * Define a comparison between the numeric value of a property and another numeric value.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the value to compare with.
   */
  BinaryNumericRule(final String propertyNameOrExpression, final String a) {
    super(ValueType.NUMERIC, propertyNameOrExpression);
    this.expression = new NumericExpression(a);
  }

  /**
   * Determines whether this policy accepts the specified node.
   * @param info system information for the node on which the tasks will run if accepted.
   * @return true if the node is accepted, false otherwise.
   */
  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    final double value = (Double) getLeftOperandValue(info);
    return accepts(value, expression.evaluate(info));
  }

  /**
   * Determine whether the comparison between the two operands of the test is true.
   * @param a the first operand.
   * @param b the second operand.
   * @return {@code true} if the comparison is true, {@code false} otherwise.
   */
  abstract boolean accepts(final double a, final double b);

  /**
   * @return the XML tag name for this policy.
   */
  abstract String getTag();

  @Override
  public String toString(final int n) {
    return new StringBuilder(indent(n)).append('<').append(getTag()).append(">\n")
      .append(indent(n + 1)).append("<Property>").append(leftOperand.getExpression()).append("</Property>\n")
      .append(indent(n + 1)).append("<Value>").append(expression.getExpression()).append("</Value>\n")
      .append(indent(n)).append("</").append(getTag()).append(">\n").toString();
  }
}
