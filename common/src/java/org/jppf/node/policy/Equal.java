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
 * An execution policy rule that encapsulates a test of type <i>{@code property_value_or_expression equals a}</i>.
 * The test applies to numeric, string or boolean values or expressions.
 * @author Laurent Cohen
 */
public class Equal extends LeftOperandRule {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * A numeric value to compare with.
   */
  private Expression<Double> numberValue;
  /**
   * A string value to compare with.
   */
  private Expression<String> stringValue;
  /**
   * An object value to compare with.
   */
  private Expression<Boolean> booleanValue;
  /**
   * Determines if the comparison should ignore the string case.
   */
  private boolean ignoreCase;

  /**
   * Define an equality comparison between the numeric value of a property and another numeric value.<br>
   * Note that if the value type is equal to {@link ValueType#STRING}, then the comparison will be case-insensitive.
   * To specify case sensitivity, use the {@link #Equal(String, boolean, String)} constructor instead.
   * @param valueType the type of value to which thie equality comparison applies. 
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the value to compare with.
   */
  public Equal(final ValueType valueType, final String propertyNameOrExpression, final String a) {
    super(valueType, propertyNameOrExpression);
    switch(valueType) {
      case BOOLEAN:
        this.booleanValue = new BooleanExpression(a);
        break;

      case NUMERIC:
        this.numberValue = new NumericExpression(a);
        break;

      case STRING:
        this.stringValue = new StringExpression(a);
        break;
    }
  }

  /**
   * Define an equality comparison between the numeric value of a property and another numeric value.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the value to compare with.
   */
  public Equal(final String propertyNameOrExpression, final double a) {
    super(ValueType.NUMERIC, propertyNameOrExpression);
    this.numberValue = new NumericExpression(a);
  }

  /**
   * Define an equality comparison between the string value of a property and another string value.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param ignoreCase determines if the comparison should ignore the string case.
   * @param a the value to compare with.
   */
  public Equal(final String propertyNameOrExpression, final boolean ignoreCase, final String a) {
    super(ValueType.STRING, propertyNameOrExpression);
    this.stringValue = new StringExpression(a);
    this.ignoreCase = ignoreCase;
  }

  /**
   * Define an equality comparison between the boolean value of a property and another boolean value.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param a the value to compare with.
   */
  public Equal(final String propertyNameOrExpression, final boolean a) {
    super(ValueType.BOOLEAN, propertyNameOrExpression);
    this.booleanValue = new BooleanExpression(a);
  }

  /**
   * Determines whether this policy accepts the specified node.
   * @param info system information for the node on which the tasks will run if accepted.
   * @return true if the node is accepted, false otherwise.
   */
  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    Object o = getLeftOperandValue(info);
    if (numberValue != null) return (o == null) ? false : o.equals(numberValue.evaluate(info));
    else if (stringValue != null) return ignoreCase ? stringValue.evaluate(info).equalsIgnoreCase((String) o) : stringValue.evaluate(info).equals(o);
    else if (booleanValue != null) return (o == null) ? false : o.equals(booleanValue.evaluate(info));
    else return o == null;
  }

  @Override
  public String toString(final int n) {
    StringBuilder sb = new StringBuilder(indent(n)).append("<Equal valueType=\"");
    if (stringValue != null) sb.append("string");
    else if (numberValue != null) sb.append("numeric");
    else if (booleanValue != null) sb.append("boolean");
    sb.append("\" ignoreCase=\"").append(ignoreCase).append("\">\n");
    sb.append(indent(n + 1)).append("<Property>").append(leftOperand.getExpression()).append("</Property>\n");
    sb.append(indent(n + 1)).append("<Value>");
    if (stringValue != null) sb.append(stringValue.getExpression());
    else if (numberValue != null) sb.append(numberValue.getExpression());
    else if (booleanValue != null) sb.append(booleanValue.getExpression());
    sb.append("</Value>\n");
    sb.append(indent(n)).append("</Equal>\n");
    return sb.toString();
  }
}
