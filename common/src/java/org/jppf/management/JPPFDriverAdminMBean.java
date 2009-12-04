/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import java.util.*;

import org.jppf.server.*;
import org.jppf.server.protocol.BundleParameter;

/**
 * MBean interface for the management of a JPPF driver.
 * @author Laurent Cohen
 */
public interface JPPFDriverAdminMBean extends JPPFAdminMBean<BundleParameter, Object>
{
	/**
	 * Get the latest statistics snapshot from the JPPF driver.
	 * @return a <code>JPPFStats</code> instance.
	 * @throws Exception if any error occurs.
	 */
	JPPFStats statistics() throws Exception;
	/**
	 * Request the JMX connection information for all the nodes attached to the server.
	 * @return a collection of <code>NodeManagementInfo</code> instances.
	 * @throws Exception if any error occurs.
	 */
	Collection<NodeManagementInfo> nodesInformation() throws Exception;
	/**
	 * Process a management or monitoring request.
	 * @param parameters the parameters of the request to process
	 * @return the result of the request.
	 * @throws Exception if an error occurred while performing the request.
	 */
	Object processManagementRequest(Map<BundleParameter, Object> parameters) throws Exception;
}
