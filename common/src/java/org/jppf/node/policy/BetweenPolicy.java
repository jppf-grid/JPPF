/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
 * An execution policy rule that encapsulates a test of type <i>a &lt; property_value &lt; b</i>.
 * The test applies to numeric values only.
 * @author Laurent Cohen
 * @since 5.0
 */
public abstract class BetweenPolicy extends ExecutionPolicy {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The name of the property to compare.
   */
  protected final String propertyName;
  /**
   * The interval's lower bound.
   */
  protected final double a;
  /**
   * The interval's upper bound.
   */
  protected final double b;
  /**
   * String to use for the bounds. 
   */
  private static final String[] BOUNDS = { "EE", "EI", "IE", "II" };
  /**
   * Index of the string to use for the bounds. 
   */
  private final byte boundsIndex;

  /**
   * Define a comparison of type value between a and b.
   * @param propertyName the name of the property to compare.
   * @param a the lower bound.
   * @param b the upper bound.
   * @param boundsIndex the index of string to use for the bounds.
   * @exclude
   */
  BetweenPolicy(final String propertyName, final double a, final double b, final int boundsIndex) {
    this.propertyName = propertyName;
    this.a = a;
    this.b = b;
    this.boundsIndex = (byte) boundsIndex;
  }

  /**
   * Determines whether this policy accepts the specified node.
   * @param info system information for the node on which the tasks will run if accepted.
   * @return true if the node is accepted, false otherwise.
   */
  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    try {
      String s = getProperty(info, propertyName);
      if (s != null) {
        double value = Double.valueOf(s);
        return accepts(value);
      }
    } catch(@SuppressWarnings("unused") Exception e) {
    }
    return false;
  }

  /**
   * Determines whether this policy accepts the specified value.
   * @param value a value to compare to the lower and upper bounds.
   * @return {@code true} if the value is accepted, {@code false} otherwise.
   */
  abstract boolean accepts(final double value);

  /**
   * Print this object to a string.
   * @return an XML string representation of this object
   */
  @Override
  public String toString() {
    if (computedToString == null) {
      synchronized(ExecutionPolicy.class) {
        StringBuilder sb = new StringBuilder();
        String name = new StringBuilder("Between").append(BOUNDS[boundsIndex]).toString();
        sb.append(indent()).append('<').append(name).append(">\n");
        toStringIndent++;
        sb.append(indent()).append("<Property>").append(propertyName).append("</Property>\n");
        sb.append(indent()).append("<Value>").append(a).append("</Value>\n");
        sb.append(indent()).append("<Value>").append(b).append("</Value>\n");
        toStringIndent--;
        sb.append(indent()).append("</").append(name).append(">\n");
        computedToString = sb.toString();
      }
    }
    return computedToString;
  }
}
