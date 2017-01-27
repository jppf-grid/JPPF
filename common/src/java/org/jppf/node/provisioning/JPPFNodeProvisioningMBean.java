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

package org.jppf.node.provisioning;

import org.jppf.utils.TypedProperties;

/**
 * Provides an interface for "master" nodes, giving them the ability to start, stop and monitor "slave" nodes on the same machine.
 * @author Laurent Cohen
 * @since 4.1
 */
public interface JPPFNodeProvisioningMBean {
  /**
   * The object name of this MBean.
   */
  String MBEAN_NAME = "org.jppf:name=provisioning,type=node";

  /**
   * Get the number of slave nodes started by this MBean.
   * @return the number of slave nodes as an integer value.
   */
  int getNbSlaves();

  /**
   * Start or stop the required number of slaves to reach the specified number.
   * This is equivalent to calling {@code provisionSlaveNodes(nbNodes, null)}.
   * @param nbNodes the number of slave nodes to reach.
   */
  void provisionSlaveNodes(int nbNodes);

  /**
   * Start or stop the required number of slaves to reach the specified number.
   * This is equivalent to calling {@code provisionSlaveNodes(nbNodes, null)}.
   * @param nbNodes the number of slave nodes to reach.
   * @param interruptIfRunning if true then nodes can only be stopped once they are idle. 
   */
  void provisionSlaveNodes(int nbNodes, boolean interruptIfRunning);

  /**
   * Start or stop the required number of slaves to reach the specified number,
   * using the specified config overrides.
   * <p>If {@code configOverrides} is null, then previous overrides are applied,
   * and already running slave nodes do not need to be stopped.
   * @param nbNodes the number of slave nodes to reach.
   * @param configOverrides the configuration overrides to apply.
   */
  void provisionSlaveNodes(int nbNodes, TypedProperties configOverrides);

  /**
   * Start or stop the required number of slaves to reach the specified number,
   * using the specified config overrides.
   * <p>If {@code configOverrides} is null, then previous overrides are applied,
   * and already running slave nodes do not need to be stopped.
   * @param nbNodes the number of slave nodes to reach.
   * @param interruptIfRunning if true then nodes can only be stopped once they are idle. 
   * @param configOverrides the configuration overrides to apply.
   */
  void provisionSlaveNodes(int nbNodes, boolean interruptIfRunning, TypedProperties configOverrides);
}
