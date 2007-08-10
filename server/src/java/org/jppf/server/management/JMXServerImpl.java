/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

package org.jppf.server.management;

import java.rmi.registry.*;

import javax.management.*;
import javax.management.remote.*;

import org.jppf.management.JPPFDriverAdminMBean;
import org.jppf.utils.*;

/**
 * This class is a wrapper around a JMX management server.
 * It is used essentially to hide the details of the remote management protocol used.
 * @author Laurent Cohen
 */
public class JMXServerImpl
{
	/**
	 * Reference to the embedded RMI registry.
	 */
	private Registry registry = null;
	/**
	 * The mbean server.
	 */
	private MBeanServer server = null;
	/**
	 * The JMX connector server.
	 */
	private JMXConnectorServer connectorServer = null;

	/**
	 * Start the MBean server and associated resources.
	 * @throws Exception if an error occurs when starting the server or one of its components. 
	 */
	public void start() throws Exception
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		String host = props.getProperty("jppf.management.host", "localhost");
		int port = props.getInt("jppf.management.port", 11198);
    server = MBeanServerFactory.createMBeanServer();
    registry = LocateRegistry.createRegistry(port);
    JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"+host+":"+port+"/server");
    connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, null, server);
    connectorServer.start();
 
    StandardMBean statsBean = new StandardMBean(new JPPFDriverAdmin(), JPPFDriverAdminMBean.class);
    server.registerMBean(statsBean, new ObjectName("org.jppf:name=admin,type=driver"));
	}

	/**
	 * Stop the MBean server and associated resources.
	 * @throws Exception if an error occurs when stopping the server or one of its components. 
	 */
	public void stop() throws Exception
	{
    connectorServer.stop();
    //registry = LocateRegistry.createRegistry(11199);
	}

	/**
	 * Get a reference to the MBean server.
	 * @return an <code>MBeanServer</code> instance.
	 */
	public MBeanServer getServer()
	{
		return server;
	}
}
