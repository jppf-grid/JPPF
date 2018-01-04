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

import java.util.*;

import org.jppf.utils.PropertiesCollection;

/**
 * An execution policy rule that encapsulates a test of type <i>property_value_or_expression is one of [value1, &middot;&middot;&middot; , valueN]</i>.
 * The test applies to numeric and string values.
 * @author Laurent Cohen
 */
public class OneOf extends LeftOperandRule {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * A numeric value to compare with.
   */
  private List<Expression<Double>> numbers;
  /**
   * A string value to compare with.
   */
  private List<Expression<String>> strings;
  /**
   * Determines if the comparison should ignore the string case.
   */
  private boolean ignoreCase;

  /**
   * Determine whether the value of a property, expressed as a {@code double}, is in the specified array of values.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param values the values to compare with.
   */
  public OneOf(final String propertyNameOrExpression, final double... values) {
    super(ValueType.NUMERIC, propertyNameOrExpression);
    numbers = new ArrayList<>(values.length);
    for (final double value: values) numbers.add(new NumericExpression(value));
  }

  /**
   * Determine whether the value of a property, expressed as a {@code double}, is in the specified array of values.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param values expressions resolving to double values to compare with.
   */
  public OneOf(final String propertyNameOrExpression, final String... values) {
    super(ValueType.NUMERIC, propertyNameOrExpression);
    numbers = new ArrayList<>(values.length);
    for (final String value: values) numbers.add(new NumericExpression(value));
  }

  /**
   * Determine whether the value of a property, expressed as a {@code String}, is in the specified array of values.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param ignoreCase determines if the comparison should ignore the string case.
   * @param values the values or expressions resolving to strings to compare with.
   */
  public OneOf(final String propertyNameOrExpression, final boolean ignoreCase, final String... values) {
    super(ValueType.STRING, propertyNameOrExpression);
    strings = new ArrayList<>(values.length);
    for (final String value: values) strings.add(new StringExpression(value));
    this.ignoreCase = ignoreCase;
  }

  /**
   * Determines whether this policy accepts the specified node.
   * @param info system information for the node on which the tasks will run if accepted.
   * @return true if the node is accepted, false otherwise.
   */
  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    final Object o = getLeftOperandValue(info);
    if (numbers != null) {
      if (o != null) {
        for (final Expression<Double> expr: numbers) {
          if (o.equals(expr.evaluate(info))) return true;
        }
      }
    } else if (strings != null) {
      for (final Expression<String> expr : strings) {
        final String value = expr.evaluate(info);
        if ((value == null) && (o == null)) return true;
        else if ((value != null) && (o != null)) {
          if (!ignoreCase && o.equals(value) || ignoreCase && ((String) o).equalsIgnoreCase(value)) return true;
        }
      }
    }
    return false;
  }

  /**
   * Print this object to a string.
   * @return an XML string representation of this object
   */
  @Override
  public String toString(final int n) {
    final StringBuilder sb = new StringBuilder(indent(n)).append("<OneOf valueType=\"");
    if (strings != null) sb.append("string");
    else if (numbers != null) sb.append("numeric");
    sb.append("\" ignoreCase=\"").append(ignoreCase).append("\">\n");
    sb.append(indent(n + 1)).append(xmlElement("Property", leftOperand.getExpression())).append('\n');
    final List<? extends Expression<?>> list = (strings != null) ? strings : numbers;
    for (final Expression<?> expr: list) sb.append(indent(n + 1)).append(xmlElement("Value", expr.getExpression())).append('\n');
    sb.append(indent(n)).append("</OneOf>\n");
    return sb.toString();
  }
}
