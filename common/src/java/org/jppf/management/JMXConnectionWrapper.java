/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

import javax.management.*;
import javax.management.remote.*;

import org.apache.commons.logging.*;
import org.jppf.utils.*;

/**
 * Wrapper around a JMX connection, providing a thread-safe way of handling disconnections and recovery.
 * @author Laurent Cohen
 */
public class JMXConnectionWrapper extends ThreadSynchronization
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JMXConnectionWrapper.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * URL of the MBean server, in a JMX-compliant format.
	 */
	private JMXServiceURL url = null;
	/**
	 * The JMX client.
	 */
	private JMXConnector jmxc = null;
	/**
	 * A connection to the MBean server.
	 */
	private MBeanServerConnection mbeanConnection = null;
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
	private JMXConnectionThread connectionThread = null;
	/**
	 * A string representing this connection, used for logging purposes.
	 */
	private String idString = null;
  /**
   * Determines whether the connection to the JMX server has been established.
   */
  private boolean connected = false;
  /**
   * Determines whether the connection to the JMX server has been established.
   */
  protected boolean local = false;

	/**
	 * Initialize a local connection (same JVM) to the MBean server.
	 */
	public JMXConnectionWrapper()
	{
		local = true;
	}

	/**
	 * Initialize the connection to the remote MBean server.
	 * @param host - the host the server is running on.
	 * @param port - the RMI port used by the server.
	 * @param rmiSuffix	- RMI registry namespace suffix. 
	 */
	public JMXConnectionWrapper(String host, int port, String rmiSuffix)
	{
		this.host = host;
		this.port = port;

		try
		{
			String s = NetworkUtils.getHostName(host);
			//idString = "[" + (host == null ? "_" : host) + ":" + port + "] ";
			idString = (s == null ? "_" : s) + ":" + port;
			url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jppf" + rmiSuffix);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		local = false;
	}

	/**
	 * Initialize the connection to the remote MBean server.
	 */
	public void connect()
	{
		if (local)
		{
			mbeanConnection = ManagementFactory.getPlatformMBeanServer();
	  	connected = true;
		}
		else
		{
			connectionThread = new JMXConnectionThread();
			Thread t = new Thread(connectionThread, "JMX connection thread for " + getId());
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
		connect();
		if (connected) return;
		goToSleep(timeout);
	}

	/**
	 * Initialize the connection to the remote MBean server.
	 * @throws Exception if the connection could not be established.
	 */
	private synchronized void performConnection() throws Exception
	{
  	connected = false;
    HashMap env = new HashMap(); 
    jmxc = JMXConnectorFactory.connect(url, env);
  	mbeanConnection = jmxc.getMBeanServerConnection();
  	connected = true;
		log.info(getId() + " RMI connection successfully established");
	}

	/**
	 * Close the connection to the remote MBean server.
	 * @throws Exception if the connection could not be closed.
	 */
	public void close() throws Exception
	{
		if (connectionThread != null) connectionThread.close();
    if (jmxc != null) jmxc.close();
	}

	/**
	 * Invoke a method on the specified MBean.
	 * @param name - the name of the MBean.
	 * @param methodName - the name of the method to invoke.
	 * @param params - the method parameter values.
	 * @param signature - the types of the method parameters.
	 * @return an object or null.
	 * @throws Exception if the invocation failed.
	 */
	public synchronized Object invoke(String name, String methodName, Object[] params, String[] signature) throws Exception
	{
		if (connectionThread.isConnecting()) return null;
		Object result = null;
		try
		{
	    ObjectName mbeanName = new ObjectName(name);
  		result = getMbeanConnection().invoke(mbeanName, methodName, params, signature);
		}
		catch(IOException e)
		{
			if (!connectionThread.isConnecting()) connectionThread.resume();
			log.info(getId() + " : " + e.getMessage(), e);
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
	public synchronized MBeanServerConnection getMbeanConnection()
	{
		return mbeanConnection;
	}

	/**
   * Determines whether the connection to the JMX server has been established.
	 * @return true if the connection is established, false otherwise.
	 */
	public synchronized boolean isConnected()
	{
		return connected;
	}

	/**
	 * This class is intended to be used as a thread that attempts to (re-)connect to
	 * the management server.
	 */
	public class JMXConnectionThread extends ThreadSynchronization implements Runnable
	{
		/**
		 * Determines the suspended state of this connection thread.
		 */
		private boolean suspended = false;
		/**
		 * Determines the connecting state of this connection thread.
		 */
		private boolean connecting = true;

		/**
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			while (!isStopped())
			{
				if (isSuspended())
				{
					if (debugEnabled) log.debug(getId() + " about to go to sleep");
					goToSleep();
					continue;
				}
				if (connecting)
				{
					try
					{
						if (debugEnabled) log.debug(getId() + " about to perform RMI connection attempts");
						performConnection();
						if (debugEnabled) log.debug(getId() + " about to suspend RMI connection attempts");
						suspend();
						wakeUp();
						JMXConnectionWrapper.this.wakeUp();
					}
					catch(Exception ignored)
					{
						if (debugEnabled) log.debug(getId()+ " JMX URL = "+url, ignored);
						try
						{
							Thread.sleep(100);
						}
						catch(InterruptedException e)
						{
							log.error(e.getMessage(), e);
						}
					}
				}
			}
		}

		/**
		 * Suspend the current thread.
		 */
		public synchronized void suspend()
		{
			if (debugEnabled) log.debug(getId() + " suspending RMI connection attempts");
			setConnecting(false);
			suspended = true;
		}

		/**
		 * Resume the current thread's execution.
		 */
		public synchronized void resume()
		{
			if (debugEnabled) log.debug(getId() + " resuming RMI connection attempts");
			setConnecting(true);
			suspended = false;
			wakeUp();
		}

		/**
		 * Stop this thread.
		 */
		public synchronized void close()
		{
			setConnecting(false);
			setStopped(true);
			wakeUp();
		}

		/**
		 * Get the connecting state of this connection thread.
		 * @return true if the connection is established, false otherwise.
		 */
		public synchronized boolean isConnecting()
		{
			return connecting;
		}

		/**
		 * Get the connecting state of this connection thread.
		 * @param connecting true if the connection is established, false otherwise.
		 */
		public synchronized void setConnecting(boolean connecting)
		{
			this.connecting = connecting;
		}

		/**
		 * Determines the suspended state of this connection thread.
		 * @return true if the thread is suspended, false otherwise. 
		 */
		public synchronized boolean isSuspended()
		{
			return suspended;
		}
	}
}
