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

import java.io.*;
import java.util.regex.*;

import org.jppf.utils.PropertiesCollection;

/**
 * An execution policy rule that encapsulates a test of type <i>property_value matches regular_expression</i>.
 * The expression syntax must follow the one specified for {@link java.util.regex.Pattern Pattern}.
 * The test applies to string values only.
 * @author Laurent Cohen
 */
public class RegExp extends LeftOperandRule {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * A regular expression to match the property value against.
   */
  private String regExp;
  /**
   * The pattern object to compile from the regular expression.
   */
  private transient Pattern pattern;

  /**
   * Define an equality comparison between the string value of a property and another string value.
   * @param propertyNameOrExpression either a literal string which represents a property name, or an expression resolving to a numeric value.
   * @param regExp a regular expression to match the property value against.
   * @throws PatternSyntaxException if the syntax of expression is invalid
   */
  public RegExp(final String propertyNameOrExpression, final String regExp) throws PatternSyntaxException {
    super(ValueType.STRING, propertyNameOrExpression);
    // compiled at creation time to ensure any syntax problem in the expression is known on the client side.
    pattern = Pattern.compile(regExp);
    this.regExp = regExp;
  }

  /**
   * Determines whether this policy accepts the specified node.
   * @param info system information for the node on which the tasks will run if accepted.
   * @return true if the node is accepted, false otherwise.
   */
  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    if (regExp == null) return false;
    Object o = getLeftOperandValue(info);
    if (o == null) return false;
    return pattern.matcher((String) o).matches();
  }

  @Override
  public String toString(final int n) {
    return new StringBuilder(indent(n)).append("<RegExp>\n")
      .append(indent(n + 1)).append("<Property>").append(leftOperand.getExpression()).append("</Property>\n")
      .append(indent(n + 1)).append("<Value>").append(regExp).append("</Value>\n")
      .append(indent(n)).append("</RegExp>\n").toString();
  }

  /**
   * Save the state of this object to a stream (i.e.,serialize it).
   * @param out the output stream to which to write the job. 
   * @throws IOException if any I/O error occurs.
   * @since 5.2
   */
  private void writeObject(final ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }

  /**
   * Reconstitute this object from a stream (i.e., deserialize it).
   * @param in the input stream from which to read the state. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph can not be found.
   * @since 5.2
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    pattern = Pattern.compile(regExp);
  }
}
