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

package org.jppf.ui.monitoring.node;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.*;
import javax.swing.tree.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.management.*;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.node.actions.*;
import org.jppf.ui.options.FormattedNumberOption;
import org.jppf.ui.treetable.*;
import org.slf4j.*;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
 */
public class NodeDataPanel extends AbstractTreeTableOption implements ClientListener, ActionHolder
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(NodeDataPanel.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Handles the automatic and manual refresh of the tree.
	 */
	private transient NodeRefreshHandler refreshHandler = null;
	/**
	 * Mapping of connection names to status listener.
	 */
	private Map<String, ConnectionStatusListener> listenerMap = new Hashtable<String, ConnectionStatusListener>();
	/**
	 * Number of active servers.
	 */
	private AtomicInteger nbServers = new AtomicInteger(0);
	/**
	 * Number of active nodes.
	 */
	private AtomicInteger nbNodes = new AtomicInteger(0);

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
		refreshHandler = new NodeRefreshHandler(this);
		createUI();
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
	  treeTable.getTree().setLargeModel(true);
	  treeTable.getTree().setRootVisible(false);
	  treeTable.getTree().setShowsRootHandles(true);
		treeTable.expandAll();
		treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
		treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		treeTable.doLayout();
		treeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		treeTable.getTree().setCellRenderer(new NodeRenderer());
		JScrollPane sp = new JScrollPane(treeTable);
		setUIComponent(sp);
		//setupActions();
		treeTable.expandAll();
	}

	/**
	 * Called when the state information of a node has changed.
	 * @param driverName the name of the driver to which the node is attached.
	 * @param nodeName the name of the node to update.
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
	 * @param connection a reference to the driver connection.
	 */
	public synchronized void driverAdded(final JPPFClientConnection connection)
	{
		JMXDriverConnectionWrapper wrapper = ((JPPFClientConnectionImpl) connection).getJmxConnection();
		String driverName = wrapper.getId();
		int index = driverInsertIndex(driverName);
		if (index < 0) return;
		TopologyData driverData = new TopologyData(connection);
		DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(driverData);
		if (debugEnabled) log.debug("adding driver: " + driverName + " at index " + index);
		model.insertNodeInto(driverNode, treeTableRoot, index);
		if (listenerMap.get(wrapper.getId()) == null)
		{
			ConnectionStatusListener listener = new ConnectionStatusListener(this, wrapper.getId());
			connection.addClientConnectionStatusListener(listener);
			listenerMap.put(wrapper.getId(), listener);
		}
		Collection<JPPFManagementInfo> nodes = null;
		try
		{
			nodes = wrapper.nodesInformation();
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			return;
		}
		if (nodes != null) for (JPPFManagementInfo nodeInfo: nodes) nodeAdded(driverNode, nodeInfo);
		if (treeTable != null) treeTable.expand(driverNode);
		updateStatusBar("/StatusNbServers", 1);
	}

	/**
	 * Called to notify that a driver was removed.
	 * @param driverName the name of the driver to remove.
	 * @param removeNodesOnly true if only the nodes attached to the driver are to be removed.
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
		else
		{
			model.removeNodeFromParent(driverNode);
			updateStatusBar("/StatusNbServers", -1);
		}
	}

	/**
	 * Called to notify that a node was added to a driver.
	 * @param driverName the name of the driver to which the node is added.
	 * @param nodeInfo the object that encapsulates the node addition.
	 */
	public synchronized void nodeAdded(String driverName, JPPFManagementInfo nodeInfo)
	{
		final DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (driverNode == null) return;
		nodeAdded(driverNode, nodeInfo);
	}

	/**
	 * Called to notify that a node was added to a driver.
	 * @param driverNode the driver to which the node is added.
	 * @param nodeInfo the object that encapsulates the node addition.
	 */
	public synchronized void nodeAdded(DefaultMutableTreeNode driverNode, JPPFManagementInfo nodeInfo)
	{
		String nodeName = nodeInfo.getHost() + ":" + nodeInfo.getPort();
		int index = nodeInsertIndex(driverNode, nodeName);
		if (index < 0) return;
		if (debugEnabled) log.debug("adding node: " + nodeName + " at index " + index);
		TopologyData data = new TopologyData(nodeInfo);
		DefaultMutableTreeNode nodeNode = new DefaultMutableTreeNode(data);
		model.insertNodeInto(nodeNode, driverNode, index);
		if (nodeInfo.getType() == JPPFManagementInfo.NODE) updateStatusBar("/StatusNbNodes", 1);

		for (int i=0; i<treeTableRoot.getChildCount(); i++)
		{
			DefaultMutableTreeNode driverNode2 = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
			if (driverNode2 == driverNode) continue;
			DefaultMutableTreeNode nodeNode2 = findNode(driverNode2, nodeName);
			if (nodeNode2 != null) model.removeNodeFromParent(nodeNode2);
		}
	}

	/**
	 * Called to notify that a node was removed from a driver.
	 * @param driverName the name of the driver from which the node is removed.
	 * @param nodeName the name of the node to remove.
	 */
	public synchronized void nodeRemoved(String driverName, String nodeName)
	{
		DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (driverNode == null) return;
		final DefaultMutableTreeNode node = findNode(driverNode, nodeName);
		if (node == null) return;
		if (debugEnabled) log.debug("removing node: " + nodeName);
		model.removeNodeFromParent(node);
		TopologyData data = (TopologyData) node.getUserObject();
		if ((data != null) && (data.getNodeInformation().getType() == JPPFManagementInfo.NODE)) updateStatusBar("/StatusNbNodes", -1);
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
	 * @param driverNode name the parent of the node to find.
	 * @param nodeName the name of the node to find.
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
	 * Find the position at which to insert a driver,
	 * using the sorted lexical order of driver names. 
	 * @param driverName the name of the driver to insert.
	 * @return the index at which to insert the driver, or -1 if the driver is already in the tree.
	 */
	public synchronized int driverInsertIndex(String driverName)
	{
		int n = treeTableRoot.getChildCount();
		for (int i=0; i<n; i++)
		{
			DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
			TopologyData data = (TopologyData) driverNode.getUserObject();
			String name = data.getJmxWrapper().getId();
			if (name.equals(driverName)) return -1;
			else if (driverName.compareTo(name) < 0) return i;
		}
		return n;
	}

	/**
	 * Find the position at which to insert a node, using the sorted lexical order of node names. 
	 * @param driverNode name the parent of the node to insert.
	 * @param nodeName the name of the node to insert.
	 * @return the index at which to insert the node, or -1 if the node is already in the tree.
	 */
	public synchronized int nodeInsertIndex(DefaultMutableTreeNode driverNode, String nodeName)
	{
		int n = driverNode.getChildCount();
		for (int i=0; i<n; i++)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
			TopologyData nodeData = (TopologyData) node.getUserObject();
			String name = nodeData.getJmxWrapper().getId();
			if (nodeName.equals(name)) return -1;
			else if (nodeName.compareTo(name) < 0) return i;
		}
		return n;
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
		actionHandler.putAction("select.drivers", new SelectDriversAction(this));
		actionHandler.putAction("select.nodes", new SelectNodesAction(this));
		actionHandler.updateActions();
		treeTable.addMouseListener(new NodeTreeTableMouseListener(actionHandler));
		Runnable r = new ActionsInitializer(this, "/topology.toolbar");
		new Thread(r).start();
	}

	/**
	 * Notifiy this listener that a new driver connection was created.
	 * @param event the event to notify this listener of.
	 * @see org.jppf.client.event.ClientListener#newConnection(org.jppf.client.event.ClientEvent)
	 */
	public synchronized void newConnection(ClientEvent event)
	{
		driverAdded(event.getConnection());
	}

	/**
	 * Update the number of active servers or nodes in the status bar.
	 * @param name the name of the field to update.
	 * @param n the number of servers to add or subtract.
	 */
	private void updateStatusBar(String name, int n)
	{
		try
		{
			AtomicInteger nb = "/StatusNbServers".equals(name) ? nbServers : nbNodes;
			int newNb = nb.addAndGet(n);
			if (debugEnabled) log.debug("updating '" + name + "' with value = " + n + ", result = " + newNb);
			FormattedNumberOption option = (FormattedNumberOption) findFirstWithName(name);
			if (option != null) option.setValue(Double.valueOf(newNb));
		}
		catch(Throwable t)
		{
			log.error(t.getMessage(), t);
		}
	}

	/**
	 * Refresh the number of active servers and nodes in the status bar.
	 */
	public void refreshStatusBar()
	{
		FormattedNumberOption option = (FormattedNumberOption) findFirstWithName("/StatusNbServers");
		if (option != null) option.setValue(Double.valueOf(nbServers.get()));
		option = (FormattedNumberOption) findFirstWithName("/StatusNbNodes");
		if (option != null) option.setValue(Double.valueOf(nbNodes.get()));
	}
}
