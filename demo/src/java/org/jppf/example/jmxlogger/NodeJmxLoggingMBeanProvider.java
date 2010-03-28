/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.example.jmxlogger;

import org.jppf.management.spi.*;
import org.jppf.node.MonitoredNode;

/**
 * Node provider for the JMX logger.
 * @author Laurent Cohen
 */
public class NodeJmxLoggingMBeanProvider extends AbstractJmxLoggingMBeanProvider implements JPPFNodeMBeanProvider
{
	/**
	 * Create a {@link JmxLogger} instance.
	 * @param node not used.
	 * @return an <code>Object</code> that is an implementation of the MBean interface.
	 * @see org.jppf.management.spi.JPPFNodeMBeanProvider#createMBean(org.jppf.node.MonitoredNode)
	 */
	public Object createMBean(MonitoredNode node)
	{
		return new JmxLogger();
	}
}
