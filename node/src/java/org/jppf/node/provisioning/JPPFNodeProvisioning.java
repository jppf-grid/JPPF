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

package org.jppf.node.provisioning;

import org.jppf.utils.TypedProperties;

/**
 * Implementation of the {@link JPPFNodeProvisioningMBean} interface.
 * @author Laurent Cohen
 * @since 4.1
 * @exclude
 */
public class JPPFNodeProvisioning implements JPPFNodeProvisioningMBean {
  /**
   * The slave node manager, to which all operations are delegated.
   */
  private final SlaveNodeManager slaveManager;

  /**
   * Initialize this MBean.
   */
  public JPPFNodeProvisioning() {
    slaveManager = SlaveNodeManager.INSTANCE;
  }

  @Override
  public int getNbSlaves() {
    return slaveManager.nbSlaves();
  }

  @Override
  public void provisionSlaveNodes(final int nbNodes) {
    slaveManager.submitProvisioningRequest(nbNodes, true, null);
  }

  @Override
  public void provisionSlaveNodes(final int nbNodes, final boolean interruptIfRunning) {
    slaveManager.submitProvisioningRequest(nbNodes, interruptIfRunning, null);
  }

  @Override
  public void provisionSlaveNodes(final int nbNodes, final TypedProperties configOverrides) {
    slaveManager.submitProvisioningRequest(nbNodes, true, configOverrides);
  }

  @Override
  public void provisionSlaveNodes(final int nbNodes, final boolean interruptIfRunning, final TypedProperties configOverrides) {
    slaveManager.submitProvisioningRequest(nbNodes, interruptIfRunning, configOverrides);
  }
}
