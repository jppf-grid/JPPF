/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.utils;

/**
 * Enumeration of the possible boolean operators that can be used as a comparison predicates for integer/long values.
 * @author Laurent Cohen
 */
public enum Operator implements ComparisonOperator {
  /**
   * The number of connections is equal to the expected number.
   */
  EQUAL {
    @Override
    public boolean evaluate(final long actual, final long expected) { return actual == expected; }
    @Override
    public String toString() { return "equal to"; }
  },
  /**
   * The number of connections is different from the expected number.
   */
  NOT_EQUAL {
    @Override
    public boolean evaluate(final long actual, final long expected) { return actual != expected; }
    @Override
    public String toString() { return "not equal to"; }
  },
  /**
   * The number of connections is at least the expected number.
   */
  AT_LEAST {
    @Override
    public boolean evaluate(final long actual, final long expected) { return actual >= expected; }
  },
  /**
   * The number of connections is at most the expected number.
   */
  AT_MOST {
    @Override
    public boolean evaluate(final long actual, final long expected) { return actual <= expected; }
  },
  /**
   * The number of connections is strictly greater than the expected number.
   */
  MORE_THAN {
    @Override
    public boolean evaluate(final long actual, final long expected) { return actual > expected; }
  },
  /**
   * The number of connections is strictly less than the expected number.
   */
  LESS_THAN {
    @Override
    public boolean evaluate(final long actual, final long expected) { return actual < expected; }
  };

  @Override
  public String toString() {
    return name().toLowerCase().replace("_", " ");
  }
}
