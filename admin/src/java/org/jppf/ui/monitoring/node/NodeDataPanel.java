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

package org.jppf.ui.monitoring.node;

import java.util.Map;

import javax.swing.*;
import javax.swing.tree.*;

import org.apache.commons.logging.*;
import org.jppf.management.NodeManagementInfo;
import org.jppf.ui.monitoring.data.*;
import org.jppf.ui.monitoring.event.*;
import org.jppf.ui.options.AbstractOption;
import org.jppf.ui.treetable.JTreeTable;
import org.jppf.utils.LocalizationUtils;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
 */
public class NodeDataPanel extends AbstractOption implements NodeHandlerListener
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
	 * Base name for localization bundle lookups.
	 */
	private static final String BASE = "org.jppf.ui.i18n.NodeDataPage";
	/**
	 * A tree table component displaying the driver and nodes information. 
	 */
	private JPPFNodeTreeTable treeTable = null;
	/**
	 * Contains all the data about the drivers and nodes.
	 */
	private transient NodeHandler handler = null;
	/**
	 * The tree table model associated witht he tree table.
	 */
	private transient JPPFNodeTreeTableModel model = null;
	/**
	 * The root of the tree model.
	 */
	private DefaultMutableTreeNode root = null;

	/**
	 * Initialize this panel with the specified information.
	 */
	public NodeDataPanel()
	{
		this(new NodeHandler());
	}

	/**
	 * Initialize this panel with the specified information.
	 * @param handler contains all the data about the drivers and nodes.
	 */
	public NodeDataPanel(NodeHandler handler)
	{
		if (debugEnabled) log.debug("initializing NodeDataPanel");
		this.handler = handler;
		createTreeTableModel();
		createUI();
	  handler.addNodeHandlerListener(this);
	}

	/**
	 * Create and initialize the tree table model holding the drivers and nodes data.
	 */
	private void createTreeTableModel()
	{
		root = new DefaultMutableTreeNode(localize("tree.root.name"));
		model = new JPPFNodeTreeTableModel(root);
	}

	/**
	 * Create and initialize the tree table model holding the drivers and nodes data.
	 */
	private void populateTreeTableModel()
	{
		Map<String, NodeInfoManager> nodeManagerMap = handler.getNodeManagerMap();
		for (Map.Entry<String, NodeInfoManager> mgrEntry: nodeManagerMap.entrySet())
		{
			DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(mgrEntry.getKey());
			model.insertNodeInto(driverNode, root, root.getChildCount());
			Map<NodeManagementInfo, NodeInfoHolder> driverNodeMap = mgrEntry.getValue().getNodeMap();
			for (Map.Entry<NodeManagementInfo, NodeInfoHolder> infoEntry: driverNodeMap.entrySet())
			{
				DefaultMutableTreeNode nodeNode = new DefaultMutableTreeNode(infoEntry.getValue());
				model.insertNodeInto(nodeNode, driverNode, driverNode.getChildCount());
			}
		}
	}

	/**
	 * Create, initialize and layout the GUI components displayed in this panel.
	 */
	public void createUI()
	{
	  treeTable = new JPPFNodeTreeTable(model);
	  treeTable.getTree().setRootVisible(false);
	  treeTable.getTree().setShowsRootHandles(true);
	  populateTreeTableModel();
		treeTable.expandAll();
		treeTable.addMouseListener(new NodeTreeTableMouseListener());
		treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
		treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		treeTable.doLayout();
		treeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		treeTable.getTree().setCellRenderer(new NodeRenderer());
		JScrollPane sp = new JScrollPane(treeTable);
		setUIComponent(sp);
	}

	/**
	 * Called when the state information of a node has changed.
	 * @param event the event that encapsulates the node information.
	 * @see org.jppf.ui.monitoring.event.NodeHandlerListener#nodeDataUpdated(org.jppf.ui.monitoring.event.NodeHandlerEvent)
	 */
	public void nodeDataUpdated(NodeHandlerEvent event)
	{
		final DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		final DefaultMutableTreeNode driverNode = findDriver(event.getDriverName());
		if (driverNode == null) return;
		final DefaultMutableTreeNode node = findNode(driverNode, event.getInfoHolder());
		if (node == null) return;
		treeTable.repaint();
	}

	/**
	 * Called to notify that a driver was added.
	 * @param event the object that encapsulates the driver addition.
	 * @see org.jppf.ui.monitoring.event.NodeHandlerListener#driverAdded(org.jppf.ui.monitoring.event.NodeHandlerEvent)
	 */
	public void driverAdded(NodeHandlerEvent event)
	{
		final DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		final DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(event.getDriverName());
		if (debugEnabled) log.debug("adding driver: " + event.getDriverName());
		model.insertNodeInto(driverNode, root, root.getChildCount());
		if (root.getChildCount() == 1) expandAndResizeColumns();
	}

	/**
	 * Called to notify that a driver was removed.
	 * @param event the object that encapsulates the driver removal.
	 * @see org.jppf.ui.monitoring.event.NodeHandlerListener#driverRemoved(org.jppf.ui.monitoring.event.NodeHandlerEvent)
	 */
	public void driverRemoved(NodeHandlerEvent event)
	{
		final DefaultMutableTreeNode driverNode = findDriver(event.getDriverName());
		if (debugEnabled) log.debug("removing driver: " + event.getDriverName());
		if (driverNode != null) model.removeNodeFromParent(driverNode);
	}

	/**
	 * Called to notify that a node was added to a driver.
	 * @param event the object that encapsulates the node addition.
	 * @see org.jppf.ui.monitoring.event.NodeHandlerListener#nodeAdded(org.jppf.ui.monitoring.event.NodeHandlerEvent)
	 */
	public void nodeAdded(NodeHandlerEvent event)
	{
		final DefaultMutableTreeNode driverNode = findDriver(event.getDriverName());
		if (driverNode == null) return;
		final DefaultMutableTreeNode node = new DefaultMutableTreeNode(event.getInfoHolder());
		if (debugEnabled) log.debug("adding node: " + event.getInfoHolder());
		model.insertNodeInto(node, driverNode, driverNode.getChildCount());
		final DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		if (root.getChildCount() == 1) expandAndResizeColumns();
	}

	/**
	 * Called to notify that a node was removed from a driver.
	 * @param event the object that encapsulates the node removal.
	 * @see org.jppf.ui.monitoring.event.NodeHandlerListener#nodeRemoved(org.jppf.ui.monitoring.event.NodeHandlerEvent)
	 */
	public void nodeRemoved(NodeHandlerEvent event)
	{
		DefaultMutableTreeNode driverNode = findDriver(event.getDriverName());
		if (driverNode == null) return;
		final DefaultMutableTreeNode node = findNode(driverNode, event.getInfoHolder());
		if (node != null)
		{
			if (debugEnabled) log.debug("removing node: " + event.getInfoHolder());
			model.removeNodeFromParent(node);
		}
	}

	/**
	 * Find the driver tree node with the specified driver name.
	 * @param driverName name of the dirver to find.
	 * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
	 */
	private DefaultMutableTreeNode findDriver(String driverName)
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		for (int i=0; i<root.getChildCount(); i++)
		{
			DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) root.getChildAt(i);
			String name = (String) driverNode.getUserObject();
			if (name.equals(driverName)) return driverNode;
		}
		return null;
	}

	/**
	 * Find the node tree node with the specified driver name and node information.
	 * @param driverNode name the parent of the node to find.
	 * @param info the information on the node to find.
	 * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
	 */
	private DefaultMutableTreeNode findNode(DefaultMutableTreeNode driverNode, NodeInfoHolder info)
	{
		for (int i=0; i<driverNode.getChildCount(); i++)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
			NodeInfoHolder nodeInfoHolder = (NodeInfoHolder) node.getUserObject();
			if (nodeInfoHolder.equals(info)) return node;
		}
		return null;
	}

	/**
	 * Get a localized message given its unique name and the current locale.
	 * @param message the unique name of the localized message.
	 * @return a message in the current locale, or the default locale 
	 * if the localization for the current locale is not found. 
	 */
	private String localize(String message)
	{
		return LocalizationUtils.getLocalized(BASE, message);
	}

	/**
	 * Get the container for all the data about the drivers and nodes.
	 * @return a NodeHandler instance.
	 */
	public synchronized NodeHandler getHandler()
	{
		return handler;
	}

	/**
	 * Get the tree table component displaying the driver and nodes information. 
	 * @return a <code>JXTreeTable</code> instance.
	 */
	public synchronized JTreeTable getTreeTable()
	{
		return treeTable;
	}

	/**
	 * Not implemented.
	 * @param enabled not used.
	 * @see org.jppf.ui.options.OptionElement#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled)
	{
	}

	/**
	 * Not implemented.
	 * @param enabled not used.
	 * @see org.jppf.ui.options.OptionElement#setEventsEnabled(boolean)
	 */
	public void setEventsEnabled(boolean enabled)
	{
	}

	/**
	 * Not implemented.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	protected void setupValueChangeNotifications()
	{
	}

	/**
	 * Create, initialize and layout the GUI components displayed in this panel.
	 */
	private void expandAndResizeColumns()
	{
		treeTable.expandAll();
		treeTable.sizeColumnsToFit(0);
	  //for (int i=0; i<model.getColumnCount(); i++) treeTable.sizeColumnsToFit(i);
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
	private boolean areOnlyTypeSelected(boolean checkNodes)
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
			if (treeNode.getUserObject() instanceof NodeInfoHolder) nbNodes++;
			else nbDrivers++;
		}
		return (checkNodes && (nbNodes > 0) && (nbDrivers == 0)) || (!checkNodes && (nbNodes == 0) && (nbDrivers > 0));
	}
}
