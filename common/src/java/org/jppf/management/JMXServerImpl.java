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

import java.net.InetAddress;
import java.rmi.registry.*;
import java.util.*;

import javax.management.*;
import javax.management.remote.*;

import org.apache.commons.logging.*;
import org.jppf.management.spi.JPPFMBeanProviderManager;
import org.jppf.node.JPPFClassLoader;
import org.jppf.utils.*;

/**
 * This class is a wrapper around a JMX management server.
 * It is used essentially to hide the details of the remote management protocol used.
 * @author Laurent Cohen
 */
public class JMXServerImpl
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JMXServerImpl.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
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
	private boolean stopped = true;
	/**
	 * This server's unique id.
	 */
	private String id = new JPPFUuid(JPPFUuid.ALPHA_NUM, 24).toString();
	/**
	 * Used to distinguish between driver and node RMI registries.
	 */
	private String namespaceSuffix = null;
	/**
	 * Manager for the MBean defined through the service provider interface.
	 */
	private JPPFMBeanProviderManager providerManager = new JPPFMBeanProviderManager();
	/**
	 * Keeps a list of MBeans registered with the MBean server.
	 */
	private List<String> registeredMBeanNames = new Vector<String>();

	/**
	 * Initialize this JMX server with the specified namespace suffix.
	 * @param namespaceSuffix used to distinguish between driver and node RMI registries.
	 */
	public JMXServerImpl(String namespaceSuffix)
	{
		this.namespaceSuffix = namespaceSuffix;
	}

	/**
	 * Start the MBean server and associated resources.
	 * @param cl - the default classloader to be used by the JMX remote connector.
	 * @throws Exception if an error occurs when starting the server or one of its components. 
	 */
	public void start(ClassLoader cl) throws Exception
	{
    if (debugEnabled) log.debug("starting remote connector server");
    ClassLoader tmp = Thread.currentThread().getContextClassLoader();
    try
    {
	    Thread.currentThread().setContextClassLoader(cl);
			//server = ManagementFactory.getPlatformMBeanServer();
			server = MBeanServerFactory.newMBeanServer();
			if (cl instanceof JPPFClassLoader) server.registerMBean(cl, new ObjectName("org.jppf:name=NodeClassLoader"));
	    locateOrCreateRegistry();
			TypedProperties props = JPPFConfiguration.getProperties();
			String host = NetworkUtils.getManagementHost();
			int port = props.getInt("jppf.management.port", 11198);
			int rmiPort = props.getInt("jppf.management.rmi.port", 12198);
	    if (debugEnabled) log.debug("starting connector server with RMI registry port = " + port + " and RMI server port = " + rmiPort);
			InetAddress addr = InetAddress.getByName(host);
	    JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://localhost:" + rmiPort + "/jndi/rmi://" + host + ":" + port + "/jppf" + namespaceSuffix);
	    Map<String, Object> env = new HashMap<String, Object>();
	    env.put("jmx.remote.default.class.loader", cl);
	    env.put("jmx.remote.protocol.provider.class.loader", cl);
	    connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, env, server);
	    //connectorServer.
	    connectorServer.start();
	    stopped = false;
	    if (debugEnabled) log.debug("JMXConnectorServer started at URL " + url);
    }
    finally
    {
	    Thread.currentThread().setContextClassLoader(tmp);
    }
	}

	/**
	 * Register a management bean with the MBean server.
	 * @param name the name of the bean to register.
	 * @param mbean the management bean instance.
	 * @param mbeanInterface the exposed interface of the management bean.
	 * @throws Exception if the registration failed.
	 */
	public void registerMBean(String name, Object mbean, Class mbeanInterface) throws Exception
	{
		StandardMBean stdBean = new StandardMBean(mbean, mbeanInterface);
    server.registerMBean(stdBean, new ObjectName(name));
    registeredMBeanNames.add(name);
	}

	/**
	 * Un register a management bean from the MBean server.
	 * @param name the name of the bean to register.
	 * @throws Exception if the de-registration failed.
	 */
	public void unregisterMBean(String name) throws Exception
	{
    server.unregisterMBean(new ObjectName(name));
    registeredMBeanNames.remove(name);
	}

	/**
	 * Stop the MBean server and associated resources.
	 * @throws Exception if an error occurs when stopping the server or one of its components. 
	 */
	public void stop() throws Exception
	{
		stopped = true;
    connectorServer.stop();
    List<String> names = new ArrayList<String>(registeredMBeanNames);
    for (String name: names) unregisterMBean(name);
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
	 * Locate an RMI registry specified by the configuration properties,
	 * or create an embedded one if it cannot be found.
	 * @throws Exception if the registry could be neither located nor created. 
	 */
	private static synchronized void locateOrCreateRegistry() throws Exception
	{
		if (registry != null) return;
    if (debugEnabled) log.debug("starting RMI registry ");
		TypedProperties props = JPPFConfiguration.getProperties();
		int port = props.getInt("jppf.management.port", 11198);
    if (debugEnabled) log.debug("starting RMI registry on port " + port);
		registry = LocateRegistry.createRegistry(port);
	}

	/**
	 * Get a unique identifier for this management server. This id must be unique accross JPPF nodes and servers,
	 * and is used to identify this server if multiple nodes or servers share the same RMI registry.
	 * @return the id as a string.
	 */
	public String getId()
	{
		return id;
	}
}
