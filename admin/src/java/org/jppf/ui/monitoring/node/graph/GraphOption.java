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
	 * 
	 */
	protected Map<String, mxCell> groupsMap = new HashMap<String, mxCell>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setEnabled(final boolean enabled)
	{
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setupValueChangeNotifications()
	{
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void createUI()
	{
		graph = new JPPFGraph();
		graphComponent =  new mxGraphComponent(graph);
		graphComponent.getViewport().setOpaque(true);
		graphComponent.getViewport().setBackground(Color.WHITE);
		graphComponent.setDragEnabled(false);
		layout = new mxCompactTreeLayout(graph);
		//graphComponent.set
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
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
			Map<String, mxCell> vertices = new HashMap<String, mxCell>();
			for (int i=0; i<root.getChildCount(); i++)
			{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
				TopologyData data = (TopologyData) child.getUserObject();
				String id = data.getJmxWrapper().getId();
				drivers.put(id, child);
				mxCell vertex = insertDriverVertex(data);
				vertices.put(id, vertex);
				if (debugEnabled) log.debug("added vertex for " + id);
			}
			for (int i=0; i<root.getChildCount(); i++)
			{
				DefaultMutableTreeNode driver = (DefaultMutableTreeNode) root.getChildAt(i);
				TopologyData driverData = (TopologyData) driver.getUserObject();
				String id = driverData.getJmxWrapper().getId();
				mxCell v1 = vertices.get(id);
				for (int j=0; j<driver.getChildCount(); j++)
				{
					DefaultMutableTreeNode child = (DefaultMutableTreeNode) driver.getChildAt(j);
					TopologyData childData = (TopologyData) child.getUserObject();
					String childId = childData.getJmxWrapper().getId();
					JPPFManagementInfo info = childData.getNodeInformation();
					if (info == null) continue;
					mxCell v2 = insertNodeVertex(v1, childData);
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
	public void setTreeTableOption(final AbstractTreeTableOption treeTableOption)
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
	public void setLayout(final String name)
	{
		layout = new LayoutFactory(graph).createLayout(name);
		layout.execute(graph.getDefaultParent());
		//if (treeTableOption != null) populate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
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
		actionHandler.putAction("graph.show.information", new SystemInformationAction());
		actionHandler.putAction("graph.update.threads", new NodeThreadsAction());
		actionHandler.putAction("graph.reset.counter", new ResetTaskCounterAction());
		actionHandler.putAction("graph.restart.node", new RestartNodeAction());
		actionHandler.putAction("graph.shutdown.node", new ShutdownNodeAction());
		actionHandler.putAction("graph.select.drivers", new SelectGraphDriversAction(this));
		actionHandler.putAction("graph.select.nodes", new SelectGraphNodesAction(this));
		actionHandler.putAction("graph.button.collapse", new ExpandOrCollapseGraphAction(this, true));
		actionHandler.putAction("graph.button.expand", new ExpandOrCollapseGraphAction(this, false));
		actionHandler.updateActions();
		//treeTable.addMouseListener(new NodeTreeTableMouseListener(actionHandler));
		Runnable r = new ActionsInitializer(this, "/graph.topology.toolbar");
		new Thread(r).start();
	}

	/**
	 * Called when a driver was added in the topology.
	 * @param driver the data representing the driver.
	 */
	public void driverAdded(final TopologyData driver)
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
	public void driverRemoved(final TopologyData driver)
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
				graph.getView().invalidate();
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
	public void nodeAdded(final TopologyData driver, final TopologyData node)
	{
		String id = driver.getId();
		mxGraphModel model = (mxGraphModel) graph.getModel();
		Object parent = graph.getDefaultParent();
		model.beginUpdate();
		try
		{
			mxCell v1 = (mxCell) model.getCell(id);
			if (v1 != null)
			{
				insertNodeVertex(v1, node);
				if (debugEnabled)
				{
					String s = node.getNodeInformation().isDriver() ? "peer driver" : "node";
					log.debug("added " + s + ' ' + node.getId() + " to driver " + id);
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
	public void nodeRemoved(final TopologyData driver, final TopologyData node)
	{
		mxGraphModel model = (mxGraphModel) graph.getModel();
		model.beginUpdate();
		try
		{
			mxCell vertex = (mxCell) model.getCell(node.getId());
			if (vertex != null)
			{
				for (int i=0; i<vertex.getEdgeCount(); i++) model.remove(vertex.getEdgeAt(i));
				model.remove(vertex);
			}
			Object driverVertex = model.getCell(driver.getId());
			graph.getView().invalidate(driverVertex);
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
	public void nodeDataUpdated(final TopologyData driver, final TopologyData node)
	{
	}

	/**
	 * Insert a new vertex for a newly added driver.
	 * @param driver data for the driver to add.
	 * @return the new vertex object.
	 */
	private mxCell insertDriverVertex(final TopologyData driver)
	{
		StringBuilder style = new StringBuilder();
		style.append("shape=image;image=/org/jppf/ui/resources/mainframe.gif");
		style.append(";verticalLabelPosition=bottom");
		mxCell vertex = (mxCell) graph.insertVertex(graph.getDefaultParent(), driver.getId(), driver, 0.0, 0.0, 30.0, 20.0, style.toString());
		vertex.setCollapsed(false);
		if (layout != null)
		{
			layout.execute(vertex);
		}
		return vertex;
	}

	/**
	 * Insert a new vertex for a newly added node.
	 * @param driverVertex vertex representing the driver to which the node is attached.
	 * @param node data for the newly added node.
	 * @return the new vertex object.
	 */
	private mxCell insertNodeVertex(final mxCell driverVertex, final TopologyData node)
	{
		mxCell nodeVertex;
		if (node.getNodeInformation().isDriver())
		{
			nodeVertex = (mxCell) ((mxGraphModel) graph.getModel()).getCell(node.getId());
			mxCell edge = (mxCell) graph.insertEdge(driverVertex, null, null, driverVertex, nodeVertex);
		}
		else
		{
			StringBuilder style = new StringBuilder();
			style.append("shape=image;image=/org/jppf/ui/resources/buggi_server.gif");
			style.append(";verticalLabelPosition=bottom");
			nodeVertex = (mxCell) graph.insertVertex(driverVertex, node.getId(), node, 0.0, 0.0, 20.0, 20.0, style.toString());
			mxCell edge = (mxCell) graph.insertEdge(driverVertex, null, null, driverVertex, nodeVertex);
			nodeVertex.setParent(edge);
		}
		if (layout != null)
		{
			layout.execute(driverVertex);
		}
		return nodeVertex;
	}

	/**
	 * Get the displayed graph.
	 * @return a <code>mxGraph</code> instance.
	 */
	public mxGraph getGraph()
	{
		return graph;
	}
}
