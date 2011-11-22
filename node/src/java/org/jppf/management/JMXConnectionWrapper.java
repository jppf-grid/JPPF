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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.concurrent.atomic.*;

import javax.management.*;
import javax.management.remote.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Wrapper around a JMX connection, providing a thread-safe way of handling disconnections and recovery.
 * @author Laurent Cohen
 */
public class JMXConnectionWrapper extends ThreadSynchronization
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(JMXConnectionWrapper.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * URL of the MBean server, in a JMX-compliant format.
	 */
	final JMXServiceURL url;
	/**
	 * The JMX client.
	 */
	private JMXConnector jmxc = null;
	/**
	 * A connection to the MBean server.
	 */
	private AtomicReference<MBeanServerConnection> mbeanConnection = new AtomicReference<MBeanServerConnection>(null);
	/**
	 * The host the server is running on.
	 */
	private String host = null;
	/**
	 * The RMI port used by the server.
	 */
	private int port = 0;
	/**
	 * The connection thread that performs the connection to the management server.
	 */
	private AtomicReference<JMXConnectionThread> connectionThread = new AtomicReference<JMXConnectionThread>(null);
	/**
	 * A string representing this connection, used for logging purposes.
	 */
	private String idString = null;
  /**
   * Determines whether the connection to the JMX server has been established.
   */
  private AtomicBoolean connected = new AtomicBoolean(false);
  /**
   * Determines whether the connection to the JMX server has been established.
   */
  protected boolean local;

	/**
	 * Initialize a local connection (same JVM) to the MBean server.
	 */
	public JMXConnectionWrapper()
	{
		local = true;
		url = null;
	}

	/**
	 * Initialize the connection to the remote MBean server.
	 * @param host the host the server is running on.
	 * @param port the RMI port used by the server.
	 * @param rmiSuffix	RMI registry namespace suffix. 
	 */
	public JMXConnectionWrapper(String host, int port, String rmiSuffix)
	{
		this.host = host;
		this.port = port;

		JMXServiceURL tmp = null;
		try
		{
			//String s = NetworkUtils.getHostName(host);
			//idString = (s == null ? "_" : s) + ":" + port;
			idString = host + ":" + port;
			tmp = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + port + rmiSuffix);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		url = tmp;
		local = false;
	}

	/**
	 * Initialize the connection to the remote MBean server.
	 */
	public void connect()
	{
		if (local)
		{
			mbeanConnection.set(ManagementFactory.getPlatformMBeanServer());
			setConnectedStatus(true);
		}
		else
		{
			connectionThread.set(new JMXConnectionThread(this));
			Thread t = new Thread(connectionThread.get(), "JMX connection " + getId());
			t.setDaemon(true);
			t.start();
		}
	}

	/**
	 * Initiate the connection and wait until the connection is established or the timeout has expired, whichever comes first.
	 * @param timeout the maximum time to wait for, a value of zero means no timeout and
	 * this method just waits until the connection is established.
	 */
	public void connectAndWait(long timeout)
	{
		if (isConnected()) return;
		connect();
		if (isConnected()) return;
		goToSleep(timeout);
	}

	/**
	 * Initialize the connection to the remote MBean server.
	 * @throws Exception if the connection could not be established.
	 */
	synchronized void performConnection() throws Exception
	{
  	connected.set(false);
    HashMap<String, ?> env = new HashMap<String, Object>(); 
    jmxc = JMXConnectorFactory.connect(url, env);
  	mbeanConnection.set(jmxc.getMBeanServerConnection());
  	setConnectedStatus(true);
		if (debugEnabled) log.debug(getId() + " JMX connection successfully established");
	}

	/**
	 * Close the connection to the remote MBean server.
	 * @throws Exception if the connection could not be closed.
	 */
	public void close() throws Exception
	{
		if (connectionThread.get() != null) connectionThread.get().close();
    if (jmxc != null) jmxc.close();
	}

	/**
	 * Invoke a method on the specified MBean.
	 * @param name the name of the MBean.
	 * @param methodName the name of the method to invoke.
	 * @param params the method parameter values.
	 * @param signature the types of the method parameters.
	 * @return an object or null.
	 * @throws Exception if the invocation failed.
	 */
	public synchronized Object invoke(String name, String methodName, Object[] params, String[] signature) throws Exception
	{
		if ((connectionThread.get() != null) && connectionThread.get().isConnecting()) return null;
		Object result = null;
		try
		{
	    ObjectName mbeanName = new ObjectName(name);
  		result = getMbeanConnection().invoke(mbeanName, methodName, params, signature);
		}
		catch(IOException e)
		{
			setConnectedStatus(false);
			try
			{
		    if (jmxc != null) jmxc.close();
			}
			catch(Exception e2)
			{
				if (debugEnabled) log.debug(e2.getMessage(), e2);
			}
			if (!connectionThread.get().isConnecting()) connectionThread.get().resume();
			if (debugEnabled) log.debug(getId() + " : error while invoking the JMX connection", e);
		}
		return result;
	}

	/**
	 * Get the value of an attribute of the specified MBean.
	 * @param name the name of the MBean.
	 * @param attribute the name of the attribute to read.
	 * @return an object or null.
	 * @throws Exception if the invocation failed.
	 */
	public synchronized Object getAttribute(String name, String attribute) throws Exception
	{
		if ((connectionThread.get() != null) && connectionThread.get().isConnecting()) return null;
		Object result = null;
		try
		{
	    ObjectName mbeanName = new ObjectName(name);
  		result = getMbeanConnection().getAttribute(mbeanName, attribute);
		}
		catch(IOException e)
		{
			setConnectedStatus(false);
			try
			{
		    if (jmxc != null) jmxc.close();
			}
			catch(Exception e2)
			{
				if (debugEnabled) log.debug(e2.getMessage(), e2);
			}
			if (!connectionThread.get().isConnecting()) connectionThread.get().resume();
			if (debugEnabled) log.debug(getId() + " : error while invoking the JMX connection", e);
		}
		return result;
	}

	/**
	 * Get the host the server is running on.
	 * @return the host as a string.
	 */
	public String getHost()
	{
		return host;
	}

	/**
	 * Get the RMI port used by the server.
	 * @return the port as an int.
	 */
	public int getPort()
	{
		return port;
	}

	/**
	 * Get a string describing this connection.
	 * @return a string in the format host:port.
	 */
	public String getId()
	{
		return idString;
	}

	/**
	 * Get the connection to the MBean server.
	 * @return a <code>MBeanServerConnection</code> instance.
	 */
	public MBeanServerConnection getMbeanConnection()
	{
		return mbeanConnection.get();
	}

	/**
	 * Set the connected state of this conenction wrapper.
	 * @param status true if the jmx connection is established, false otherwise.
	 */
	protected void setConnectedStatus(boolean status)
	{
  	connected.set(status);
	}

	/**
   * Determines whether the connection to the JMX server has been established.
	 * @return true if the connection is established, false otherwise.
	 */
	public boolean isConnected()
	{
		return connected.get();
	}

	/**
	 * Obtain a proxy to the soecified remote MBean.
	 * @param <T> the type of the MBean (must be an interface).
	 * @param name the name of the mbean to retrieve.
	 * @param inf the class of the MBean interface.
	 * @return an instance of the specified proxy interface.
	 * @throws Exception if any error occurs.
	 */
	public <T> T getProxy(String name, Class<T> inf) throws Exception
	{
	  return getProxy(new ObjectName(name), inf);
	}
	
	/**
	 * Obtain a proxy to the soecified remote MBean.
	 * @param <T> the type of the MBean (must be an interface).
	 * @param objectName the name of the mbean to retrieve.
	 * @param inf the class of the MBean interface.
	 * @return an instance of the specified proxy interface.
	 * @throws Exception if any error occurs.
	 */
	public <T> T getProxy(ObjectName objectName, Class<T> inf) throws Exception
	{
		// if the connection is not yet established, then connect
		if (!isConnected()) connectAndWait(5000L);
		// obtain a connection to the remote MBean server
	  MBeanServerConnection mbsc = getMbeanConnection();
	  // finally obtain and return a proxy to the specified remote MBean
	  return (T) MBeanServerInvocationHandler.newProxyInstance(mbsc, objectName, inf, true);
	}
}
