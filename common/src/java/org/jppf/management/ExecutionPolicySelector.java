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

package org.jppf.management;

import org.jppf.node.policy.ExecutionPolicy;

/**
 * Selects nodes based on an {@link ExecutionPolicy}.
 * @author Laurent Cohen
 */
public class ExecutionPolicySelector implements NodeSelector, DriverSelector {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The execution policy to use to select the nodes.
   */
  private final ExecutionPolicy policy;

  /**
   * Initialize this selector with the specified execution policy.
   * @param policy the execution policy to use to select the nodes.
   */
  public ExecutionPolicySelector(final ExecutionPolicy policy) {
    this.policy = policy;
  }

  /**
   * Get the execution policy to use to select the nodes.
   * @return an {@link ExecutionPolicy}.
   */
  public ExecutionPolicy getPolicy() {
    return policy;
  }
}
