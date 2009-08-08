/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.ui.monitoring.data;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.management.*;
import org.jppf.ui.monitoring.event.NodeHandlerEvent;

/**
 * Instances of this class are tasks run periodically from a timer thread, requesting the latest
 * statistics form a JPPF driver connection each time they are run.
 * @author Laurent Cohen
 */
public class NodeRefreshTask extends TimerTask
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeRefreshTask.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The node handler.
	 */
	private NodeHandler handler = null;

	/**
	 * Initialize this task with the specified node handler.
	 * @param handler The node handler.
	 */
	public NodeRefreshTask(NodeHandler handler)
	{
		this.handler = handler;
	}

	/**
	 * Request an update from the JPPF driver.
	 * @see java.util.TimerTask#run()
	 */
	public void run()
	{
		synchronized(handler)
		{
			Map<String, NodeInfoManager> nodeManagerMap = handler.getNodeManagerMap();
			for (Map.Entry<String, NodeInfoManager> mgrEntry: nodeManagerMap.entrySet())
			{
				Map<NodeManagementInfo, NodeInfoHolder> nodesMap = mgrEntry.getValue().getNodeMap();
				for (Map.Entry<NodeManagementInfo, NodeInfoHolder> infoEntry: nodesMap.entrySet())
				{
					updateNodeState(mgrEntry.getKey(), infoEntry.getValue());
				}
			}
		}
	}

	/**
	 * Update the state of a node.
	 * @param driverName the name of the driver to which the node is attached.
	 * @param infoHolder the object that holds the node information and state.
	 */
	private void updateNodeState(String driverName, NodeInfoHolder infoHolder)
	{
		JPPFNodeState state = null;
		try
		{
			state = infoHolder.getJmxClient().state();
		}
		catch(Exception ignored)
		{
			if (debugEnabled) log.debug(ignored.getMessage(), ignored);
		}

		if (state == null)
		{
			handler.fireNodeHandlerEvent(driverName, infoHolder, NodeHandlerEvent.REMOVE_NODE);
			return;
		}
		infoHolder.setState(state);
		handler.fireNodeHandlerEvent(driverName, infoHolder, NodeHandlerEvent.UPDATE_NODE);
	}
}
