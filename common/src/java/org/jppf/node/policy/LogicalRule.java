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

import java.util.*;

import org.jppf.JPPFRuntimeException;

/**
 * An execution policy that realizes a binary logical combination of the policies specified as operands.
 */
public abstract class LogicalRule extends ExecutionPolicy {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this binary logical operator with the specified operands.
   * @param rules the first operand.
   */
  public LogicalRule(final ExecutionPolicy...rules) {
    super(rules);
  }

  /**
   * Print this object to a string.
   * @return an XML string representation of this object
   */
  @Override
  public String toString(final int indentLevel) {
    final StringBuilder sb = new StringBuilder();
    if (children == null) sb.append(indent(indentLevel)).append("null\n");
    else {
      for (final ExecutionPolicy ep: children) sb.append(ep.toString(indentLevel));
    }
    return sb.toString();
  }

  @Override
  protected ExecutionPolicy[] checkRules(final ExecutionPolicy...rules) throws JPPFRuntimeException {
    if ((rules != null) && (rules.length > 0)) {
      final List<ExecutionPolicy> result = new ArrayList<>();
      for (final ExecutionPolicy rule: rules) {
        if (rule != null) result.add(rule);
      }
      if (result.size() == 1) result.add(result.get(0));
      if (result.size() > 0) return result.toArray(new ExecutionPolicy[result.size()]);
    }
    final String message = String.format("a '%s' execution policy must have at lest one non-null argument", getClass().getSimpleName());
    throw new JPPFRuntimeException(message);
  }
}
