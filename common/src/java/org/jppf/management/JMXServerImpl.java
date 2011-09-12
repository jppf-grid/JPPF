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

import java.lang.management.ManagementFactory;
import java.net.*;
import java.rmi.registry.*;
import java.util.*;

import javax.management.remote.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class is a wrapper around a JMX management server.
 * It is used essentially to hide the details of the remote management protocol used.
 * @author Laurent Cohen
 */
public class JMXServerImpl extends AbstractJMXServer
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JMXServerImpl.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Reference to the embedded RMI registry.
	 */
	private static Registry registry = null;
	/**
	 * Used to distinguish between driver and node RMI registries.
	 */
	private String namespaceSuffix = null;

	/**
	 * Initialize this JMX server with the specified namespace suffix.
	 * @param namespaceSuffix used to distinguish between driver and node RMI registries.
	 */
	public JMXServerImpl(String namespaceSuffix)
	{
		this(namespaceSuffix, new JPPFUuid(JPPFUuid.HEXADECIMAL, 32).toString());
	}

	/**
	 * Initialize this JMX server with the specified namespace suffix and uuid.
	 * @param namespaceSuffix used to distinguish between driver and node RMI registries.
	 * @param id the unique id of the driver or node holding this jmx server.
	 */
	public JMXServerImpl(String namespaceSuffix, String id)
	{
		this.namespaceSuffix = namespaceSuffix;
		this.id = id;
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
    lock.lock();
    try
    {
	    Thread.currentThread().setContextClassLoader(cl);
			server = ManagementFactory.getPlatformMBeanServer();
			TypedProperties props = JPPFConfiguration.getProperties();
			String host = NetworkUtils.getManagementHost();
			int port = locateOrCreateRegistry();
			int rmiPort = props.getInt("jppf.management.rmi.port", 12198);
			boolean found = false;
			JMXServiceURL url = null;
			while (!found)
			{
				try
				{
					InetAddress addr = InetAddress.getByName(host);
			    url = new JMXServiceURL("service:jmx:rmi://" + host + ":" + rmiPort + "/jndi/rmi://localhost:" + port + namespaceSuffix);
			    Map<String, Object> env = new HashMap<String, Object>();
			    env.put("jmx.remote.default.class.loader", cl);
			    env.put("jmx.remote.protocol.provider.class.loader", cl);
			    connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, env, server);
			    connectorServer.start();
			    found = true;
				}
				catch(Exception e)
				{
					Throwable cause = e.getCause();
					String s = cause.getMessage();
					if ((cause instanceof BindException) || ((s != null) && (s.toLowerCase().indexOf("bind") >= 0)))
					{
						if (rmiPort >= 65530) rmiPort = 1024;
						rmiPort++;
					}
					else throw e;
				}
			}
			props.setProperty("jppf.management.rmi.port", "" + rmiPort);
	    if (debugEnabled) log.debug("starting connector server with RMI registry port = " + port + " and RMI server port = " + rmiPort);
	    stopped = false;
	    if (debugEnabled) log.debug("JMXConnectorServer started at URL " + url);
    }
    finally
    {
      lock.unlock();
	    Thread.currentThread().setContextClassLoader(tmp);
    }
	}

	/**
	 * Locate an RMI registry specified by the configuration properties,
	 * or create an embedded one if it cannot be found.
	 * @return the port number to which the registry is bound.
	 * @throws Exception if the registry could be neither located nor created. 
	 */
	private static synchronized int locateOrCreateRegistry() throws Exception
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		int port = props.getInt("jppf.management.port", 11198);
		if (registry != null) return port;
    if (debugEnabled) log.debug("starting RMI registry on port " + port);
    boolean found = false;
    while (!found)
    {
    	try
    	{
    		registry = LocateRegistry.createRegistry(port);
    		found = true;
    	}
    	catch(Exception e)
    	{
    		Throwable cause = e.getCause();
				String s = cause.getMessage();
				if ((cause instanceof BindException) || ((s != null) && (s.toLowerCase().indexOf("bind") >= 0)))
				{
					if (port >= 65530) port = 1024;
					port++;
				}
    		else throw e;
    	}
    }
    props.setProperty("jppf.management.port", "" + port);
		return port;
	}
}
