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
import org.jppf.utils.configuration.*;

/**
 * Common super class for expressions written as the value of configuration properties containing;
 * properties substitutions and scripted values, i.e. containing expressions of the form "<code>${&lt;property_name&gt;}</code>"
 * or "<code>$script:&lt;language&gt;:&lt;script_source&gt;{&lt;inline_script_or_script_location&gt;}$</code>".<p>
 * @param <E> the type of results returned by the expression.
 * @see <a href="http://www.jppf.org/doc/6.0/index.php?title=Includes,_substitutions_and_scripted_values_in_the_configuration#Substitutions_in_the_values_of_properties">
 * Substitutions in the values of properties</a>
 * @see <a href="http://www.jppf.org/doc/6.0/index.php?title=Includes,_substitutions_and_scripted_values_in_the_configuration#Scripted_property_values">Scripted property values</a>
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractExpression<E> implements Expression<E> {
  /**
   * The expression to evaluate.
   */
  String expression;
  /**
   * Whether this expression represents a literal value.
   */
  boolean literal;
  /**
   * The value to return if this expression is a boolean literal.
   */
  E value;

  /**
   * Initialize with a string expression.
   * @param expression the expression to evaluate.
   */
  public AbstractExpression(final String expression) {
    this.expression = expression;
  }

  @Override
  public E evaluate(final PropertiesCollection<String> properties) {
    if (literal) return value;
    return valueOf(compute(properties));
  }

  /**
   * Compute and resolve the expression as a string.
   * @param properties the prroperties to evaluate against.
   * @return the resulting value as a string.
   */
  String compute(final PropertiesCollection<String> properties) {
    String s = new SubstitutionsHandler().evaluateProp(properties, expression);
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("jppfSystemInfo", properties);
    return  new ScriptHandler().evaluate(expression, s, bindings);
  }

  /**
   * Convert a string value to this expression's type.
   * @param value the value to convert.
   * @return the convert value.
   */
  abstract E valueOf(String value);

  @Override
  public String getExpression() {
    return literal ? (value == null ? "" : value.toString()) : expression;
  }

  @Override
  public boolean isLiteral() {
    return literal;
  }
}
