/*
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

import java.security.*;
import java.util.Hashtable;

import org.apache.commons.logging.*;
import org.jppf.JPPFNodeReloadNotification;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.security.JPPFPolicy;
import org.jppf.utils.*;

/**
 * Bootstrap class for lauching a JPPF node. The node class is dynamically loaded from a remote server.
 * @author Laurent Cohen
 */
public class NodeLauncher
{
	/**
	 * Log4j logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeLauncher.class);
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
	 * Run a node as a standalone application.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		MonitoredNode node = null;
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
					nodeSocket = node.getSocketWrapper();
					System.out.println(notif.getMessage());
					System.out.println("Reloading this node");
					classLoader = null;
					node.stopNode(false);
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
				Policy.setPolicy(new JPPFPolicy(getJPPFClassLoader()));
				System.setSecurityManager(new SecurityManager());
				securityManagerSet = true;
			}
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
					JPPFClassLoader cl = new JPPFClassLoader(NodeLauncher.class.getClassLoader());
					return cl;
				}
			});
			Thread.currentThread().setContextClassLoader(classLoader);
		}
		return classLoader;
	}

	/**
	 * Set a persistent object with the specified key.
	 * @param key the key used to retrieve the object.
	 * @param value the object to persist.
	 */
	public static void setPersistentData(Object key, Object value)
	{
		persistentData.put(key, value);
	}

	/**
	 * Get a persistent object given its key.
	 * @param key the used to find a persistent object.
	 * @return the value associated with the key.
	 */
	public static Object getPersistentData(Object key)
	{
		return persistentData.get(key);
	}
}
