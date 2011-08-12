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

package org.jppf.ui.monitoring.node.graph;

import java.awt.Color;
import java.util.*;

import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.management.JPPFManagementInfo;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.node.TopologyData;
import org.jppf.ui.monitoring.node.actions.*;
import org.jppf.ui.options.AbstractOption;
import org.jppf.ui.treetable.AbstractTreeTableOption;
import org.slf4j.*;

import com.mxgraph.layout.*;
import com.mxgraph.model.*;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

/**
 * 
 * @author Laurent Cohen
 */
public class GraphOption extends AbstractOption implements ActionHolder
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(GraphOption.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The graph to display.
	 */
	protected mxGraph graph = null;
	/**
	 * The graph component.
	 */
	protected mxGraphComponent graphComponent = null;
	/**
	 * The tree view.
	 */
	protected AbstractTreeTableOption treeTableOption = null;
	/**
	 * The graph layout.
	 */
	protected mxIGraphLayout layout = null;
	/**
	 * Manages the actions for this graph.
	 */
	protected ActionHandler actionHandler = null;

	/**
	 * {@inheritDoc}
	 */
	public void setEnabled(boolean enabled)
	{
	}

	/**
	 * {@inheritDoc}
	 */
	protected void setupValueChangeNotifications()
	{
	}

	/**
	 * {@inheritDoc}
	 */
	public void createUI()
	{
		graph = new mxGraph();
		graphComponent =  new mxGraphComponent(graph);
		graphComponent.getViewport().setOpaque(true);
		graphComponent.getViewport().setBackground(Color.WHITE);
		graphComponent.setDragEnabled(false);
		layout = new mxCompactTreeLayout(graph);
		graph.getSelectionModel().setSingleSelection(false);
		graph.getSelectionModel().setEventsEnabled(true);
		graph.setDisconnectOnMove(false);
		graph.setCellsEditable(false);
		graph.setCellsResizable(false);
		graph.setCellsSelectable(true);
		//graphComponent.set
	}

	/**
	 * {@inheritDoc}
	 */
	public JComponent getUIComponent()
	{
		return graphComponent;
	}

	/**
	 * Redraw the graph.
	 */
	public void populate()
	{
		Object parent = graph.getDefaultParent();
		graph.getModel().beginUpdate();
		try
		{
			graph.selectAll();
			graph.removeCells();
		}
		finally
		{
			graph.getModel().endUpdate();
		}
		graph.getModel().beginUpdate();
		DefaultMutableTreeNode root = treeTableOption.getTreeTableRoot();
		try
		{
			Map<String, DefaultMutableTreeNode> drivers = new HashMap<String, DefaultMutableTreeNode>();
			Map<String, Object> vertices = new HashMap<String, Object>();
			for (int i=0; i<root.getChildCount(); i++)
			{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
				TopologyData data = (TopologyData) child.getUserObject();
				String id = data.getJmxWrapper().getId();
				drivers.put(id, child);
				Object vertex = insertDriverVertex(data);
				vertices.put(id, vertex);
				if (debugEnabled) log.debug("added vertex for " + id);
			}
			for (int i=0; i<root.getChildCount(); i++)
			{
				DefaultMutableTreeNode driver = (DefaultMutableTreeNode) root.getChildAt(i);
				TopologyData driverData = (TopologyData) driver.getUserObject();
				String id = driverData.getJmxWrapper().getId();
				Object v1 = vertices.get(id);
				for (int j=0; j<driver.getChildCount(); j++)
				{
					DefaultMutableTreeNode child = (DefaultMutableTreeNode) driver.getChildAt(j);
					TopologyData childData = (TopologyData) child.getUserObject();
					String childId = childData.getJmxWrapper().getId();
					JPPFManagementInfo info = childData.getNodeInformation();
					if (info == null) continue;
					if (JPPFManagementInfo.DRIVER == info.getType())
					{
						Object v2 = vertices.get(childId);
						graph.insertEdge(parent, null, null, v1, v2);
						if (debugEnabled) log.debug("inserted edge between " + id + " and " + childId);
					}
					else
					{
						Object v2 = insertNodeVertex(v1, childData);
						if (debugEnabled) log.debug("added vertex for " + childId);
						graph.insertEdge(parent, null, null, v1, v2);
						if (debugEnabled) log.debug("inserted edge between " + id + " and " + childId);
					}
						
				}
			}
			layout.execute(parent);
		}
		finally
		{
			graph.getModel().endUpdate();
		}
	}

	/**
	 * Set the corresponding tree view onto this graph.
	 * @param treeTableOption a {@link AbstractTreeTableOption} instance.
	 */
	public void setTreeTableOption(AbstractTreeTableOption treeTableOption)
	{
		this.treeTableOption = treeTableOption;
	}

	/**
	 * Get the current layout.
	 * @return the layout.
	 */
	public mxIGraphLayout getLayout()
	{
		return layout;
	}

	/**
	 * Set the current layout.
	 * @param name the layout name.
	 */
	public void setLayout(String name)
	{
		layout = new LayoutFactory(graph).createLayout(name);
		layout.execute(graph.getDefaultParent());
		//if (treeTableOption != null) populate();
	}

	/**
	 * {@inheritDoc}
	 */
	public ActionHandler getActionHandler()
	{
		return actionHandler;
	}

	/**
	 * Initialize all actions used in the panel.
	 */
	public void setupActions()
	{
		actionHandler = new GraphActionHandler(graph);
		actionHandler.putAction("graph.shutdown.restart.driver", new ServerShutdownRestartAction());
		actionHandler.putAction("graph.driver.reset.statistics", new ServerStatisticsResetAction());
		actionHandler.putAction("graph.update.configuration", new NodeConfigurationAction());
		actionHandler.putAction("graph.show.information", new NodeInformationAction());
		actionHandler.putAction("graph.update.threads", new NodeThreadsAction());
		actionHandler.putAction("graph.reset.counter", new ResetTaskCounterAction());
		actionHandler.putAction("graph.restart.node", new RestartNodeAction());
		actionHandler.putAction("graph.shutdown.node", new ShutdownNodeAction());
		//actionHandler.putAction("select.drivers", new SelectDriversAction(this));
		//actionHandler.putAction("select.nodes", new SelectNodesAction(this));
		actionHandler.updateActions();
		//treeTable.addMouseListener(new NodeTreeTableMouseListener(actionHandler));
		Runnable r = new ActionsInitializer(this, "/graph.topology.toolbar");
		new Thread(r).start();
	}

	/**
	 * Called when a driver was added in the topology.
	 * @param driver the data representing the driver.
	 */
	public void driverAdded(TopologyData driver)
	{
		String id = driver.getId();
		Object parent = graph.getDefaultParent();
		graph.getModel().beginUpdate();
		try
		{
			Object vertex = insertDriverVertex(driver);
			if (debugEnabled) log.debug("added driver " + id + " to graph");
		}
		catch(Throwable t)
		{
			log.info(t.getMessage(), t);
		}
		finally
		{
			graph.getModel().endUpdate();
		}
	}

	/**
	 * Called when a driver was removed from the topology.
	 * @param driver the data representing the driver.
	 */
	public void driverRemoved(TopologyData driver)
	{
		String id = driver.getId();
		mxGraphModel model = (mxGraphModel) graph.getModel();
		model.beginUpdate();
		try
		{
			mxCell vertex = (mxCell) model.getCell(id);
			if (vertex != null)
			{
				for (int i=0; i<vertex.getEdgeCount(); i++) model.remove(vertex.getEdgeAt(i));
				model.remove(vertex);
				if (debugEnabled) log.debug("removed driver " + id + " from graph");
			}
		}
		catch(Throwable t)
		{
			log.info(t.getMessage(), t);
		}
		finally
		{
			model.endUpdate();
		}
	}

	/**
	 * Called when a node was added in the topology.
	 * @param driver the driver to which the node is added.
	 * @param node the data representing the node.
	 */
	public void nodeAdded(TopologyData driver, TopologyData node)
	{
		String id = driver.getId();
		mxGraphModel model = (mxGraphModel) graph.getModel();
		Object parent = graph.getDefaultParent();
		model.beginUpdate();
		try
		{
			Object v1 = model.getCell(id);
			if (v1 != null)
			{
				if (node.getNodeInformation().isDriver())
				{
					Object v2 = model.getCell(node.getId());
					graph.insertEdge(parent, null, null, v1, v2);
					if (debugEnabled) log.debug("linking driver " + id + " to peer driver " + node.getId());
				}
				else
				{
					insertNodeVertex(v1, node);
					if (debugEnabled) log.debug("added node " + node.getId() + " to driver " + id);
				}
			}
		}
		catch(Throwable t)
		{
			log.info(t.getMessage(), t);
		}
		finally
		{
			model.endUpdate();
		}
	}

	/**
	 * Called when a node was removed from the topology.
	 * @param driver the driver to which the node is added.
	 * @param node the data representing the node.
	 */
	public void nodeRemoved(TopologyData driver, TopologyData node)
	{
		String id = node.getId();
		mxGraphModel model = (mxGraphModel) graph.getModel();
		model.beginUpdate();
		try
		{
			mxCell vertex = (mxCell) model.getCell(id);
			if (vertex != null)
			{
				for (int i=0; i<vertex.getEdgeCount(); i++) model.remove(vertex.getEdgeAt(i));
				model.remove(vertex);
			}
			if (debugEnabled) log.debug("removed node " + node.getId() + " from driver " + driver.getId());
		}
		catch(Throwable t)
		{
			log.info(t.getMessage(), t);
		}
		finally
		{
			model.endUpdate();
		}
	}

	/**
	 * Called when the state information of a node has changed.
	 * @param driver the driver to which the node is attached.
	 * @param node the node to update.
	 */
	public void nodeDataUpdated(TopologyData driver, TopologyData node)
	{
	}

	/**
	 * Insert a new vertex for a newly added driver.
	 * @param driver data for the driver to add.
	 * @return the new vertex object.
	 */
	private Object insertDriverVertex(TopologyData driver)
	{
		StringBuilder style = new StringBuilder();
		style.append("shape=image;image=/org/jppf/ui/resources/mainframe.gif");
		style.append(";verticalLabelPosition=bottom;label='").append(driver.getId()).append("'");
		Object vertex = graph.insertVertex(graph.getDefaultParent(), driver.getId(), driver, 0, 0, 40, 30, style.toString());
		if (layout != null) layout.execute(vertex);
		return vertex;
	}

	/**
	 * Insert a new vertex for a newly added node.
	 * @param driverVertex vertex representing the driver to which the node is attached.
	 * @param node data for the newly added node.
	 * @return the new vertex object.
	 */
	private Object insertNodeVertex(Object driverVertex, TopologyData node)
	{
		StringBuilder style = new StringBuilder();
		style.append("shape=image;image=/org/jppf/ui/resources/buggi_server.gif");
		style.append(";verticalLabelPosition=bottom;label='").append(node.getId()).append("'");
		Object nodeVertex = graph.insertVertex(graph.getDefaultParent(), node.getId(), node, 0, 0, 40, 30, style.toString());
		graph.insertEdge(graph.getDefaultParent(), null, null, driverVertex, nodeVertex);
		if (layout != null) layout.execute(nodeVertex);
		return nodeVertex;
	}
}
