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

package org.jppf.node.provisioning;

import static org.jppf.node.provisioning.NodeProvisioningConstants.*;

import org.jppf.management.RegistrationCondition;
import org.jppf.management.spi.JPPFNodeMBeanProvider;
import org.jppf.node.Node;
import org.jppf.utils.*;

/**
 * NodeProvisioningMBean provider implementation, discovered by JPPF via the service provider API.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFNodeProvisioningMBeanProvider implements JPPFNodeMBeanProvider, RegistrationCondition {
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
    return new JPPFNodeProvisioning();
  }

  @Override
  public String getMBeanName() {
    return JPPFNodeProvisioningMBean.MBEAN_NAME;
  }

  @Override
  public boolean mustRegister(final Object...params) {
    Node node = (Node) params[0];
    TypedProperties config = JPPFConfiguration.getProperties();
    boolean slave = !node.isOffline() && config.getBoolean(SLAVE_PROPERTY, false);
    boolean master = !node.isOffline() && config.getBoolean(MASTER_PROPERTY, true);
    config.setBoolean(MASTER_PROPERTY, master);
    config.setBoolean(SLAVE_PROPERTY, slave);
    return master;
  }
}
