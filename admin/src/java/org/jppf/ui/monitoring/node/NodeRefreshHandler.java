/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.ui.monitoring.node;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.utils.NetworkUtils;

/**
 * Instances of this class hold information about the associations between JPPF drivers and
 * their attached nodes, for management and monitoring purposes. 
 * @author Laurent Cohen
 */
public class NodeRefreshHandler
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeRefreshHandler.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * JPPF client used to submit execution requests.
	 */
	private JPPFClient jppfClient = null;
	/**
	 * Timer used to query the driver management data. 
	 */
	private Timer refreshTimer = null;
	/**
	 * Interval, in milliseconds, between refreshes from the server.
	 */
	private long refreshInterval = 1000L;
	/**
	 * The panel to refresh.
	 */
	private NodeDataPanel nodeDataPanel = null;

	/**
	 * Initialize this node handler.
	 * @param nodeDataPanel - the panel to refresh.
	 */
	public NodeRefreshHandler(NodeDataPanel nodeDataPanel)
	{
		this.nodeDataPanel = nodeDataPanel;
		this.jppfClient = StatsHandler.getInstance().getJppfClient(null);
		initialize();
	}

	/**
	 * Initialize this node refresh handler.
	 */
	private void initialize()
	{
		//refresh();
		startRefreshTimer();
	}

	/**
	 * Refresh the tree structure.
	 */
	public synchronized void refresh()
	{
		Collection<JPPFClientConnection> connectionList = jppfClient.getAllConnections();
		Map<String, JPPFClientConnection> map = new HashMap<String, JPPFClientConnection>();
		for (JPPFClientConnection c: connectionList) map.put(((JPPFClientConnectionImpl) c).getJmxConnection().getId(), c);
		Map<String, JPPFClientConnection> connectionMap = nodeDataPanel.getAllDriverNames();

		// handle drivers that were removed
		List<String> driversToProcess = new ArrayList<String>();
		for (String name: connectionMap.keySet())
		{
			if (!map.containsKey(name)) driversToProcess.add(name);
			else refreshNodes(name);
		}
		for (String name: driversToProcess) removeDriver(name);

		// handle drivers that were added
		driversToProcess = new ArrayList<String>();
		for (String name: map.keySet())
		{
			if (!connectionMap.containsKey(name)) driversToProcess.add(name);
		}
		for (String name: driversToProcess) addDriver(map.get(name));
		nodeDataPanel.refreshNodeStates();
		nodeDataPanel.getTreeTable().invalidate();
		nodeDataPanel.getTreeTable().repaint();
	}

	/**
	 * Refresh the nodes currently attached to the specified driver.
	 * @param driverName - the name of the driver.
	 */
	private synchronized void refreshNodes(String driverName)
	{
		DefaultMutableTreeNode driverNode = nodeDataPanel.findDriver(driverName);
		if (driverNode == null) return;
		Set<String> panelNames = new HashSet<String>();
		for (int i=0; i<driverNode.getChildCount(); i++)
		{
			DefaultMutableTreeNode nodeNode = (DefaultMutableTreeNode) driverNode.getChildAt(i);
			TopologyData data = (TopologyData) nodeNode.getUserObject();
			panelNames.add(data.getJmxWrapper().getId());
		}
		TopologyData data = (TopologyData) driverNode.getUserObject();
		JMXDriverConnectionWrapper wrapper = (JMXDriverConnectionWrapper) data.getJmxWrapper();
		if (!wrapper.isConnected()) return;
		Collection<NodeManagementInfo> nodesInfo = null;
		try
		{
			nodesInfo = wrapper.nodesInformation();
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			return;
		}
		if (nodesInfo == null) return;
		Map<String, NodeManagementInfo> actualMap = new HashMap<String, NodeManagementInfo>();
		for (NodeManagementInfo info: nodesInfo) actualMap.put(NetworkUtils.getHostName(info.getHost()) + ":" + info.getPort(), info);
		List<String> nodesToProcess = new ArrayList<String>();
		for (String name: panelNames)
		{
			if (!actualMap.containsKey(name)) nodesToProcess.add(name);
		}
		for (String name: nodesToProcess) nodeDataPanel.nodeRemoved(driverName, name);
		nodesToProcess = new ArrayList<String>();
		for (String name: actualMap.keySet())
		{
			if (!panelNames.contains(name)) nodesToProcess.add(name);
		}
		for (String name: nodesToProcess) nodeDataPanel.nodeAdded(driverName, actualMap.get(name));
	}

	/**
	 * Add a driver connection to the currently monitored set of connections.
	 * @param connection - the driver connection to add.
	 */
	private synchronized void addDriver(JPPFClientConnection connection)
	{
		nodeDataPanel.driverAdded(connection);
	}

	/**
	 * Remove a driver connection from the currently monitored set of connections.
	 * @param driverName the name of the connection to remove.
	 */
	private synchronized void removeDriver(String driverName)
	{
		nodeDataPanel.driverRemoved(driverName, false);
	}

	/**
	 * Stop the automatic refresh of the nodes state through a timer.
	 */
	public void stopRefreshTimer()
	{
		if (refreshTimer != null)
		{
			refreshTimer.cancel();
			refreshTimer = null;
		}
	}

	/**
	 * Start the automatic refresh of the nodes state through a timer.
	 */
	public void startRefreshTimer()
	{
		if (refreshTimer != null) return;
		if (refreshInterval <= 0L) return;
		refreshTimer = new Timer("JPPF Topology Update Timer");
		TimerTask task = new TimerTask()
		{
			public void run()
			{
				refresh();
			}
		};
		refreshTimer.schedule(task, 1000L, refreshInterval);
	}
}
