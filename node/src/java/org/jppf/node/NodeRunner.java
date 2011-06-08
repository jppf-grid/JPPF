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
package org.jppf.node;

import java.security.*;
import java.util.Hashtable;
import java.util.concurrent.*;

import org.jppf.*;
import org.jppf.classloader.*;
import org.jppf.comm.discovery.*;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.logging.jmx.JmxMessageNotifier;
import org.jppf.process.LauncherListener;
import org.jppf.security.JPPFPolicy;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Bootstrap class for lauching a JPPF node. The node class is dynamically loaded from a remote server.
 * @author Laurent Cohen
 */
public class NodeRunner
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(NodeRunner.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The ClassLoader used for loading the classes of the framework.
	 */
	private static AbstractJPPFClassLoader classLoader = null;
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
	 * The JPPF node.
	 */
	private static MonitoredNode node = null;
	/**
	 * Used to synchronize start and stop methods when the node is run as a service.
	 */
	private static SimpleObjectLock serviceLock = new SimpleObjectLock();
	/**
	 * This node's universal identifier.
	 */
	private static String uuid = new JPPFUuid(JPPFUuid.HEXADECIMAL, 32).toString().toUpperCase();
	/**
	 * Handles include and exclude IP filters.
	 */
	private static IPFilter ipFilter = new IPFilter(JPPFConfiguration.getProperties());

	/**
	 * Run a node as a standalone application.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		node = null;
		try
		{
			// initialize the jmx logger
			new JmxMessageNotifier();
			if (debugEnabled) log.debug("launching the JPPF node");
			if ((args == null) || (args.length <= 0))
				throw new JPPFException("The node should be run with an argument representing a valid TCP port or 'noLauncher'");
			if (!"noLauncher".equals(args[0]))
			{
				int port = Integer.parseInt(args[0]);
				new LauncherListener(port).start();
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
			System.exit(1);
		}
		try
		{
			int pid = SystemUtils.getPID();
			if (pid > 0) System.out.println("node process id: " + pid);
			log.info("starting node, uuid=" + uuid);
			// to ensure VersionUtils is loaded by the same class loader as this class.
			VersionUtils.getBuildNumber();
			while (true)
			{
				try
				{
					node = createNode();
					node.run();
				}
				catch(JPPFNodeReconnectionNotification e)
				{
					if (debugEnabled) log.debug("received reconnection notification");
					if (classLoader != null) classLoader.close();
					classLoader = null;
					if (node != null) node.stopNode(true);
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
	 * Run a node as a standalone application.
	 * @param args not used.
	 */
	public static void start(String...args)
	{
		main(args);
		serviceLock.goToSleep();
	}

	/**
	 * Run a node as a standalone application.
	 * @param args not used.
	 */
	public static void stop(String...args)
	{
		serviceLock.wakeUp();
		System.exit(0);
	}

		/**
	 * Start the node.
	 * @return the node that was started, as a <code>MonitoredNode</code> instance.
	 * @throws Exception if the node failed to run or couldn't connect to the server.
	 */
	public static MonitoredNode createNode() throws Exception
	{
		if (JPPFConfiguration.getProperties().getBoolean("jppf.discovery.enabled", true)) discoverDriver();
		setSecurity();
		String className = "org.jppf.server.node.remote.JPPFRemoteNode";
		Class clazz = getJPPFClassLoader().loadClass(className);
		MonitoredNode node = (MonitoredNode) clazz.newInstance();
		if (debugEnabled) log.debug("Created new node instance: " + node);
		node.setSocketWrapper(nodeSocket);
		return node;
	}

	/**
	 * Automatically discover the server connection information using a datagram multicast.
	 * Upon receiving the connection information, the JPPF configuration is modified to take into
	 * account the discovered information. If no information could be received, the node relies on
	 * the static information in the configuration file. 
	 */
	public static void discoverDriver()
	{
		JPPFMulticastReceiver receiver = new JPPFMulticastReceiver(ipFilter);
		JPPFConnectionInformation info = receiver.receive();
		receiver.setStopped(true);
		if (info == null)
		{
			if (debugEnabled) log.debug("Could not auto-discover the driver connection information");
			return;
		}
		if (debugEnabled) log.debug("Discovered driver: " + info);
		TypedProperties config = JPPFConfiguration.getProperties();
		config.setProperty("jppf.server.host", info.host);
		config.setProperty("class.server.port", StringUtils.buildString(info.classServerPorts));
		config.setProperty("node.server.port", StringUtils.buildString(info.nodeServerPorts));
		if (info.managementHost != null) config.setProperty("jppf.management.host", info.managementHost);
		if (info.recoveryPort >= 0)
		{
			config.setProperty("jppf.recovery.server.port", "" + info.recoveryPort);
			//config.setProperty("jppf.recovery.enabled", "true");
		}
		else config.setProperty("jppf.recovery.enabled", "false");
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
				if (debugEnabled) log.debug("setting security");
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
			if (debugEnabled) log.debug("un-setting security");
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
	 * @return a <code>AbstractJPPFClassLoader</code> used for loading the classes of the framework.
	 */
	public static AbstractJPPFClassLoader getJPPFClassLoader()
	{
		synchronized(JPPFClassLoader.class)
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
	 * Get the JPPF node.
	 * @return a <code>MonitoredNode</code> instance.
	 */
	public static MonitoredNode getNode()
	{
		return node;
	}

	/**
	 * Shutdown and eventually restart the node.
	 * @param node the node to shutdown or restart.
	 * @param restart determines whether this node should be restarted by the node launcher.
	 */
	public static void shutdown(MonitoredNode node, final boolean restart)
	{
		//node.stopNode(true);
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

	/**
	 * This node's universal identifier.
	 * @return a uuid as a string.
	 */
	public static String getUuid()
	{
		return uuid;
	}
}
