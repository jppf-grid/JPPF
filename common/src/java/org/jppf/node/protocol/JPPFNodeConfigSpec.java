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

package org.jppf.node.protocol;

import java.io.Serializable;

import org.jppf.utils.TypedProperties;

/**
 * Instances of this class are set as job SLA attributes and describe the desired node configuration for a job.
 * @author Laurent Cohen
 */
public class JPPFNodeConfigSpec implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The desired JPPF configuration of each node.
   */
  private final TypedProperties configuration;
  /**
   * Whether to force the restart of a node after reconfiguring it.
   */
  private final boolean forceRestart;

  /**
   * Initialize this node config spec with the specified desiredConfiguration and a restart flag set to {@code true}.
   * @param desiredConfiguration the desired JPPF configuration of each node.
   * @throws IllegalArgumentException if {@code desiredConfiguration} is {@code null}.
   */
  public JPPFNodeConfigSpec(final TypedProperties desiredConfiguration) {
    this(desiredConfiguration, true);
  }

  /**
   * Initialize this node config spec with the specified desiredConfiguration and restart flag.
   * @param desiredConfiguration the desired JPPF configuration of each node.
   * @param forceRestart whether to force the restart of a node after reconfiguring it.
   * @throws IllegalArgumentException if {@code desiredConfiguration} is {@code null}.
   */
  public JPPFNodeConfigSpec(final TypedProperties desiredConfiguration, final boolean forceRestart) throws IllegalArgumentException {
    if (desiredConfiguration == null)
      throw new IllegalArgumentException("the desired configuration of a " + getClass().getSimpleName() + " cannot be null");
    this.configuration = desiredConfiguration;
    this.forceRestart = forceRestart;
  }

  /**
   * Get the desired JPPF configuration of each node.
   * @return a TypedProperties object containing the desired configuration properties.
   */
  public TypedProperties getConfiguration() {
    return configuration;
  }

  /**
   * Determine whether to force the restart of a node after reconfiguring it.
   * @return {@code true} if each node is to be restarted after reconfiguration, {@code false} otherwise.
   */
  public boolean isForceRestart() {
    return forceRestart;
  }
}
