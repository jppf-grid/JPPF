/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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
package org.jppf.node;

import java.net.Socket;
import java.security.*;
import java.util.Hashtable;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
import org.jppf.*;
import org.jppf.comm.discovery.*;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.management.*;
import org.jppf.security.JPPFPolicy;
import org.jppf.utils.*;

/**
 * Bootstrap class for lauching a JPPF node. The node class is dynamically loaded from a remote server.
 * @author Laurent Cohen
 */
public class NodeRunner
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeRunner.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The ClassLoader used for loading the classes of the framework.
	 */
	private static JPPFClassLoader classLoader = null;
	/**
	 * Determine whether a security manager has already been set.
	 */
	private static boolean securityManagerSet = false;
	/**
	 * The actual socket connection used by the node.
	 * Provided as a means to reuse it when the node updates its own code, therefore removing the need to
	 * disconnect from the server.
	 */
	private static SocketWrapper nodeSocket = null;
	/**
	 * Container for data stored at the JVM level.
	 */
	private static Hashtable<Object, Object> persistentData = new Hashtable<Object, Object>();
	/**
	 * The jmx server that handles administration and monitoring functions for this node.
	 */
	private static JMXServerImpl jmxServer = null;
	/**
	 * Used to executed a JVM termination task;
	 */
	private static ExecutorService executor = Executors.newFixedThreadPool(1);
	/**
	 * Task used to shutdown the node.
	 */
	private static final ShutdownOrRestart SHUTDOWN_TASK = new ShutdownOrRestart(false); 
	/**
	 * Task used to restart the node.
	 */
	private static final ShutdownOrRestart RESTART_TASK = new ShutdownOrRestart(true); 

	/**
	 * Run a node as a standalone application.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		MonitoredNode node = null;
		try
		{
			log.debug("launching the JPPF node");
			if ((args == null) || (args.length <= 0))
				throw new JPPFException("The node should be run with an argument representing a valid TCP port or 'noLauncher'");
			if (!"noLauncher".equals(args[0]))
			{
				int port = Integer.parseInt(args[0]);
				runLauncherListener(port);
			}
		}
		catch(Exception e)
		{
			log.fatal(e.getMessage(), e);
			System.exit(1);
		}
		try
		{
			log.info("starting node");
			// to ensure VersionUtils is loaded by the same class loader as this class.
			VersionUtils.getBuildNumber();
			while (true)
			{
				try
				{
					node = createNode();
					node.run();
				}
				catch(JPPFNodeReloadNotification notif)
				{
					if (debugEnabled) log.debug("received reload notfication");
					nodeSocket = node.getSocketWrapper();
					System.out.println(notif.getMessage());
					System.out.println("Reloading this node");
					classLoader.close();
					classLoader = null;
					node.stopNode(false);
					unsetSecurity();
				}
				catch(JPPFNodeReconnectionNotification e)
				{
					if (debugEnabled) log.debug("received reconnection notfication");
					classLoader.close();
					classLoader = null;
					node.stopNode(true);
					unsetSecurity();
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Start the node.
	 * @return the node that was started, as a <code>MonitoredNode</code> instance.
	 * @throws Exception if the node failed to run or couldn't connect to the server.
	 */
	public static MonitoredNode createNode() throws Exception
	{
		try
		{
			if (JPPFConfiguration.getProperties().getBoolean("jppf.discovery.enabled", true)) discoverDriver();
			setSecurity();
			Class clazz = getJPPFClassLoader().loadClass("org.jppf.server.node.JPPFNode");
			MonitoredNode node = (MonitoredNode) clazz.newInstance();
			node.setSocketWrapper(nodeSocket);
			return node;
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	/**
	 * Automatically discover the server connection information using a datagram multicast.
	 * Upon receiving the connection information, the JPPF configuration is modified to take into
	 * account the discovered information. If no information could be received, the node relies on
	 * the static information in the configuration file. 
	 */
	public static void discoverDriver()
	{
		JPPFMulticastReceiver receiver = new JPPFMulticastReceiver();
		JPPFConnectionInformation info = receiver.receive();
		receiver.setStopped(true);
		if (info == null)
		{
			if (debugEnabled) log.debug("Could not auto-discover the driver connection information");
			return;
		}
		TypedProperties props = JPPFConfiguration.getProperties();
		props.setProperty("jppf.server.host", info.host);
		props.setProperty("class.server.port", StringUtils.buildString(info.classServerPorts));
		props.setProperty("node.server.port", StringUtils.buildString(info.nodeServerPorts));
		if (info.managementHost != null) props.setProperty("jppf.management.host", info.managementHost);
	}

	/**
	 * Set the security manager with the permission granted in the policy file.
	 * @throws Exception if the security could not be set.
	 */
	public static void setSecurity() throws Exception
	{
		if (!securityManagerSet)
		{
			TypedProperties props = JPPFConfiguration.getProperties();
			String s = props.getString("jppf.policy.file");
			if (s != null)
			{
				//java.rmi.server.hostname
				String rmiHostName = props.getString("jppf.management.host", "localhost");
				System.setProperty("java.rmi.server.hostname", rmiHostName);
				Policy.setPolicy(new JPPFPolicy(getJPPFClassLoader()));
				System.setSecurityManager(new SecurityManager());
				securityManagerSet = true;
			}
		}
	}

	/**
	 * Set the security manager with the permission granted in the policy file.
	 */
	public static void unsetSecurity()
	{
		if (securityManagerSet)
		{
			AccessController.doPrivileged(new PrivilegedAction<Object>()
			{
				public Object run()
				{
					System.setSecurityManager(null);
					return null;
				}
			});
			securityManagerSet = false;
		}
	}

	/**
	 * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
	 * @return a <code>ClassLoader</code> used for loading the classes of the framework.
	 */
	public static JPPFClassLoader getJPPFClassLoader()
	{
		if (classLoader == null)
		{
			classLoader = AccessController.doPrivileged(new PrivilegedAction<JPPFClassLoader>()
			{
				public JPPFClassLoader run()
				{
					JPPFClassLoader cl = new JPPFClassLoader(NodeRunner.class.getClassLoader());
					return cl;
				}
			});
			Thread.currentThread().setContextClassLoader(classLoader);
		}
		return classLoader;
	}

	/**
	 * Set a persistent object with the specified key.
	 * @param key the key associated with the object's value.
	 * @param value the object to persist.
	 */
	public static synchronized void setPersistentData(Object key, Object value)
	{
		persistentData.put(key, value);
	}

	/**
	 * Get a persistent object given its key.
	 * @param key the key used to retrieve the persistent object.
	 * @return the value associated with the key.
	 */
	public static synchronized Object getPersistentData(Object key)
	{
		return persistentData.get(key);
	}

	/**
	 * Remove a persistent object.
	 * @param key the key associated with the object to remove.
	 * @return the value associated with the key, or null if the key was not found.
	 */
	public static synchronized Object removePersistentData(Object key)
	{
		return persistentData.remove(key);
	}

	/**
	 * Get the jmx server that handles administration and monitoring functions for this node.
	 * @return a <code>JMXServerImpl</code> instance.
	 */
	public static JMXServerImpl getJmxServer()
	{
		if ((jmxServer == null) || jmxServer.isStopped())
		{
			try
			{
				jmxServer = new JMXServerImpl(JPPFAdminMBean.NODE_SUFFIX);
				jmxServer.start();
			}
			catch(Exception e)
			{
				log.error("Error creating the JMX server", e);
			}
		}
		return jmxServer;
	}

	/**
	 * Listen to a socket connection setup in the Node Launcher, to handle the situation when the Launcher dies unexpectedly.<br>
	 * In that situation, the connection is broken and this node knows that it must exit.
	 * @param port the port to listen to.
	 */
	private static void runLauncherListener(final int port)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				try
				{
					Socket s = new Socket("localhost", port);
					s.getInputStream().read();
				}
				catch(Throwable t)
				{
					System.exit(0);
				}
			}
		};
		new Thread(r).start();
	}

	/**
	 * Shutdown and evenetually restart the node.
	 * @param node the node to shutdown or restart.
	 * @param restart determines whether this node should be restarted by the node launcher.
	 */
	public static void shutdown(MonitoredNode node, final boolean restart)
	{
		node.stopNode(true);
		executor.submit(restart ? RESTART_TASK : SHUTDOWN_TASK);
	}

	/**
	 * Task used to terminate the JVM.
	 */
	public static class ShutdownOrRestart implements Runnable
	{
		/**
		 * True if the node is to be restarted, false to only shut it down.
		 */
		private boolean restart = false;

		/**
		 * Initialize this task.
		 * @param restart true if the node is to be restarted, false to only shut it down.
		 */
		public ShutdownOrRestart(boolean restart)
		{
			this.restart = restart;
		}

		/**
		 * Execute this task.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			AccessController.doPrivileged(new PrivilegedAction<Object>()
			{
				public Object run()
				{
					System.exit(restart ? 2 : 0);
					return null;
				}
			});
		}
	}
}
