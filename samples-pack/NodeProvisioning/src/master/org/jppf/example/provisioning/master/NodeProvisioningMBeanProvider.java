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

import org.jppf.management.spi.JPPFNodeMBeanProvider;
import org.jppf.node.Node;

/**
 * NodeProvisioningMBean provider implementation, discovered by JPPF via the service provider API.
 * @author Laurent Cohen
 */
public class NodeProvisioningMBeanProvider implements JPPFNodeMBeanProvider {
  /**
   * Get the fully qualified name of the MBean interface defined by this provider.
   * @return the name as a string.
   */
  @Override
  public String getMBeanInterfaceName() {
    return NodeProvisioningMBean.class.getName();
  }

  /**
   * Create a concrete MBean instance.
   * @param node a reference ot the JPPF node object - used by the built-in JPPF MBeans.
   * @return the created MBean implementation.
   */
  @Override
  public Object createMBean(final Node node) {
    return new NodeProvisioning();
  }

  /**
   * Get the object name of the specified MBean.
   * @return the MBean's object name as a string.
   */
  @Override
  public String getMBeanName() {
    return NodeProvisioningMBean.MBEAN_NAME;
  }
}
