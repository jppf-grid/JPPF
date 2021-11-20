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

package org.jppf.job;

import java.util.*;

import org.jppf.node.protocol.JPPFDistributedJob;

/**
 * 
 * @author Laurent Cohen
 */
abstract class LogicalJobSelector implements JobSelector {
  /**
   * The operands to perform the logical operation with.
   */
  final JobSelector[] selectors;

  /**
   * 
   * @param selector the first operand.
   * @param others the other operands to perform the logical operation with.
   */
  LogicalJobSelector(final JobSelector selector,final JobSelector...others) {
    if ((selector == null) || (others == null)) throw new IllegalArgumentException("arguments cannot be null");
    final List<JobSelector> list = new ArrayList<>(others.length + 1);
    list.add(selector);
    for (final JobSelector other: others) {
      if (other != null) list.add(other);
    }
    if (list.isEmpty()) throw new IllegalArgumentException("arguments cannot be null or empty");
    this.selectors = list.toArray(new JobSelector[list.size()]);
  }

  @Override
  public int hashCode() {
    return 31 + Arrays.hashCode(selectors);
  }


  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final LogicalJobSelector other = (LogicalJobSelector) obj;
    if (!Arrays.equals(selectors, other.selectors)) return false;
    return true;
  }

  /**
   * 
   */
  static class And extends LogicalJobSelector {
    /**
     * 
     * @param selector the first operand.
     * @param others the other operands to perform the AND operation with.
     */
    And(final JobSelector selector, final JobSelector... others) {
      super(selector, others);
    }

    @Override
    public boolean accepts(final JPPFDistributedJob job) {
      for (final JobSelector selector: selectors) {
        if (!selector.accepts(job)) return false;
      }
      return true;
    }
  }

  /**
   * 
   */
  static class Or extends LogicalJobSelector {
    /**
     * 
     * @param selector the first operand.
     * @param others the other operands to perform the OR operation with.
     */
    Or(final JobSelector selector, final JobSelector... others) {
      super(selector, others);
    }

    @Override
    public boolean accepts(final JPPFDistributedJob job) {
      for (final JobSelector selector: selectors) {
        if (selector.accepts(job)) return true;
      }
      return false;
    }
  }

  /**
   * 
   */
  static class Xor extends LogicalJobSelector {
    /**
     * 
     * @param selector the first operand.
     * @param other the other operands to perform the XOR operation with.
     */
    Xor(final JobSelector selector, final JobSelector other) {
      super(selector, other);
      if (other == null) throw new IllegalArgumentException("'other' argument cannot be null");
    }

    @Override
    public boolean accepts(final JPPFDistributedJob job) {
      if (selectors.length < 2) return false;
      return selectors[0].accepts(job) != selectors[1].accepts(job);
    }
  }

  /**
   * 
   */
  static class Not extends LogicalJobSelector {
    /**
     * 
     * @param selector the selector to negate.
     */
    Not(final JobSelector selector) {
      super(selector, (JobSelector) null);
    }

    @Override
    public boolean accepts(final JPPFDistributedJob job) {
      return !selectors[0].accepts(job);
    }
  }
}
