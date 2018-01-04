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

import java.util.List;

import org.jppf.utils.PropertiesCollection;

/**
 * A policy which evaluates a set of policies ordered by preference.
 * @author Laurent Cohen
 */
public class Preference extends LogicalRule {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this policy with the specified array of children polices.
   * @param policies the polcies in order of preference.
   */
  public Preference(final ExecutionPolicy... policies) {
    super(policies);
    if ((policies == null) || (policies.length == 0)) throw new IllegalArgumentException("there must be at least one policy in the list");
  }

  /**
   * Initialize this policy with the specified array of children polices.
   * @param policies the polcies in order of preference.
   */
  public Preference(final List<ExecutionPolicy> policies) {
    super(policies.toArray(new ExecutionPolicy[policies.size()]));
    if (policies.isEmpty()) throw new IllegalArgumentException("there must be at least one policy in the list");
  }

  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    for (ExecutionPolicy policy: children) {
      if ((policy == null) || policy.evaluate(info)) return true;
    }
    return false;
  }

  @Override
  public String toString(final int n) {
    return new StringBuilder().append(indent(n)).append("<Preference>\n").append(super.toString(n)).append(indent(n)).append("</Preference>\n").toString();
  }
}
