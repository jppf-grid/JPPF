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

package org.jppf.node.debug;

import org.jppf.management.spi.JPPFNodeMBeanProvider;
import org.jppf.node.Node;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.configuration.JPPFProperties;

/**
 * NodeDebugMBean provider implementation, discovered by JPPF via the service provider API.
 * @author Laurent Cohen
 * @exclude
 */
public class NodeDebugMBeanProvider implements JPPFNodeMBeanProvider {
  /**
   * Iniitialize this MBean provider.
   */
  public NodeDebugMBeanProvider() {
  }

  @Override
  public String getMBeanInterfaceName() {
    return NodeDebugMBean.class.getName();
  }

  @Override
  public Object createMBean(final Node node) {
    return JPPFConfiguration.get(JPPFProperties.DEBUG_ENABLED) ? new NodeDebug() : null;
  }

  @Override
  public String getMBeanName() {
    return NodeDebugMBean.MBEAN_NAME;
  }
}
