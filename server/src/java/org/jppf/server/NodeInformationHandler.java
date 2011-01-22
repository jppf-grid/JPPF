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
	}

	/**
	 * Remove a node information object from the map of node information.
	 * @param channel a <code>SocketChannel</code> instance.
	 */
	public void removeNodeInformation(ChannelWrapper channel)
	{
		synchronized (nodeInfo)
		{
			nodeInfo.remove(channel);
		}
	}

	/**
	 * Remove a node information object from the map of node information.
	 * @return channel a <code>SocketChannel</code> instance.
	 */
	public Map<ChannelWrapper<?>, JPPFManagementInfo> getNodeInformationMap()
	{
		synchronized (nodeInfo)
		{
			return Collections.unmodifiableMap(nodeInfo);
		}
	}

	/**
	 * Get the system information for the specified node.
	 * @param channel the node for which ot get the information.
	 * @return a <code>NodeManagementInfo</code> instance, or null if no informaiton is recorded for the node.
	 */
	public JPPFManagementInfo getNodeInformation(ChannelWrapper channel)
	{
		synchronized (nodeInfo)
		{
			return nodeInfo.get(channel);
		}
	}
}
