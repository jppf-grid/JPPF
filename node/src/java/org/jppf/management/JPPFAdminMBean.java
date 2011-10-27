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

package org.jppf.management;

import java.io.Serializable;

/**
 * Management interface for the administration of a JPPF component, driver or node.
 * @author Laurent Cohen
 */
public interface JPPFAdminMBean extends Serializable
{
	/**
	 * RMI registry namespace suffix for drivers.
	 */
	String DRIVER_SUFFIX = "/jppf/driver";
	/**
	 * RMI registry namespace suffix for nodes.
	 */
	String NODE_SUFFIX = "/jppf/node";
	/**
	 * Name of the node's admin MBean.
	 */
	String NODE_MBEAN_NAME = "org.jppf:name=admin,type=node";
	/**
	 * Name of the driver's admin MBean.
	 */
	String DRIVER_MBEAN_NAME = "org.jppf:name=admin,type=driver";

	/**
	 * Get detailed information about the node's JVM properties, environment variables
	 * and runtime information such as memory usage and available processors.
	 * @return a <code>JPPFSystemInformation</code> instance.
	 * @throws Exception if any error occurs.
	 */
	JPPFSystemInformation systemInformation() throws Exception;
}
