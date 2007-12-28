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

package org.jppf.ui.monitoring.node;

import java.util.Map;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jppf.server.NodeManagementInfo;
import org.jppf.ui.monitoring.data.*;
import org.jppf.ui.monitoring.event.*;
import org.jppf.ui.options.AbstractOption;
import org.jppf.utils.LocalizationUtils;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
 */
public class NodeDataPanel extends AbstractOption implements NodeHandlerListener
{
	/**
	 * Base name for localization bundle lookups.
	 */
	private static final String BASE = "org.jppf.ui.i18n.NodeDataPage";
	/**
	 * A tree table component displaying the driver and nodes information. 
	 */
	private JXTreeTable treeTable = null;
	/**
	 * Contains all the data about the drivers and nodes.
	 */
	private transient NodeHandler handler = null;
	/**
	 * The tree table model associated witht he tree table.
	 */
	private transient JPPFNodeTreeTableModel model = null;

	/**
	 * Initialize this panel with the specified information.
	 */
	public NodeDataPanel()
	{
		this(new NodeHandler(StatsHandler.getInstance().getJppfClient()));
	}

	/**
	 * Initialize this panel with the specified information.
	 * @param handler contains all the data about the drivers and nodes.
	 */
	public NodeDataPanel(NodeHandler handler)
	{
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
		DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode(localize("tree.root.name"));
		model = new JPPFNodeTreeTableModel(root);
		Map<String, NodeInfoManager> nodeManagerMap = handler.getNodeManagerMap();
		for (Map.Entry<String, NodeInfoManager> mgrEntry: nodeManagerMap.entrySet())
		{
			DefaultMutableTreeTableNode driverNode = new DefaultMutableTreeTableNode(mgrEntry.getKey());
			model.insertNodeInto(driverNode, root, root.getChildCount());
			Map<NodeManagementInfo, NodeInfoHolder> driverNodeMap = mgrEntry.getValue().getNodeMap();
			for (Map.Entry<NodeManagementInfo, NodeInfoHolder> infoEntry: driverNodeMap.entrySet())
			{
				DefaultMutableTreeTableNode nodeNode = new DefaultMutableTreeTableNode(infoEntry.getValue());
				model.insertNodeInto(nodeNode, driverNode, driverNode.getChildCount());
			}
		}
	}

	/**
	 * Create, initialize and layout the GUI components displayed in this panel.
	 */
	public void createUI()
	{
	  treeTable = new JXTreeTable(model);
	  //setViewportView(treeTable);
		treeTable.expandAll();
	  //for (int i=0; i<model.getColumnCount(); i++) treeTable.sizeColumnsToFit(i);
		setUIComponent(new JScrollPane(treeTable));
		treeTable.addMouseListener(new TreeTableMouseListener());
	}

	/**
	 * Called when the state information of a node has changed.
	 * @param event the event that encapsulates the node information.
	 * @see org.jppf.ui.monitoring.event.NodeHandlerListener#nodeDataUpdated(org.jppf.ui.monitoring.event.NodeHandlerEvent)
	 */
	public void nodeDataUpdated(NodeHandlerEvent event)
	{
		final DefaultMutableTreeTableNode root = (DefaultMutableTreeTableNode) model.getRoot();
		final DefaultMutableTreeTableNode driverNode = findDriver(event.getDriverName());
		if (driverNode == null) return;
		final DefaultMutableTreeTableNode node = findNode(driverNode, event.getInfoHolder());
		if (node == null) return;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				TreePath path = new TreePath(new Object[] { root, driverNode, node });
				treeTable.tableChanged(new TableModelEvent(treeTable.getModel(), treeTable.getRowForPath(path)));
			}
		});
	}

	/**
	 * Called to notify that a driver was added.
	 * @param event the object that encapsulates the driver addition.
	 * @see org.jppf.ui.monitoring.event.NodeHandlerListener#driverAdded(org.jppf.ui.monitoring.event.NodeHandlerEvent)
	 */
	public void driverAdded(NodeHandlerEvent event)
	{
		final DefaultMutableTreeTableNode root = (DefaultMutableTreeTableNode) model.getRoot();
		final DefaultMutableTreeTableNode driverNode = new DefaultMutableTreeTableNode(event.getDriverName());
		model.insertNodeInto(driverNode, root, root.getChildCount());
	}

	/**
	 * Called to notify that a driver was removed.
	 * @param event the object that encapsulates the driver removal.
	 * @see org.jppf.ui.monitoring.event.NodeHandlerListener#driverRemoved(org.jppf.ui.monitoring.event.NodeHandlerEvent)
	 */
	public void driverRemoved(NodeHandlerEvent event)
	{
		final DefaultMutableTreeTableNode driverNode = findDriver(event.getDriverName());
		if (driverNode != null) model.removeNodeFromParent(driverNode);
	}

	/**
	 * Called to notify that a node was added to a driver.
	 * @param event the object that encapsulates the node addition.
	 * @see org.jppf.ui.monitoring.event.NodeHandlerListener#nodeAdded(org.jppf.ui.monitoring.event.NodeHandlerEvent)
	 */
	public void nodeAdded(NodeHandlerEvent event)
	{
		final DefaultMutableTreeTableNode driverNode = findDriver(event.getDriverName());
		if (driverNode == null) return;
		final DefaultMutableTreeTableNode node = new DefaultMutableTreeTableNode(event.getInfoHolder());
		model.insertNodeInto(node, driverNode, driverNode.getChildCount());
	}

	/**
	 * Called to notify that a node was removed from a driver.
	 * @param event the object that encapsulates the node removal.
	 * @see org.jppf.ui.monitoring.event.NodeHandlerListener#nodeRemoved(org.jppf.ui.monitoring.event.NodeHandlerEvent)
	 */
	public void nodeRemoved(NodeHandlerEvent event)
	{
		DefaultMutableTreeTableNode driverNode = findDriver(event.getDriverName());
		if (driverNode == null) return;
		final DefaultMutableTreeTableNode node = findNode(driverNode, event.getInfoHolder());
		if (node != null) model.removeNodeFromParent(node);
	}

	/**
	 * Find the driver tree node with the specified driver name.
	 * @param driverName name of the dirver to find.
	 * @return a <code>DefaultMutableTreeTableNode</code> or null if the driver could not be found.
	 */
	private DefaultMutableTreeTableNode findDriver(String driverName)
	{
		DefaultMutableTreeTableNode root = (DefaultMutableTreeTableNode) model.getRoot();
		for (int i=0; i<root.getChildCount(); i++)
		{
			DefaultMutableTreeTableNode driverNode = (DefaultMutableTreeTableNode) root.getChildAt(i);
			String name = (String) driverNode.getUserObject();
			if (name.equals(driverName)) return driverNode;
		}
		return null;
	}

	/**
	 * Find the node tree node with the specified driver name and node information.
	 * @param driverNode name the parent of the node to find.
	 * @param info the information on the node to find.
	 * @return a <code>DefaultMutableTreeTableNode</code> or null if the driver could not be found.
	 */
	private DefaultMutableTreeTableNode findNode(DefaultMutableTreeTableNode driverNode, NodeInfoHolder info)
	{
		for (int i=0; i<driverNode.getChildCount(); i++)
		{
			DefaultMutableTreeTableNode node = (DefaultMutableTreeTableNode) driverNode.getChildAt(i);
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
	public synchronized JXTreeTable getTreeTable()
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
}
