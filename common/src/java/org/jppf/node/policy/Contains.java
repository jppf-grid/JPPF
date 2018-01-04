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
 * An execution policy rule that encapsulates a test of type <i>{@code property_value_or_expression contains a}</i>.
 * The test applies to string values only.
 * @author Laurent Cohen
 */
public class Contains extends LeftOperandRule {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * A string value to compare with.
   */
  private Expression<String> value = null;
  /**
   * Determines if the comparison should ignore the string case.
   */
  private boolean ignoreCase = false;

  /**
   * Define an contains test between the string value of a property and another string value.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param ignoreCase determines if the comparison should ignore the string case.
   * @param a the value to compare with.
   */
  public Contains(final String propertyNameOrExpression, final boolean ignoreCase, final String a) {
    super(ValueType.STRING, propertyNameOrExpression);
    this.value = new StringExpression(a);
    this.ignoreCase = ignoreCase;
  }

  /**
   * Determines whether this policy accepts the specified node.
   * @param info system information for the node on which the tasks will run if accepted.
   * @return true if the node is accepted, false otherwise.
   */
  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    final String value = this.value.evaluate(info);
    if (value == null) return false;
    final String s = (String) getLeftOperandValue(info);
    if (s == null) return false;
    if (ignoreCase) return s.toLowerCase().contains(value.toLowerCase());
    return s.contains(value);
  }

  @Override
  public String toString(final int n) {
    return new StringBuilder(indent(n)).append("<Contains ignoreCase=\"").append(ignoreCase).append("\">\n")
      .append(indent(n + 1)).append("<Property>").append(leftOperand.getExpression()).append("</Property>\n")
      .append(indent(n + 1)).append("<Value>").append(value.getExpression()).append("</Value>\n")
      .append(indent(n)).append("</Contains>\n").toString();
  }
}
