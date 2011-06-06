/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.example.tasknotifications.mbean;

import org.jppf.management.spi.JPPFNodeMBeanProvider;
import org.jppf.node.MonitoredNode;

/**
 * TaskNotifications MBean provider implementation.
 * @author Laurent Cohen
 */
public class TaskNotificationsMBeanProvider implements JPPFNodeMBeanProvider
{
	/**
	 * Get the fully qualified name of the MBean interface defined by this provider.
	 * @return the name as a string.
	 * @see org.jppf.management.spi.JPPFMBeanProvider#getMBeanInterfaceName()
	 */
	public String getMBeanInterfaceName()
	{
		return TaskNotificationsMBean.class.getName();
	}

	/**
	 * Create a concrete MBean instance.
	 * @param node not used.
	 * @return the created MBean implementation.
	 * @see org.jppf.management.spi.JPPFNodeMBeanProvider#createMBean(org.jppf.node.MonitoredNode)
	 */
	public Object createMBean(MonitoredNode node)
	{
		return new TaskNotifications();
	}

	/**
	 * Get the object name for the MBean.
	 * @return the MBean's object name as a string.
	 * @see org.jppf.management.spi.JPPFMBeanProvider#getMBeanName()
	 */
	public String getMBeanName()
	{
		return TaskNotificationsMBean.MBEAN_NAME;
	}
}
