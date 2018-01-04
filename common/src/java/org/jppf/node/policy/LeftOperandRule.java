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
 * Common abstract superclass for execution policies whose left operands can be either an expression or a property name.
 * @author Laurent Cohen
 */
abstract class LeftOperandRule extends ExecutionPolicy {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * either a literal string which represents a property name, or an expression resolving to a numeric value.
   */
  Expression<?> leftOperand;

  /**
   * Define an equality comparison between the numeric value of a property and another numeric value.<br>
   * Note that if the value tyoe is equal to {@link ValueType.STRING}, then the comparison will be case-insensitive.
   * To specify case sensitivity, use the {@link #Equal(String, boolean, String)} constructor instead.
   * @param valueType the type of value to which thie equality comparison applies. 
   * @param leftExpr either a literal string that represents a property name, or an expression which resolves to a value of the specified type.
   */
  LeftOperandRule(final ValueType valueType, final String leftExpr) {
    if (ExecutionPolicy.isExpression(leftExpr)) {
      switch(valueType) {
        case BOOLEAN:
          this.leftOperand = new BooleanExpression(leftExpr);
          break;
  
        case NUMERIC:
          this.leftOperand = new NumericExpression(leftExpr);
          break;
  
        case STRING:
          this.leftOperand = new StringExpression(leftExpr);
          break;
      }
    } else {
      this.leftOperand = new PropertyNameExpression(leftExpr, valueType);
    }
  }

  /**
   * @param properties the proeprties collection to evaluate against.
   * @return the typed value of the left operand expression.
   */
  protected Object getLeftOperandValue(final PropertiesCollection<String> properties) {
    switch(leftOperand.getValueType()) {
      case BOOLEAN:
      case NUMERIC:
      case STRING:
        return leftOperand.evaluate(properties);

      case PROPERTY_NAME:
        final String s = properties.getProperty(leftOperand.getExpression());
        switch(((PropertyNameExpression) leftOperand).getTargetValueType()) {
          case BOOLEAN:
            return Boolean.valueOf(s);
    
          case NUMERIC:
            return Double.valueOf(s);
    
          case STRING:
            return s;
        }
        break;
    }
    return null;
  }
}
