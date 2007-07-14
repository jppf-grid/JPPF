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
	public static final Properties NODE_1 = buildNodeConfig("localhost", 11111, 11113, 1);
	/**
	 * Node configuration 2.
	 */
	public static final Properties NODE_2 = buildNodeConfig("localhost", 11111, 11113, 1);
	/**
	 * Node configuration 3.
	 */
	public static final Properties NODE_3 = buildNodeConfig("localhost", 11121, 11123, 1);

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
		props.setProperty("processing.threads", "" + nbThreads);
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
