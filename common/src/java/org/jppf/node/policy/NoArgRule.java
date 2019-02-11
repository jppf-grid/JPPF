/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

import org.jppf.management.JPPFSystemInformation;
import org.jppf.utils.PropertiesCollection;
import org.jppf.utils.configuration.*;

/**
 * An execution policy predicates that determines whether a node is a master node.
 * @author Laurent Cohen
 */
class NoArgRule extends ExecutionPolicy {
  /**
   * The name of the property to lookup in {@link #accepts(PropertiesCollection)}.
   */
  private final String propertyName;
  /**
   * The property's default value.
   */
  private final boolean defaultValue;

  /**
   * Construct this policy rule from the name of a property.
   * @param propertyName the name of the property to lookup in {@link #accepts(PropertiesCollection)}.
   * @param defaultValue the property's default value.
   */
  public NoArgRule(final String propertyName, final boolean defaultValue) {
    this.propertyName = propertyName;
    this.defaultValue = defaultValue;
  }

  /**
   * Construct this policy rule from a {@link JPPFProperty} instance.
   * This is equivalent to calling {@code new NoArgRule(property.getName())}.
   * @param property the property whose name to lookup in {@link #accepts(PropertiesCollection)}.
   */
  public NoArgRule(final JPPFProperty<?> property) {
    this(property.getName(), (property instanceof BooleanProperty) ? ((BooleanProperty) property).getDefaultValue() : false);
  }

  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    final String val = ((JPPFSystemInformation) info).getProperty(propertyName);
    return (val == null) ? defaultValue : Boolean.valueOf(val);
  }

  @Override
  public String toString(final int n) {
    return new StringBuilder(indent(n)).append("<").append(getClass().getSimpleName()).append("/>\n").toString();
  }

  @Override
  public String toXML() {
    return super.toXML();
  }
}
