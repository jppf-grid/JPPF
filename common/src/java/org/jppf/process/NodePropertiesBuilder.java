/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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

package org.jppf.process;

import java.util.*;

/**
 * 
 * @author Laurent Cohen
 */
public class NodePropertiesBuilder
{
	/**
	 * Node configuration 1.
	 */
	public static final Properties NODE_1 = buildNodeConfig("localhost", 11111, 11112, 1);
	/**
	 * Node configuration 2.
	 */
	public static final Properties NODE_2 = buildNodeConfig("localhost", 11111, 11112, 1);
	/**
	 * Node configuration 3.
	 */
	public static final Properties NODE_3 = buildNodeConfig("localhost", 11121, 11122, 1);

	/**
	 * Generate the base configuration properties for a node.
	 * @param host the host on which the driver is running.
	 * @param classPort the port number for the class server.
	 * @param nodePort the port number for the class server.
	 * @param nbThreads number of worker threads run by the node.
	 * @return a <code>Properties</code> instance.
	 */
	public static Properties buildNodeConfig(String host, int classPort, int nodePort, int nbThreads)
	{
		Properties props = new Properties();
		props.setProperty("jppf.server.host", "" + host);
		props.setProperty("class.server.port", "" + classPort);
		props.setProperty("node.server.port", "" + nodePort);
		props.setProperty("reconnect.initial.delay", "1");
		props.setProperty("reconnect.max.time", "-1");
		props.setProperty("reconnect.interval", "1");
		props.setProperty("max.memory.option", "128");

		return props;
	}

	/**
	 * Build the base permissions for a node.
	 * @return a list of <code>NodePermission</code> instances.
	 */
	public static List<NodePermission> buildBasePermissions()
	{
		List<NodePermission> list = new ArrayList<NodePermission>();
		list.add(new NodePermission("java.lang.RuntimePermission", "createClassLoader", null));
		list.add(new NodePermission("java.lang.RuntimePermission", "setContextClassLoader", null));
		list.add(new NodePermission("java.lang.RuntimePermission", "getClassLoader", null));
		list.add(new NodePermission("java.lang.RuntimePermission", "accessClassInPackage.*", null));
		list.add(new NodePermission("java.lang.RuntimePermission", "defineClassInPackage.*", null));
		list.add(new NodePermission("java.lang.RuntimePermission", "accessDeclaredMembers", null));
		list.add(new NodePermission("java.lang.RuntimePermission", "getStackTrace", null));
		list.add(new NodePermission("java.lang.RuntimePermission", "modifyThread", null));
		list.add(new NodePermission("java.io.FilePermission", "${user.dir}/jppf-node.log", "read,write,delete"));
		list.add(new NodePermission("java.io.FilePermission", "${user.dir}/jppf-node1.log", "read,write,delete"));
		list.add(new NodePermission("java.io.FilePermission", "${user.dir}/jppf-node2.log", "read,write,delete"));
		list.add(new NodePermission("java.io.FilePermission", "${user.dir}/config/log4j-node.properties", "read"));
		list.add(new NodePermission("java.io.FilePermission", "${user.dir}/config/log4j-node1.properties", "read"));
		list.add(new NodePermission("java.io.FilePermission", "${user.dir}/config/log4j-node2.properties", "read"));
		list.add(new NodePermission("java.util.PropertyPermission", "log4j.*", "read"));
		list.add(new NodePermission("java.util.PropertyPermission", "java.version", "read"));
		list.add(new NodePermission("java.util.PropertyPermission", "line.separator", "read"));
		list.add(new NodePermission("java.lang.RuntimePermission", "modifyThreadGroup", null));
		list.add(new NodePermission("java.lang.RuntimePermission", "shutdownHooks", null));
		list.add(new NodePermission("org.tanukisoftware.wrapper.security.WrapperPermission", "signalStarting", null));
		list.add(new NodePermission("org.tanukisoftware.wrapper.security.WrapperPermission", "signalStopped", null));
		return list;
	}

	/**
	 * Descriptor class for a security policy permission in a node.
	 */
	public static class NodePermission
	{
		/**
		 * Fully qualified class name of the type of permission.
		 * e.g. <code>java.lang.RuntimePermission</code>.
		 */
		public String permissionClass = null;
		/**
		 * Name of the permission.
		 */
		public String name = null;
		/**
		 * Eventual actions granted on the permission.
		 */
		public String actions = null;

		/**
		 * Instantiate a node permission from the specified parameters.
		 * @param permissionClass fully qualified class name of the type of permission.
		 * @param name name of the permission.
		 * @param actions eventual actions granted on the permission.
		 */
		public NodePermission(String permissionClass, String name, String actions)
		{
			this.permissionClass = permissionClass;
			this.name = name;
			this.actions = actions;
		}
	
		/**
		 * Obtain a string representation for this node permission.
		 * @return a string representing this node permission.
		 * @see java.lang.Object#toString()
		 */
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("permission ").append(permissionClass);
			sb.append(" \"").append(name).append("\"");
			if (actions != null)
			{
				sb.append(", \"").append(actions).append("\"");
			}
			sb.append(";");
			return sb.toString();
		}
	}
}
