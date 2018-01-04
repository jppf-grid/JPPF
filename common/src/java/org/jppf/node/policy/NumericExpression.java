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
 * An expression that represents a numeric value as either a literal or an expression that returns a numeric value.
 * @author Laurent Cohen
 * @exclude
 */
public class NumericExpression extends AbstractExpression<Double> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * @param expression the expression to evaluate.
   */
  public NumericExpression(final String expression) {
    super(expression);
    try {
      value = Double.valueOf(expression);
      literal = true;
      this.expression = null;
    } catch (@SuppressWarnings("unused") final Exception e) {
    }
  }

  /**
   * Initialize as a literal numeric value.
   * @param value the value to return.
   */
  public NumericExpression(final Double value) {
    super(null);
    this.value = value;
    literal = true;
  }

  @Override
  Double valueOf(final String value) {
    try {
      return Double.valueOf(value);
    } catch (@SuppressWarnings("unused") final Exception e) {
      return 0d;
    }
  }

  @Override
  public ValueType getValueType() {
    return ValueType.NUMERIC;
  }
}
