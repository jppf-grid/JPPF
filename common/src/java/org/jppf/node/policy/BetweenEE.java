/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
 * An execution policy rule that encapsulates a test of type <i>a &lt; property_value &lt; b</i>.
 * The test applies to numeric values only.
 * @author Laurent Cohen
 */
public class BetweenEE extends BetweenPolicy {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Define a comparison of type value between a and b with a excluded and b excluded.
   * @param propertyName the name of the property to compare.
   * @param a the lower bound.
   * @param b the upper bound.
   */
  public BetweenEE(final String propertyName, final double a, final double b) {
    super(propertyName, a, b, 0);
  }

  @Override
  boolean accepts(final double value) {
    return (value > a) && (value < b);
  }
}
