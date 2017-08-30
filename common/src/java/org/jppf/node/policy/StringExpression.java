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
 * An expression that represents a string as either a literal or an expression that returns a string.
 * @author Laurent Cohen
 * @exclude
 */
public class StringExpression extends AbstractExpression<String> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * @param expression the expression to evaluate.
   */
  public StringExpression(final String expression) {
    super(expression);
    if (expression != null) {
      if (ExecutionPolicy.isExpression(expression)) {
        literal = false;
      } else {
        value = expression;
        literal = true;
      }
    } else literal = true;
  }

  /**
   * @param expression the expression to evaluate.
   * @param literal whther the expression should be considered a literal string.
   */
  public StringExpression(final String expression, final boolean literal) {
    super(expression);
    this.literal = literal;
    if (literal) value = expression;
  }

  @Override
  String valueOf(final String value) {
    return value;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.STRING;
  }
}
