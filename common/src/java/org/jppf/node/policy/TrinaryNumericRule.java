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
 * Common abstract superclass for execution policy rules that encapsulates a test of type <i>{@code property_value_or_expression between a and b}</i>.
 * The test applies to numeric values only.
 * @author Laurent Cohen
 */
abstract class TrinaryNumericRule extends LeftOperandRule {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The interval's lower bound.
   */
  protected Expression<Double> a;
  /**
   * The interval's upper bound.
   */
  protected Expression<Double> b;
  /**
   * String to use for the bounds. 
   */
  private static final String[] BOUNDS = { "EE", "EI", "IE", "II" };
  /**
   * Index of the string to use for the bounds. 
   */
  private final byte boundsIndex;

  /**
   * Define a comparison of type value between a and b.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param boundsIndex the index of string to use for the bounds.
   */
  TrinaryNumericRule(final String propertyNameOrExpression, final int boundsIndex) {
    super(ValueType.NUMERIC, propertyNameOrExpression);
    this.boundsIndex = (byte) boundsIndex;
  }

  /**
   * Define a comparison of type value between a and b.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the lower bound.
   * @param b the upper bound.
   * @param boundsIndex the index of string to use for the bounds.
   */
  TrinaryNumericRule(final String propertyNameOrExpression, final double a, final double b, final int boundsIndex) {
    this(propertyNameOrExpression, boundsIndex);
    this.a = new NumericExpression(a);
    this.b = new NumericExpression(b);
  }

  /**
   * Define a comparison of type value between a and b.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the lower bound expression.
   * @param b the upper bound.
   * @param boundsIndex the index of string to use for the bounds.
   */
  TrinaryNumericRule(final String propertyNameOrExpression, final String a, final double b, final int boundsIndex) {
    this(propertyNameOrExpression, boundsIndex);
    this.a = new NumericExpression(a);
    this.b = new NumericExpression(b);
  }

  /**
   * Define a comparison of type value between a and b.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the lower bound.
   * @param b the upper bound expression.
   * @param boundsIndex the index of string to use for the bounds.
   */
  TrinaryNumericRule(final String propertyNameOrExpression, final double a, final String b, final int boundsIndex) {
    this(propertyNameOrExpression, boundsIndex);
    this.a = new NumericExpression(a);
    this.b = new NumericExpression(b);
  }

  /**
   * Define a comparison of type value between a and b.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the lower bound expression.
   * @param b the upper bound expression.
   * @param boundsIndex the index of string to use for the bounds.
   */
  TrinaryNumericRule(final String propertyNameOrExpression, final String a, final String b, final int boundsIndex) {
    this(propertyNameOrExpression, boundsIndex);
    this.a = new NumericExpression(a);
    this.b = new NumericExpression(b);
  }

  /**
   * Determines whether this policy accepts the specified node.
   * @param info system information for the node on which the tasks will run if accepted.
   * @return true if the node is accepted, false otherwise.
   */
  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    final Object o = getLeftOperandValue(info);
    if (o != null) {
      final double value = (Double) o;
      final double a = this.a.evaluate(info);
      final double b = this.b.evaluate(info);
      return accepts(value, a, b);
    }
    return false;
  }

  /**
   * Determines whether this policy accepts the specified value.
   * @param value a value to compare to the lower and upper bounds.
   * @param a the lower bound of the between comparison.
   * @param b the upper bound of the between comparison.
   * @return {@code true} if the value is accepted, {@code false} otherwise.
   */
  abstract boolean accepts(final double value, final double a, final double b);

  @Override
  public String toString(final int n) {
    final StringBuilder sb = new StringBuilder();
    final String name = new StringBuilder("Between").append(BOUNDS[boundsIndex]).toString();
    sb.append(indent(n)).append('<').append(name).append(">\n");
    sb.append(indent(n + 1)).append("<Property>").append(leftOperand.getExpression()).append("</Property>\n");
    sb.append(indent(n + 1)).append("<Value>").append(a.getExpression()).append("</Value>\n");
    sb.append(indent(n + 1)).append("<Value>").append(b.getExpression()).append("</Value>\n");
    sb.append(indent(n)).append("</").append(name).append(">\n");
    return sb.toString();
  }
}
