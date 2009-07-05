/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
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
 * Node-specific connection wrapper, implementing a user-friendly interface for the monitoring
 * and management of the node.
 * @author Laurent Cohen
 */
public class JMXDriverConnectionWrapper extends JMXConnectionWrapper implements JPPFDriverAdminMBean
{
	/**
	 * Signature of the method invoked on the MBean.
	 */
	public static final String[] MBEAN_SIGNATURE = new String[] {Map.class.getName()};

	/**
	 * Initialize the connection to the remote MBean server.
	 * @param host the host the server is running on.
	 * @param port the RMI port used by the server.
	 */
	public JMXDriverConnectionWrapper(String host, int port)
	{
		super(host, port, JPPFAdminMBean.DRIVER_SUFFIX);
	}

	/**
	 * Request the JMX connection information for all the nodes attached to the server.
	 * @return a collection of <code>NodeManagementInfo</code> instances.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.management.JPPFDriverAdminMBean#nodesInformation()
	 */
	public Collection<NodeManagementInfo> nodesInformation() throws Exception
	{
		return (Collection<NodeManagementInfo>) invoke(DRIVER_MBEAN_NAME, "nodesInformation", (Object[]) null, (String[]) null);
	}

	/**
	 * Get the latest statistics snapshot from the JPPF driver.
	 * @return a <code>JPPFStats</code> instance.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.management.JPPFDriverAdminMBean#statistics()
	 */
	public JPPFStats statistics() throws Exception
	{
    return (JPPFStats) invoke(DRIVER_MBEAN_NAME, "statistics", (Object[]) null, (String[]) null);
	}

	/**
	 * Perform a shutdown or restart of the server.
	 * @param shutdownDelay - the delay before shutting down the server, once the command is received. 
	 * @param restartDelay - the delay before restarting, once the server is shutdown. If it is < 0, no restart occurs.
	 * @return an acknowledgement message.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.management.JPPFDriverAdminMBean#restartShutdown(java.lang.Long, java.lang.Long)
	 */
	public String restartShutdown(Long shutdownDelay, Long restartDelay) throws Exception
	{
		return (String) invoke(DRIVER_MBEAN_NAME, "restartShutdown",
			new Object[] {shutdownDelay, restartDelay}, new String[] {Long.class.getName(), Long.class.getName()});
	}

	/**
	 * Change the bundle size tuning settings.
	 * @param algorithm - the name opf the load-balancing algorithm to set.
	 * @param parameters - the algorithm's parameters.
	 * @return an acknowledgement or error message.
	 * @throws Exception if an error occurred while updating the settings.
	 * @see org.jppf.management.JPPFDriverAdminMBean#changeLoadBalancerSettings(java.lang.String, java.util.Map)
	 */
	public String changeLoadBalancerSettings(String algorithm, Map parameters) throws Exception
	{
		return (String) invoke(DRIVER_MBEAN_NAME, "changeLoadBalancerSettings",
			new Object[] {algorithm, parameters}, new String[] {String.class.getName(), Map.class.getName()});
	}

	/**
	 * Obtain the current load-balancing settings.
	 * @return an instance of <code>LoadBalancingInformation</code>.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.management.JPPFDriverAdminMBean#loadBalancerInformation()
	 */
	public LoadBalancingInformation loadBalancerInformation() throws Exception
	{
		return (LoadBalancingInformation) invoke(DRIVER_MBEAN_NAME, "loadBalancerInformation", (Object[]) null, (String[]) null);
	}
}
