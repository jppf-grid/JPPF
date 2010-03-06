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

package org.jppf.management;

import java.util.*;

import org.jppf.server.JPPFStats;
import org.jppf.server.scheduler.bundle.LoadBalancingInformation;

/**
 * MBean interface for the management of a JPPF driver.
 * @author Laurent Cohen
 */
public interface JPPFDriverAdminMBean extends JPPFAdminMBean
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
	Collection<JPPFManagementInfo> nodesInformation() throws Exception;
	/**
	 * Perform a shutdown or restart of the server.
	 * @param shutdownDelay - the delay before shutting down the server, once the command is received. 
	 * @param restartDelay - the delay before restarting, once the server is shutdown. If it is < 0, no restart occurs.
	 * @return an acknowledgement message.
	 * @throws Exception if any error occurs.
	 */
	String restartShutdown(Long shutdownDelay, Long restartDelay) throws Exception;
	/**
	 * Change the bundle size tuning settings.
	 * @param algorithm - the name opf the load-balancing algorithm to set.
	 * @param parameters - the algorithm's parameters.
	 * @return an acknowledgement or error message.
	 * @throws Exception if an error occurred while updating the settings.
	 */
	String changeLoadBalancerSettings(String algorithm, Map parameters) throws Exception;
	/**
	 * Obtain the current load-balancing settings.
	 * @return an instance of <code>LoadBalancingInformation</code>.
	 * @throws Exception if an error occurred while fetching the settings.
	 */
	LoadBalancingInformation loadBalancerInformation() throws Exception;
}
