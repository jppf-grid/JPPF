/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.management.NodeManagementInfo;
import org.jppf.ui.monitoring.event.*;

/**
 * Instances of this class hold information about the associations between JPPF drivers and
 * their attached nodes, for management and monitoring purposes. 
 * @author Laurent Cohen
 */
public class NodeHandler implements ClientListener
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeHandler.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * JPPF client used to submit execution requests.
	 */
	private JPPFClient jppfClient = null;
	/**
	 * Contains all the data and its converted values received from the server.
	 */
	private Map<String, NodeInfoManager> nodeManagerMap = new HashMap<String, NodeInfoManager>();
	/**
	 * Timer used to query the nodes states. 
	 */
	private Timer nodeTimer = null;
	/**
	 * Timer used to query the driver management data. 
	 */
	private Timer driverTimer = null;
	/**
	 * Interval, in milliseconds, between refreshes from the server.
	 */
	private long refreshInterval = 1000L;
	/**
	 * List of listeners registered with this stats formatter.
	 */
	private List<NodeHandlerListener> listeners = new ArrayList<NodeHandlerListener>();

	/**
	 * Initialize this node handler with the specified jppf client.
	 * @param jppfClient the client that manages the connections to all configured JPPF drivers.
	 */
	public NodeHandler(JPPFClient jppfClient)
	{
		this.jppfClient = jppfClient;
		jppfClient.addClientListener(this);
		initialize(false);
	}

	/**
	 * Initialize the association of driver connections with the corresponding node managers.
	 * @param triggerEvents specifies whether the listeners should be notified.
	 */
	private void initialize(boolean triggerEvents)
	{
		//refresh(triggerEvents);
		startRefreshNodeTimer();
		startRefreshDriverTimer();
	}

	/**
	 * Refresh the association of driver connections with the corresponding node managers.
	 * @param triggerEvents specifies whether the listeners should be notified.
	 */
	public synchronized void refresh(boolean triggerEvents)
	{
		List<String> list = jppfClient.getAllConnectionNames();
		Set<String> connectionSet = nodeManagerMap.keySet();

		// handle drivers that were removed
		List<String> driversToProcess = new ArrayList<String>();
		for (String name: connectionSet)
		{
			if (!list.contains(name)) driversToProcess.add(name);
		}
		for (String name: driversToProcess) removeDriver(name, triggerEvents);

		// handle drivers that were added
		driversToProcess = new ArrayList<String>();
		for (String name: list)
		{
			if (!nodeManagerMap.containsKey(name)) driversToProcess.add(name);
		}
		for (String name: driversToProcess) addDriver(name, triggerEvents);

		for (String name: list)
		{
			NodeInfoManager nodeMgr = nodeManagerMap.get(name);
			Collection<NodeManagementInfo> nodeList = null;
			try
			{
				JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) jppfClient.getClientConnection(name);
				nodeList = c.getNodeManagementInfo();
				if (nodeList == null) continue;
			}
			catch(Exception e)
			{
				if (debugEnabled) log.debug(e.getMessage(), e);
				continue;
			}

			// handle nodes that were removed
			List<NodeManagementInfo> nodesToProcess = new ArrayList<NodeManagementInfo>();
			for (NodeManagementInfo info: nodeMgr.getNodeMap().keySet())
			{
				if (!nodeList.contains(info)) nodesToProcess.add(info);
			}
			for (NodeManagementInfo info: nodesToProcess) removeNode(name, info, triggerEvents);

			// handle nodes that were added
			nodesToProcess = new ArrayList<NodeManagementInfo>();
			for (NodeManagementInfo info: nodeList)
			{
				if (!nodeMgr.hasNode(info)) nodesToProcess.add(info);
			}
			for (NodeManagementInfo info: nodesToProcess) addNode(name, info, triggerEvents);
		}
	}

	/**
	 * Add a driver connection to the currently monitored set of connections.
	 * @param driverName the name of the connection to add.
	 * @param triggerEvent specifies whether listeners should be notified.
	 * @return the <code>NodeInfoManager</code> that was added.
	 */
	private synchronized NodeInfoManager addDriver(String driverName, boolean triggerEvent)
	{
		NodeInfoManager nodeMgr = new NodeInfoManager(driverName);
		nodeManagerMap.put(driverName, nodeMgr);
		if (triggerEvent)
		{
			fireNodeHandlerEvent(driverName, null, NodeHandlerEvent.ADD_DRIVER);
		}
		return nodeMgr;
	}

	/**
	 * Remove a driver connection from the currently monitored set of connections.
	 * @param driverName the name of the connection to remove.
	 * @param triggerEvent specifies whether listeners should be notified.
	 */
	private synchronized void removeDriver(String driverName, boolean triggerEvent)
	{
		nodeManagerMap.remove(driverName);
		if (triggerEvent)
		{
			fireNodeHandlerEvent(driverName, null, NodeHandlerEvent.REMOVE_DRIVER);
		}
	}

	/**
	 * Add a node connection to the specified driver connection.
	 * @param driverName the name of the connection to add a node to.
	 * @param info the information for the node to add.
	 * @param triggerEvent specifies whether listeners should be notified.
	 */
	private synchronized void addNode(String driverName, NodeManagementInfo info, boolean triggerEvent)
	{
		NodeInfoManager nodeMgr = nodeManagerMap.get(driverName);
		nodeMgr.addNode(info);
		nodeMgr.activateNode(info);
		if (triggerEvent)
		{
			NodeInfoHolder infoHolder = nodeMgr.getNodeMap().get(info);
			fireNodeHandlerEvent(driverName, infoHolder, NodeHandlerEvent.ADD_NODE);
		}
	}

	/**
	 * Remove a node connection from the specified driver connection.
	 * @param driverName the name of the connection from which to remove the node.
	 * @param info the information for the node to remove.
	 * @param triggerEvent specifies whether listeners should be notified.
	 */
	private synchronized void removeNode(String driverName, NodeManagementInfo info, boolean triggerEvent)
	{
		NodeInfoManager nodeMgr = nodeManagerMap.get(driverName);
		NodeInfoHolder infoHolder = nodeMgr.getNodeMap().get(info);
		nodeMgr.removeNode(info);
		if (triggerEvent)
		{
			fireNodeHandlerEvent(driverName, infoHolder, NodeHandlerEvent.REMOVE_NODE);
		}
	}

	/**
	 * Stop the automatic refresh of the nodes state through a timer.
	 */
	public void stopRefreshNodeTimer()
	{
		if (nodeTimer != null)
		{
			nodeTimer.cancel();
			nodeTimer = null;
		}
	}

	/**
	 * Start the automatic refresh of the nodes state through a timer.
	 */
	public void startRefreshNodeTimer()
	{
		if (nodeTimer != null) return;
		if (refreshInterval <= 0L) return;
		nodeTimer = new Timer("JPPF Nodes Update Timer");
		TimerTask task = new NodeRefreshTask(this);
		nodeTimer.schedule(task, 1000L, refreshInterval);
	}

	/**
	 * Stop the automatic refresh of the nodes state through a timer.
	 */
	public void stopRefreshDriverTimer()
	{
		if (driverTimer != null)
		{
			driverTimer.cancel();
			driverTimer = null;
		}
	}

	/**
	 * Start the automatic refresh of the nodes state through a timer.
	 */
	public void startRefreshDriverTimer()
	{
		if (driverTimer != null) return;
		if (refreshInterval <= 0L) return;
		driverTimer = new Timer("JPPF Drivers Update Timer");
		TimerTask task = new TimerTask()
		{
			public void run()
			{
				refresh(true);
			}
		};
		driverTimer.schedule(task, 1000L, refreshInterval);
	}

	/**
	 * Get the map that contains all the data and its converted values received from the server.
	 * @return a map of strings to <code>NodeInfoManager</code> instances.
	 */
	public Map<String, NodeInfoManager> getNodeManagerMap()
	{
		return nodeManagerMap;
	}

	/**
	 * Register a <code>NodeHandlerListener</code> with this node handler.
	 * @param listener the listener to register.
	 */
	public void addNodeHandlerListener(NodeHandlerListener listener)
	{
		if (listener != null) listeners.add(listener);
	}

	/**
	 * Unregister a <code>NodeHandlerListener</code> from this node handler.
	 * @param listener the listener to register.
	 */
	public void removeNodeHandlerListener(NodeHandlerListener listener)
	{
		if (listener != null) listeners.remove(listener);
	}

	/**
	 * Notify all listeners of a change in this node handler.
	 * @param driverName The name of the driver to which the node is attached.
	 * @param infoHolder The node connection and state information.
	 * @param operation the type of operation to notify for.
	 */
	public void fireNodeHandlerEvent(String driverName, NodeInfoHolder infoHolder, int operation)
	{
		NodeHandlerEvent event = new NodeHandlerEvent(this, driverName, infoHolder, operation);
		for (NodeHandlerListener listener: listeners)
		{
			switch(operation)
			{
				case NodeHandlerEvent.ADD_DRIVER:
					listener.driverAdded(event);
					break;
				case NodeHandlerEvent.REMOVE_DRIVER:
					listener.driverRemoved(event);
					break;
				case NodeHandlerEvent.ADD_NODE:
					listener.nodeAdded(event);
					break;
				case NodeHandlerEvent.REMOVE_NODE:
					listener.nodeRemoved(event);
					break;
				case NodeHandlerEvent.UPDATE_NODE:
					listener.nodeDataUpdated(event);
					break;
			}
		}
	}

	/**
	 * Notifiy this listener that a new driver connection was created.
	 * @param event the event to notify this listener of.
	 * @see org.jppf.client.event.ClientListener#newConnection(org.jppf.client.event.ClientEvent)
	 */
	public synchronized void newConnection(ClientEvent event)
	{
		refresh(true);
	}
}
