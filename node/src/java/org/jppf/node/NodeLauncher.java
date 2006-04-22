/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.node;

import java.security.Policy;
import org.jppf.JPPFNodeReloadNotification;
import org.jppf.security.JPPFPolicy;
import org.jppf.utils.*;

/**
 * Bootstrap class for lauching a JPPF node. The node class is dynamically loaded from a remote server.
 * @author Laurent Cohen
 */
public class NodeLauncher
{
	/**
	 * The ClassLoader used for loading the classes of the framework.
	 */
	private static ClassLoader classLoader = null;
	/**
	 * Determine whether a security manager has already been set.
	 */
	private static boolean securityManagerSet = false;

	/**
	 * Run a node as a standalone application.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		MonitoredNode node = null;
		try
		{
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
					System.out.println(notif.getMessage());
					System.out.println("Reloading this node");
					classLoader = null;
					node.stopNode();
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
			return node;
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	/**
	 * Set the security manager with the permission granted in the plicy file.
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
				Policy.setPolicy(new JPPFPolicy());
				System.setSecurityManager(new SecurityManager());
				securityManagerSet = true;
			}
		}
	}

	/**
	 * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
	 * @return a <code>ClassLoader</code> used for loading the classes of the framework.
	 */
	public static ClassLoader getJPPFClassLoader()
	{
		if (classLoader == null)
		{
			classLoader = new JPPFClassLoader(NodeLauncher.class.getClassLoader());
		}
		return classLoader;
	}
}
