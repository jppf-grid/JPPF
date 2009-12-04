/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.ui.monitoring.data;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.management.*;

/**
 * Instances of this class hold and manage the state of the nodes attached to a specific JPPF driver.
 * @author Laurent Cohen
 */
public class NodeInfoManager
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeInfoManager.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The name of the driver.
	 */
	private String driverName = null;
	/**
	 * Map of node jmx connections to corresponding node manager.
	 */
	private Map<NodeManagementInfo, NodeInfoHolder> nodeMap = new TreeMap<NodeManagementInfo, NodeInfoHolder>();

	/**
	 * Initialize this node information manager witht he specified driver.
	 * @param driverName the name of the JPPF driver.
	 */
	public NodeInfoManager(String driverName)
	{
		this.driverName = driverName;
	}

	/**
	 * Add a new node to this node handler.
	 * @param nodeInfo holds the information to connect to the node's JMX server.
	 */
	public synchronized void addNode(NodeManagementInfo nodeInfo)
	{
		NodeInfoHolder mgr = new NodeInfoHolder(new JPPFNodeState(),
			new JMXNodeConnectionWrapper(nodeInfo.getHost(), nodeInfo.getPort()));
		nodeMap.put(nodeInfo, mgr);
	}

	/**
	 * Remove a node from this node handler.
	 * @param nodeInfo holds the conenction information for the node to remove.
	 */
	public synchronized void removeNode(NodeManagementInfo nodeInfo)
	{
		NodeInfoHolder mgr = nodeMap.get(nodeInfo);
		if (mgr == null) return;
		if (mgr.isActive()) disconnectNode(mgr);
		nodeMap.remove(nodeInfo);
	}

	/**
	 * Activate automatic refresh of the node state.
	 * This method effectively establishes the connection with the node's JMX server.
	 * @param nodeInfo holds the information to connect to the node's JMX server.
	 */
	public synchronized void activateNode(NodeManagementInfo nodeInfo)
	{
		NodeInfoHolder mgr = nodeMap.get(nodeInfo);
		if (mgr == null) return;
		mgr.getJmxClient().connect();
	}

	/**
	 * De-activate automatic refresh of the node state.
	 * This method effectively disconnects from the node's JMX server.
	 * @param nodeInfo holds the information to connect to the node's JMX server.
	 */
	public synchronized void deactivateNode(NodeManagementInfo nodeInfo)
	{
		NodeInfoHolder mgr = nodeMap.get(nodeInfo);
		if ((mgr == null) || !mgr.isActive()) return;
		disconnectNode(mgr);
	}

	/**
	 * Determine whether this node manager contains the specified node.
	 * @param nodeInfo the node to find.
	 * @return true if the node exists in this node manager, false otherwise.
	 */
	public synchronized boolean hasNode(NodeManagementInfo nodeInfo)
	{
		return nodeMap.containsKey(nodeInfo);
	}

	/**
	 * Close the JMX connection to a node.
	 * @param mgr a <code>NodeManager</code> instance.
	 */
	private void disconnectNode(NodeInfoHolder mgr)
	{
		try
		{
			mgr.getJmxClient().close();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Get the map of node jmx connections to corresponding node manager.
	 * @return a map of <code>NodeManagementInfo</code> to <code>NodeInfoHolder</code> instances.
	 */
	public Map<NodeManagementInfo, NodeInfoHolder> getNodeMap()
	{
		return nodeMap;
	}

	/**
	 * Get the driver name.
	 * @return the name of the JPPF driver as a string.
	 */
	public synchronized String getDriverName()
	{
		return driverName;
	}

	/**
	 * Determine whether this object is equal to another.
	 * @param obj the object to compare with.
	 * @return true if the 2 objects are equal, false otherwise.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!(obj instanceof NodeInfoManager)) return false;
		NodeInfoManager other = (NodeInfoManager) obj;
		if (driverName == null) return other.getDriverName() == null;
		return driverName.equals(other.getDriverName());
	}

	/**
	 * Compute this object's hashcode.
	 * @return the hascode as an int.
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return 53 + ((driverName == null) ? 0 : driverName.hashCode());
	}
}
