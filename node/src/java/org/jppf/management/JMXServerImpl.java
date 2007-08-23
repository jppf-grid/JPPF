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

package org.jppf.management;

import java.rmi.registry.*;

import javax.management.*;
import javax.management.remote.*;

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
	private static Registry registry = null;
	/**
	 * The mbean server.
	 */
	private MBeanServer server = null;
	/**
	 * The JMX connector server.
	 */
	private JMXConnectorServer connectorServer = null;
	/**
	 * Determines whether this JMX server is stopped.
	 */
	private boolean stopped = false;

	/**
	 * Start the MBean server and associated resources.
	 * @throws Exception if an error occurs when starting the server or one of its components. 
	 */
	public void start() throws Exception
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		String host = props.getProperty("jppf.server.host", "localhost");
		int port = props.getInt("jppf.management.port", 11198);
    server = MBeanServerFactory.createMBeanServer();
    if (getRegistry() == null) setRegistry(LocateRegistry.createRegistry(port));
    JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"+host+":"+port+"/server");
    connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, null, server);
    connectorServer.start();
	}

	/**
	 * Register a management bean with the MBean server.
	 * @param name the name of the bean to register.
	 * @param mbean the management bean instance.
	 * @param mbeanInterface the exposed interface of the management bean.
	 * @throws Exception if the registration failed.
	 */
	public void registerMbean(String name, Object mbean, Class mbeanInterface) throws Exception
	{
		StandardMBean stdBean = new StandardMBean(mbean, mbeanInterface);
    server.registerMBean(stdBean, new ObjectName(name));
	}

	/**
	 * Un register a management bean from the MBean server.
	 * @param name the name of the bean to register.
	 * @throws Exception if the de-registration failed.
	 */
	public void unregisterMbean(String name) throws Exception
	{
    server.unregisterMBean(new ObjectName(name));
	}

	/**
	 * Stop the MBean server and associated resources.
	 * @throws Exception if an error occurs when stopping the server or one of its components. 
	 */
	public void stop() throws Exception
	{
		stopped = true;
    connectorServer.stop();
	}

	/**
	 * Get a reference to the MBean server.
	 * @return an <code>MBeanServer</code> instance.
	 */
	public MBeanServer getServer()
	{
		return server;
	}

	/**
	 * Determine whether this JMX server is stopped.
	 * @return true if this JMX server is stopped, false otherwise.
	 */
	public boolean isStopped()
	{
		return stopped;
	}

	/**
	 * Set the embedded RMI registry.
	 * @param registry a <code>Registry</code> instance.
	 */
	private static synchronized void setRegistry(Registry registry)
	{
		JMXServerImpl.registry = registry;
	}

	/**
	 * Get the embedded RMI registry.
	 * @return a <code>Registry</code> instance. 
	 */
	private static synchronized Registry getRegistry()
	{
		return registry;
	}
}
