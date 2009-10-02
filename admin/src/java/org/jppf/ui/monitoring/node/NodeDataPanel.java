/*
 * Java Parallel Processing Framework.
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

import javax.swing.*;
import javax.swing.tree.*;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.management.*;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.node.actions.*;
import org.jppf.ui.treetable.*;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
 */
public class NodeDataPanel extends AbstractTreeTableOption implements ClientListener
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeDataPanel.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Handles the automatic and manual refresh of the tree.
	 */
	private transient NodeRefreshHandler refreshHandler = null;
	/**
	 * Mapping of connection names to status listener.
	 */
	private Map<String, ConnectionStatusListener> listenerMap = new Hashtable<String, ConnectionStatusListener>();

	/**
	 * Initialize this panel with the specified information.
	 */
	public NodeDataPanel()
	{
		BASE = "org.jppf.ui.i18n.NodeDataPage";
		if (debugEnabled) log.debug("initializing NodeDataPanel");
		createTreeTableModel();
		populateTreeTableModel();
		refreshNodeStates();
		createUI();
		refreshHandler = new NodeRefreshHandler(this);
	}

	/**
	 * Create and initialize the tree table model holding the drivers and nodes data.
	 */
	private void createTreeTableModel()
	{
		treeTableRoot = new DefaultMutableTreeNode(localize("tree.root.name"));
		model = new JPPFNodeTreeTableModel(treeTableRoot);
	}

	/**
	 * Create and initialize the tree table model holding the drivers and nodes data.
	 */
	private void populateTreeTableModel()
	{
		JPPFClient client = StatsHandler.getInstance().getJppfClient(null);
		List<JPPFClientConnection> allConnections = client.getAllConnections();
		for (JPPFClientConnection c: allConnections) driverAdded(c);
	}

	/**
	 * Create, initialize and layout the GUI components displayed in this panel.
	 */
	public void createUI()
	{
	  treeTable = new JPPFTreeTable(model);
	  treeTable.getTree().setRootVisible(false);
	  treeTable.getTree().setShowsRootHandles(true);
	  populateTreeTableModel();
		treeTable.expandAll();
		treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
		treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		treeTable.doLayout();
		treeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		treeTable.getTree().setCellRenderer(new NodeRenderer());
		JScrollPane sp = new JScrollPane(treeTable);
		setUIComponent(sp);
		setupActions();
		treeTable.expandAll();
	}

	/**
	 * Called when the state information of a node has changed.
	 * @param driverName - the name of the driver to which the node is attached.
	 * @param nodeName - the name of the node to update.
	 */
	public synchronized void nodeDataUpdated(String driverName, String nodeName)
	{
		final DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (driverNode == null) return;
		final DefaultMutableTreeNode node = findNode(driverNode, nodeName);
		if (node != null) model.changeNode(node);
	}

	/**
	 * Called to notify that a driver was added.
	 * @param connection - a reference to the driver connection.
	 */
	public synchronized void driverAdded(final JPPFClientConnection connection)
	{
		JMXDriverConnectionWrapper wrapper = ((JPPFClientConnectionImpl) connection).getJmxConnection();
		String driverName = wrapper.getId();
		if (findDriver(driverName) != null) return;
		TopologyData driverData = new TopologyData(connection);
		DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(driverData);
		model.insertNodeInto(driverNode, treeTableRoot, treeTableRoot.getChildCount());
		if (listenerMap.get(wrapper.getId()) == null)
		{
			ConnectionStatusListener listener = new ConnectionStatusListener(wrapper.getId());
			connection.addClientConnectionStatusListener(listener);
			listenerMap.put(wrapper.getId(), listener);
		}
		Collection<NodeManagementInfo> nodes = null;
		try
		{
			nodes = wrapper.nodesInformation();
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			return;
		}
		if (nodes != null) for (NodeManagementInfo nodeInfo: nodes) nodeAdded(driverNode, nodeInfo);
		if (treeTable != null) treeTable.expand(driverNode);
	}

	/**
	 * Called to notify that a driver was removed.
	 * @param driverName - the name of the driver to remove.
	 * @param removeNodesOnly - true if only the nodes attached to the driver are to be removed.
	 */
	public synchronized void driverRemoved(String driverName, boolean removeNodesOnly)
	{
		final DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (debugEnabled) log.debug("removing driver: " + driverName);
		if (driverNode == null) return;
		if (removeNodesOnly)
		{
			for (int i=driverNode.getChildCount()-1; i>=0; i--)
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode ) driverNode.getChildAt(i);
				model.removeNodeFromParent(node);
			}
		}
		else model.removeNodeFromParent(driverNode);
	}

	/**
	 * Called to notify that a node was added to a driver.
	 * @param driverName - the name of the driver to which the node is added.
	 * @param nodeInfo - the object that encapsulates the node addition.
	 */
	public synchronized void nodeAdded(String driverName, NodeManagementInfo nodeInfo)
	{
		final DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (driverNode == null) return;
		nodeAdded(driverNode, nodeInfo);
	}

	/**
	 * Called to notify that a node was added to a driver.
	 * @param driverNode - the driver to which the node is added.
	 * @param nodeInfo - the object that encapsulates the node addition.
	 */
	public synchronized void nodeAdded(DefaultMutableTreeNode driverNode, NodeManagementInfo nodeInfo)
	{
		String nodeName = nodeInfo.getHost() + ":" + nodeInfo.getPort();
		if (findNode(driverNode, nodeName) != null) return;
		TopologyData nodeData = new TopologyData(nodeInfo);
		DefaultMutableTreeNode nodeNode = new DefaultMutableTreeNode(nodeData);
		model.insertNodeInto(nodeNode, driverNode, driverNode.getChildCount());
	}

	/**
	 * Called to notify that a node was removed from a driver.
	 * @param driverName - the name of the driver from which the node is removed.
	 * @param nodeName - the name of the node to remove.
	 */
	public synchronized void nodeRemoved(String driverName, String nodeName)
	{
		DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (driverNode == null) return;
		final DefaultMutableTreeNode node = findNode(driverNode, nodeName);
		if (node == null) return;
		if (debugEnabled) log.debug("removing node: " + nodeName);
		model.removeNodeFromParent(node);
	}

	/**
	 * Find the driver tree node with the specified driver name.
	 * @param driverName name of the dirver to find.
	 * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
	 */
	public synchronized DefaultMutableTreeNode findDriver(String driverName)
	{
		for (int i=0; i<treeTableRoot.getChildCount(); i++)
		{
			DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
			TopologyData data = (TopologyData) driverNode.getUserObject();
			String name = data.getJmxWrapper().getId();
			if (name.equals(driverName)) return driverNode;
		}
		return null;
	}

	/**
	 * Find the node tree node with the specified driver name and node information.
	 * @param driverNode - name the parent of the node to find.
	 * @param nodeName - the name of the node to find.
	 * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
	 */
	public synchronized DefaultMutableTreeNode findNode(DefaultMutableTreeNode driverNode, String nodeName)
	{
		for (int i=0; i<driverNode.getChildCount(); i++)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
			TopologyData nodeData = (TopologyData) node.getUserObject();
			if (nodeName.equals(nodeData.getJmxWrapper().getId())) return node;
		}
		return null;
	}

	/**
	 * Get the object that handles the automatic and manual refresh of the tree.
	 * @return a <code>NodeRefreshHandler</code> instance.
	 */
	public NodeRefreshHandler getRefreshHandler()
	{
		return refreshHandler;
	}

	/**
	 * Determine whether only nodes are currently selected.
	 * @return true if at least one node and no driver is selected, false otherwise. 
	 */
	public boolean areOnlyNodesSelected()
	{
		return areOnlyTypeSelected(true);
	}

	/**
	 * Determine whether only drivers are currently selected.
	 * @return true if at least one driver and no node is selected, false otherwise. 
	 */
	public boolean areOnlyDriversSelected()
	{
		return areOnlyTypeSelected(false);
	}

	/**
	 * Determine whether only tree elements of the specified type are currently selected.
	 * @param checkNodes true to check if nodes only are selected, false to check if drivers only are selected.
	 * @return true if at least one element of the specified type and no element of another type is selected, false otherwise. 
	 */
	private synchronized boolean areOnlyTypeSelected(boolean checkNodes)
	{
		int[] rows = treeTable.getSelectedRows();
		if ((rows == null) || (rows.length <= 0)) return false;
		int nbNodes = 0;
		int nbDrivers = 0;
		for (int n: rows)
		{
			TreePath path = treeTable.getPathForRow(n);
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
			if (treeNode.getParent() == null) continue;
			TopologyData data = (TopologyData) treeNode.getUserObject();
			if (TopologyDataType.NODE.equals(data.getType())) nbNodes++;
			else nbDrivers++;
		}
		return (checkNodes && (nbNodes > 0) && (nbDrivers == 0)) || (!checkNodes && (nbNodes == 0) && (nbDrivers > 0));
	}

	/**
	 * Get a mapping of driver names to their corresponding connection.
	 * @return a map of string to <code>JPPFClientConnection</code> instances.
	 */
	public synchronized Map<String, JPPFClientConnection> getAllDriverNames()
	{
		Map<String, JPPFClientConnection> map = new HashMap<String, JPPFClientConnection>();
		for (int i=0; i<treeTableRoot.getChildCount(); i++)
		{
			DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
			TopologyData data = (TopologyData) driverNode.getUserObject();
			map.put(data.getJmxWrapper().getId(), data.getClientConnection());
		}
		return map;
	}

	/**
	 * Refresh the states of all displayed nodes.
	 */
	public synchronized void refreshNodeStates()
	{
		for (int i=0; i<treeTableRoot.getChildCount(); i++)
		{
			DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
			for (int j=0; j<driverNode.getChildCount(); j++)
			{
				DefaultMutableTreeNode nodeNode = (DefaultMutableTreeNode) driverNode.getChildAt(j);
				TopologyData data = (TopologyData) nodeNode.getUserObject();
				data.refreshNodeState();
			}
		}
	}

	/**
	 * Initialize all actions used in the panel.
	 */
	public void setupActions()
	{
		actionHandler = new JTreeTableActionHandler(treeTable);
		actionHandler.putAction("shutdown.restart.driver", new ServerShutdownRestartAction());
		actionHandler.putAction("update.configuration", new NodeConfigurationAction());
		actionHandler.putAction("show.information", new NodeInformationAction());
		actionHandler.putAction("update.threads", new NodeThreadsAction());
		actionHandler.putAction("reset.counter", new ResetTaskCounterAction());
		actionHandler.putAction("restart.node", new RestartNodeAction());
		actionHandler.putAction("shutdown.node", new ShutdownNodeAction());
		actionHandler.updateActions();
		treeTable.addMouseListener(new NodeTreeTableMouseListener(actionHandler));
		Runnable r = new ActionsInitializer(this, "/topology.toolbar");
		new Thread(r).start();
	}

	/**
	 * Notifiy this listener that a new driver connection was created.
	 * @param event - the event to notify this listener of.
	 * @see org.jppf.client.event.ClientListener#newConnection(org.jppf.client.event.ClientEvent)
	 */
	public synchronized void newConnection(ClientEvent event)
	{
		driverAdded(event.getConnection());
	}

	/**
	 * Listens to JPPF client connection status changes for rendering purposes.
	 */
	public class ConnectionStatusListener implements ClientConnectionStatusListener
	{
		/**
		 * The name of the connection.
		 */
		String driverName = null;

		/**
		 * Initialize this listener with the specified connection name.
		 * @param driverName - the name of the connection.
		 */
		public ConnectionStatusListener(String driverName)
		{
			this.driverName = driverName;
		}

		/**
		 * Invoked when thew conneciton status has changed.
		 * @param event - the connection status event.
		 * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
		 */
		public void statusChanged(ClientConnectionStatusEvent event)
		{
			ClientConnectionStatusHandler ccsh =  event.getClientConnectionStatusHandler();
			System.out.println("Received connection status changed event for " + ccsh + " : " + ccsh.getStatus());
			DefaultMutableTreeNode driverNode = findDriver(driverName);
			if (driverNode != null)
			{
				/*
				TreeNode[] tmp = driverNode.getPath();
				if (tmp == null) return;
				TreePath path = new TreePath(tmp);
				Rectangle rect = treeTable.getTree().getPathBounds(path);
				treeTable.getTree().repaint(rect);
				*/
				//treeTable.getTree().repaint();
				driverRemoved(driverName, true);
			}
		}
	}
}
