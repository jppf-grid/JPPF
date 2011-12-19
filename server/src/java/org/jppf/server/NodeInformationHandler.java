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

package org.jppf.server;

import java.util.*;

import org.jppf.management.JPPFManagementInfo;
import org.jppf.server.nio.ChannelWrapper;

/**
 * Instances of this class manage information on the nodes obtained via JMX.
 * @author Laurent Cohen
 */
public class NodeInformationHandler
{
	/**
	 * A list of objects containing the information required to connect to the nodes JMX servers.
	 */
	private Map<ChannelWrapper<?>, JPPFManagementInfo> nodeInfo = new HashMap<ChannelWrapper<?>, JPPFManagementInfo>();
	/**
	 * A list of objects containing the information required to connect to the nodes JMX servers.
	 */
	private Map<String, JPPFManagementInfo> uuidMap = new HashMap<String, JPPFManagementInfo>();

	/**
	 * Add a node information object to the map of node information.
	 * @param channel a <code>SocketChannel</code> instance.
	 * @param info a <code>JPPFNodeManagementInformation</code> instance.
	 */
	public void addNodeInformation(ChannelWrapper<?> channel, JPPFManagementInfo info)
	{
		synchronized (nodeInfo)
		{
			nodeInfo.put(channel, info);
		}
		synchronized (uuidMap)
		{
			uuidMap.put(info.getId(), info);
		}
	}

	/**
	 * Remove a node information object from the map of node information.
	 * @param channel a <code>SocketChannel</code> instance.
	 */
	public void removeNodeInformation(ChannelWrapper<?> channel)
	{
		JPPFManagementInfo info = null;
		synchronized (nodeInfo)
		{
			info = nodeInfo.remove(channel);
		}
		synchronized (uuidMap)
		{
			if (info != null) uuidMap.remove(info.getId());
		}
	}

	/**
	 * Get the mapping of channels to corresponding node information.
	 * @return channel a map of <code>ChannelWrapper</code> keys to <code>JPPFManagementInfo</code> values.
	 */
	public Map<ChannelWrapper<?>, JPPFManagementInfo> getNodeInformationMap()
	{
		synchronized (nodeInfo)
		{
			return Collections.unmodifiableMap(nodeInfo);
		}
	}

	/**
	 * Get the mapping of node uuid to corresponding node information.
	 * @return channel a map of <code>String</code> keys to <code>JPPFManagementInfo</code> values.
	 */
	public Map<String, JPPFManagementInfo> getUuidMap()
	{
		synchronized (uuidMap)
		{
			return Collections.unmodifiableMap(uuidMap);
		}
	}

	/**
	 * Get the system information for the specified node.
	 * @param channel the node for which to get the information.
	 * @return a <code>JPPFManagementInfo</code> instance, or null if no information is recorded for the node.
	 */
	public JPPFManagementInfo getNodeInformation(ChannelWrapper<?> channel)
	{
		synchronized (nodeInfo)
		{
			return nodeInfo.get(channel);
		}
	}

	/**
	 * Get the system information for the specified node uuid.
	 * @param uuid the uuid of the node for which to get the information.
	 * @return a <code>JPPFManagementInfo</code> instance, or null if no information is recorded for the node.
	 */
	public JPPFManagementInfo getNodeInformation(String uuid)
	{
		synchronized (uuidMap)
		{
			return uuidMap.get(uuid);
		}
	}


	/**
	 * The uuids of all the currently connected nodes.
	 * @return a set of uuids for all the nodes connected to the driver.
	 */
	public Set<String> getNodeUuids()
	{
		synchronized (uuidMap)
		{
			return Collections.unmodifiableSet(uuidMap.keySet());
		}
	}
}
