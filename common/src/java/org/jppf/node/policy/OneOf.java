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
 * An execution policy rule that encapsulates a test of type <i>property_value is one of [value1, &middot;&middot;&middot; , valueN]</i>.
 * The test applies to numeric and string values only.
 * @author Laurent Cohen
 */
public class OneOf extends ExecutionPolicy {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The name of the property to compare.
   */
  private String propertyName = null;
  /**
   * A numeric value to compare with.
   */
  private double[] numberValues = null;
  /**
   * A string value to compare with.
   */
  private String[] stringValues = null;
  /**
   * Determines if the comparison should ignore the string case.
   */
  private boolean ignoreCase = false;

  /**
   * Determine whether the value of a property, expressed as a {@code double}, is in the specified array of values.
   * @param propertyName the name of the property to compare.
   * @param values the values to compare with.
   */
  public OneOf(final String propertyName, final double... values) {
    this.propertyName = propertyName;
    this.numberValues = values;
  }

  /**
   * Determine whether the value of a property, expressed as a {@code String}, is in the specified array of values.
   * @param propertyName the name of the property to compare.
   * @param ignoreCase determines if the comparison should ignore the string case.
   * @param values the values to compare with.
   */
  public OneOf(final String propertyName, final boolean ignoreCase, final String... values) {
    this.propertyName = propertyName;
    this.stringValues = values;
    this.ignoreCase = ignoreCase;
  }

  /**
   * Determines whether this policy accepts the specified node.
   * @param info system information for the node on which the tasks will run if accepted.
   * @return true if the node is accepted, false otherwise.
   */
  @Override
  public boolean accepts(final PropertiesCollection info) {
    try {
      String s = getProperty(info, propertyName);
      if (numberValues != null) {
        double value = Double.valueOf(s);
        for (double d : numberValues) if (d == value) return true;
      } else if (stringValues != null) {
        for (String value : stringValues) {
          if ((value == null) && (s == null)) return true;
          else if ((value != null) && (s != null)) {
            if (!ignoreCase && s.equals(value)) return true;
            else if (ignoreCase && s.equalsIgnoreCase(value)) return true;
          }
        }
      }
    } catch (Exception e) {
    }
    return false;
  }

  /**
   * Print this object to a string.
   * @return an XML string representation of this object
   */
  @Override
  public String toString() {
    if (computedToString == null) {
      synchronized (ExecutionPolicy.class) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("<OneOf valueType=\"");
        if (stringValues != null) sb.append("string");
        else if (numberValues != null) sb.append("numeric");
        sb.append("\" ignoreCase=\"").append(ignoreCase).append("\">\n");
        toStringIndent++;
        sb.append(indent()).append(xmlElement("Property", propertyName)).append('\n');
        if (stringValues != null) {
          for (String s : stringValues) sb.append(indent()).append(xmlElement("Value", s)).append('\n');
        } else {
          for (double d : numberValues) sb.append(indent()).append(xmlElement("Value", d)).append('\n');
        }
        toStringIndent--;
        sb.append(indent()).append("</OneOf>\n");
        computedToString = sb.toString();
      }
    }
    return computedToString;
  }
}
