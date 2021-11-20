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

package org.jppf.node.provisioning;

import org.jppf.management.spi.JPPFNodeMBeanProvider;
import org.jppf.node.Node;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;

/**
 * NodeProvisioningMBean provider implementation, discovered by JPPF via the service provider API.
 * @author Laurent Cohen
 * @since 4.1
 * @exclude
 */
public class JPPFNodeProvisioningMBeanProvider implements JPPFNodeMBeanProvider {
  /**
   * Iniitialize this MBean provider.
   */
  public JPPFNodeProvisioningMBeanProvider() {
  }

  @Override
  public String getMBeanInterfaceName() {
    return JPPFNodeProvisioningMBean.class.getName();
  }

  @Override
  public Object createMBean(final Node node) {
    //return mustRegister(node) ? new JPPFNodeProvisioning(node) : null;
    return new JPPFNodeProvisioning(node);
  }

  @Override
  public String getMBeanName() {
    return JPPFNodeProvisioningMBean.MBEAN_NAME;
  }

  /**
   * Determine whether this MBean should be registeres in the current node
   * @param node the node in which to register.
   * @return true if the node is a master node, false otherwise.
   */
  static boolean mustRegister(final Node node) {
    final TypedProperties config = node.getConfiguration();
    final boolean slave = !node.isOffline() && config.get(JPPFProperties.PROVISIONING_SLAVE);
    final boolean master = !node.isOffline() && config.get(JPPFProperties.PROVISIONING_MASTER);
    config.set(JPPFProperties.PROVISIONING_MASTER, master);
    config.set(JPPFProperties.PROVISIONING_SLAVE, slave);
    return master;
  }
}
