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
 * A special type or expression that represents either the name of a property or an expression that resolves to a specified target type.
 * @author Laurent Cohen
 * @exclude
 */
public class PropertyNameExpression implements Expression<String> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The name of a property as a literal
   */
  final String propertyNameOrExpression;
  /**
   * The type of value for the property whose name is specfied in this expression.
   */
  final ValueType targetValueType;

  /**
   * Initialize with the specified property name.
   * @param propertyNameOrExpression the name of a property as a literal.
   * @param targetValueType the type of value for the property whose name is specfied in this expression.
   */
  public PropertyNameExpression(final String propertyNameOrExpression, final ValueType targetValueType) {
    this.propertyNameOrExpression = propertyNameOrExpression;
    this.targetValueType = targetValueType;
  }

  @Override
  public String evaluate(final PropertiesCollection<String> properties) {
    return propertyNameOrExpression;
  }

  @Override
  public String getExpression() {
    return propertyNameOrExpression;
  }

  @Override
  public boolean isLiteral() {
    return true;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.PROPERTY_NAME;
  }

  /**
   * @return the type of value for the property whose name is specfied in this expression.
   */
  public ValueType getTargetValueType() {
    return targetValueType;
  }
}
