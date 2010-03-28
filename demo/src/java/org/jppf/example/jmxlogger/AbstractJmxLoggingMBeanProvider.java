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

import org.jppf.management.spi.JPPFMBeanProvider;

/**
 * ABstract Jmx Logger MBean provider for both servers and nodes.
 * @author Laurent Cohen
 */
public abstract class AbstractJmxLoggingMBeanProvider implements JPPFMBeanProvider
{
	/**
	 * Return the fully qualified name of the management interface defined by this provider.
	 * @return the fully qualified interface name as a string.
	 * @see org.jppf.management.spi.JPPFMBeanProvider#getMBeanInterfaceName()
	 */
	public String getMBeanInterfaceName()
	{
		return JmxLoggerMBean.class.getName();
	}

	/**
	 * Return the name of the specified MBean.<br>
	 * This is the name under which the MBean will be registered with the MBean server.
	 * It must be a valid object name, as specified in the documentation for {@link javax.management.ObjectName ObjectName}.
	 * @return the MBean name for this MBean provider.
	 * @see org.jppf.management.spi.JPPFMBeanProvider#getMBeanName()
	 */
	public String getMBeanName()
	{
		return JmxLoggerMBean.JMX_LOGGER_MBEAN_NAME;
	}
}
