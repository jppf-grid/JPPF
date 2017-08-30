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

import java.io.Serializable;

import org.jppf.utils.*;

/**
 * Interface for values that can be represented as either literals or expressions similar to the values of JPPF
 * configuration properties that include property substituions and/or scripted expressions.
 * Property substitutions are in the form <code>${property_name}</code>, whereas scripted expressions are in the
 * form <code>$script:language:source_type{script_inline_or_location}$</code>.
 * <p>In the case of execution policies, substituted values and scripted expressions apply to a {@link PropertiesCollection}
 * instead of a single {@link TypedProperties} object.
 * <p>Scripted expressions have a predefined variable named "{@code jppfSystemInfo}" which references a {@link org.jppf.management.JPPFSystemInformation JPPFSystemInformation} object.
 * @param <E> the type of results returned by the expression.
 * @see <a href="http://www.jppf.org/doc/6.0/index.php?title=Includes,_substitutions_and_scripted_values_in_the_configuration#Substitutions_in_the_values_of_properties">
 * Substitutions in the values of properties</a>
 * @see <a href="http://www.jppf.org/doc/6.0/index.php?title=Includes,_substitutions_and_scripted_values_in_the_configuration#Scripted_property_values">Scripted property values</a>
 * @author Laurent Cohen
 * @exclude
 */
public interface Expression<E> extends Serializable {
  /**
   * Evaluate this expression against a set of properties.
   * @param properties against which to evaluate.
   * @return the expression result.
   */
  E evaluate(PropertiesCollection<String> properties);

  /**
   * @return the string representing the expression.
   */
  String getExpression();

  /**
   * @return whether this expression is a literal value.
   */
  boolean isLiteral();

  /**
   * @return the type of value this expression returns.
   */
  ValueType getValueType();
}
