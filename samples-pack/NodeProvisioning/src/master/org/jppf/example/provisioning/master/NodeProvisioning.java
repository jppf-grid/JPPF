/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.example.provisioning.master;

import org.jppf.utils.TypedProperties;

/**
 * Implementation of the {@link NodeProvisioningMBean} interface.
 * @author Laurent Cohen
 */
public class NodeProvisioning implements NodeProvisioningMBean {
  /**
   * The slave node manager, to which all operations are delegated.
   */
  private final SlaveNodeManager slaveManager;

  /**
   * Initialize this MBean.
   */
  public NodeProvisioning() {
    slaveManager = new SlaveNodeManager();
  }

  @Override
  public Integer getNbSlaves() {
    return slaveManager.nbSlaves();
  }

  @Override
  public void provisionSlaveNodes(final Integer nbNodes) {
    slaveManager.shrinkOrGrowSlaves(nbNodes, null);
  }

  @Override
  public void provisionSlaveNodes(final Integer nbNodes, final TypedProperties configOverrides) {
    slaveManager.shrinkOrGrowSlaves(nbNodes, configOverrides);
  }
}
