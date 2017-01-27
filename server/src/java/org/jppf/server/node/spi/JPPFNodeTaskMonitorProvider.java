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

package org.jppf.server.node.spi;

import org.jppf.management.*;
import org.jppf.management.spi.JPPFNodeMBeanProvider;
import org.jppf.node.Node;
import org.jppf.server.node.JPPFNode;

/**
 * SPI Provider implementation for task-level monitoring MBean.
 * @author Laurent Cohen
 */
public class JPPFNodeTaskMonitorProvider implements JPPFNodeMBeanProvider
{
  /**
   * Return the fully qualified name of the management interface defined by this provider.
   * @return the fully qualified interface name as a string.
   */
  @Override
  public String getMBeanInterfaceName()
  {
    return JPPFNodeTaskMonitorMBean.class.getName();
  }

  /**
   * Return a concrete MBean.<br>
   * The class of this MBean must implement the interface defined by {@link #getMBeanInterfaceName() getMBeanInterfaceName()}.
   * @param node - the JPPF node that is managed or monitored by the MBean.
   * @return an <code>Object</code> that is an implementation of the MBean interface.
   */
  @Override
  public Object createMBean(final Node node)
  {
    JPPFNodeTaskMonitor monitor = new JPPFNodeTaskMonitor(JPPFNodeTaskMonitorMBean.MBEAN_NAME);
    ((JPPFNode) node).getExecutionManager().getTaskNotificationDispatcher().addTaskExecutionListener(monitor);
    return monitor;
  }

  /**
   * Return the name of the specified MBean.<br>
   * This is the name under which the MBean will be registered with the MBean server.
   * It must be a valid object name, as specified in the documentation for {@link javax.management.ObjectName ObjectName}.
   * @return the MBean name for this MBean provider.
   */
  @Override
  public String getMBeanName()
  {
    return JPPFNodeTaskMonitorMBean.MBEAN_NAME;
  }
}
