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

import java.io.Serializable;

/**
 * Interface for numerical binary comparison operators.
 * @author Laurent Cohen
 */
@FunctionalInterface
public interface ComparisonOperator extends Serializable {
  /**
   * Evaluate the condition based on the actual and expected numbers.
   * @param actual the actual number.
   * @param expected the expected numbers
   * @return true if the condition is matched, false otherwise.
   */
  boolean evaluate(long actual, long expected);

  /*
  default ComparisonOperator and(final ComparisonOperator operator) {
    return (actual, expected) -> evaluate(actual, expected) && operator.evaluate(actual, expected);
  }

  default ComparisonOperator or(final ComparisonOperator operator) {
    return (actual, expected) -> evaluate(actual, expected) || operator.evaluate(actual, expected);
  }

  static ComparisonOperator not(final ComparisonOperator operator) {
    return (actual, expected) -> !operator.evaluate(actual, expected);
  }
  */
}
