/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.ui.monitoring.node.TopologyData;
import org.slf4j.*;

import edu.uci.ics.jung.graph.SparseMultigraph;

/**
 * This class handles operations on the graph.
 * <p>It actually holds 2 graphs:
 * <ul>
 * <li>one, the full graph, which contains the entire set of vertices (representing drviers and nodes)</li>  
 * <li>another one, the displayed graph, where the collapsed nodes are filtered out</li>
 * </ul>  
 * @author Laurent Cohen
 */
public class GraphTopologyHandler
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
   * The underlying graph that keeps track of all drivers and nodes.
   */
  private SparseMultigraph<TopologyData, Number> fullGraph = null;
  /**
   * The graph that is actually displayed and accounts for collapsed vertices.
   */
  private SparseMultigraph<TopologyData, Number> displayGraph = null;
  /**
   * Edge objects as numbers from a sequence.
   */
  private AtomicLong edgeCount = new AtomicLong(0L);
  /**
   * The panel that displays the graph.
   */
  private GraphOption graphOption = null;
  /**
   * 
   */
  private Map<String, TopologyData> drivers = new HashMap<String, TopologyData>();
  /**
   * 
   */
  private Map<String, java.util.List<TopologyData>> driversAsNodes = new HashMap<String, java.util.List<TopologyData>>();

  /**
   * Initialize this graph handler.
   * @param graphOption the panel that displays the graph.
   */
  public GraphTopologyHandler(final GraphOption graphOption)
  {
    this.graphOption = graphOption;
    fullGraph = new SparseMultigraph<TopologyData, Number>();
    displayGraph = new SparseMultigraph<TopologyData, Number>();
  }

  /**
   * Get the underlying graph that keeps track of all drivers and nodes.
   * @return a <code>SparseMultigraph</code> instance.
   */
  public SparseMultigraph<TopologyData, Number> getFullGraph()
  {
    return fullGraph;
  }

  /**
   * Get the graph that is actually displayed and accounts for collapsed vertices.
   * @return a <code>SparseMultigraph</code> instance.
   */
  public SparseMultigraph<TopologyData, Number> getDisplayGraph()
  {
    return displayGraph;
  }

  /**
   * Redraw the graph.
   * @param root the root of the tree from which to populate.
   */
  public void populate(final DefaultMutableTreeNode root)
  {
    if (debugEnabled) log.debug("start populate");
    graphOption.repaintFlag.set(false);
    try
    {
      for (int i=0; i<root.getChildCount(); i++)
      {
        DefaultMutableTreeNode driver = (DefaultMutableTreeNode) root.getChildAt(i);
        TopologyData driverData = (TopologyData) driver.getUserObject();
        driverAdded(driverData);
        for (int j=0; j<driver.getChildCount(); j++)
        {
          DefaultMutableTreeNode child = (DefaultMutableTreeNode) driver.getChildAt(j);
          TopologyData nodeData = (TopologyData) child.getUserObject();
          nodeAdded(driverData, nodeData, null);
        }
      }
    }
    finally
    {
      graphOption.repaintFlag.set(true);
    }
    graphOption.repaintGraph(graphOption.isAutoLayout());
    if (debugEnabled) log.debug("end populate");
  }

  /**
   * Called when a driver was added in the topology.
   * @param driver the data representing the driver.
   */
  public void driverAdded(final TopologyData driver)
  {
    synchronized(drivers)
    {
      if (!drivers.containsKey(driver.getUuid())) drivers.put(driver.getUuid(), driver);
      java.util.List<TopologyData> list = driversAsNodes.get(driver.getUuid());
      if (list != null)
      {
        for (TopologyData tmpDriver: list)
        {
          tmpDriver.setClientConnection(driver.getClientConnection());
          tmpDriver.setJmxWrapper(driver.getJmxWrapper());
        }
        driversAsNodes.remove(driver.getUuid());
      }
    }
    insertDriverVertex(driver);
    graphOption.repaintGraph(graphOption.isAutoLayout());
    if (debugEnabled) log.debug("added driver " + driver + " to graph");
  }

  /**
   * Called when a driver was removed from the topology.
   * @param driver the data representing the driver.
   */
  public void driverRemoved(final TopologyData driver)
  {
    synchronized(drivers)
    {
      drivers.remove(driver.getUuid());
      driversAsNodes.remove(driver.getUuid());
    }
    removeVertex(driver);
    graphOption.repaintGraph(graphOption.isAutoLayout());
    if (debugEnabled) log.debug("removed driver " + driver + " from graph");
  }

  /**
   * Called when a node was added in the topology.
   * @param driver the driver to which the node is added.
   * @param node the data representing the node.
   * @param peerDriver non-null if the node is a peer driver, rpresents the driver it is connected to.
   */
  public void nodeAdded(final TopologyData driver, final TopologyData node, final TopologyData peerDriver)
  {
    synchronized(drivers)
    {
      if (peerDriver != null)
      {
        //TopologyData other = drivers.get(peerDriver.getId());
        node.setClientConnection(peerDriver.getClientConnection());
        node.setJmxWrapper(peerDriver.getJmxWrapper());
        insertPeerVertex(driver, peerDriver);
      }
      else
      {
        java.util.List<TopologyData> list = driversAsNodes.get(node.getUuid());
        if (list == null)
        {
          list = new ArrayList<TopologyData>();
          driversAsNodes.put(node.getUuid(), list);
        }
        list.add(node);
      }
      insertNodeVertex(driver, node);
    }
    graphOption.repaintGraph(graphOption.isAutoLayout());
    if (debugEnabled) log.debug("added " + (node.isNode() ? "node " : "peer driver ") + node + " to driver " + driver);
  }

  /**
   * Called when a node was removed from the topology.
   * @param driver the driver to which the node is added.
   * @param node the data representing the node.
   */
  public void nodeRemoved(final TopologyData driver, final TopologyData node)
  {
    removeVertex(node);
    graphOption.repaintGraph(graphOption.isAutoLayout());
    if (debugEnabled) log.debug("removed node " + node + " from driver " + driver);
  }

  /**
   * Called when the state information of a node has changed.
   * @param driver the driver to which the node is attached.
   * @param node the node to update.
   */
  public void nodeDataUpdated(final TopologyData driver, final TopologyData node)
  {
    if (debugEnabled) log.debug("driver=" + driver + ", node=" + node);
    graphOption.repaintGraph(false);
    //graphOption.repaintGraph(graphOption.isAutoLayout());
  }

  /**
   * Insert a new vertex for a newly added driver.
   * @param driver data for the driver to add.
   * @return the new vertex object.
   */
  TopologyData insertDriverVertex(final TopologyData driver)
  {
    fullGraph.addVertex(driver);
    displayGraph.addVertex(driver);
    return driver;
  }

  /**
   * Insert a new vertex for a newly added node.
   * @param driver vertex representing the driver to which the node is attached.
   * @param node data for the newly added node.
   * @return the new vertex object.
   */
  TopologyData insertNodeVertex(final TopologyData driver, final TopologyData node)
  {
    fullGraph.addVertex(node);
    Number edge = null;
    if (fullGraph.findEdge(driver, node) == null)
    {
      edge = edgeCount.incrementAndGet();
      fullGraph.addEdge(edge, driver, node);
    }
    if (!driver.isCollapsed())
    {
      displayGraph.addVertex(node);
      if (displayGraph.findEdge(driver, node) == null && (edge != null)) displayGraph.addEdge(edge, driver, node);
    }
    return node;
  }

  /**
   * Insert a new vertex for a newly added node.
   * @param driver vertex representing the driver to which the node is attached.
   * @param peer data for the newly added node.
   * @return the new vertex object.
   */
  TopologyData insertPeerVertex(final TopologyData driver, final TopologyData peer)
  {
    //fullGraph.addVertex(peer);
    Number edge = null;
    if (fullGraph.findEdge(driver, peer) == null)
    {
      edge = edgeCount.incrementAndGet();
      fullGraph.addEdge(edge, driver, peer);
    }
    if (displayGraph.findEdge(driver, peer) == null && (edge != null)) displayGraph.addEdge(edge, driver, peer);
    return peer;
  }

  /**
   * Remove the specified vertex and all connecting edges from the graph.
   * @param vertex the vertex to remove.
   */
  void removeVertex(final TopologyData vertex)
  {
    fullGraph.removeVertex(vertex);
    displayGraph.removeVertex(vertex);
  }

  /**
   * Collapse the specified driver by removing attached nodes from the display graph.
   * @param driver the driver to collapse.
   */
  public void collapse(final TopologyData driver)
  {
    if (!driver.isCollapsed() && !driver.isNode())
    {
      driver.setCollapsed(true);
      Collection<TopologyData> neighbors = displayGraph.getNeighbors(driver);
      for (TopologyData data: neighbors)
      {
        if (data.isNode()) displayGraph.removeVertex(data);
      }
    }
  }

  /**
   * Expand the specified driver by adding attached nodes to the display graph.
   * @param driver the driver to expand.
   */
  public void expand(final TopologyData driver)
  {
    if (driver.isCollapsed() && !driver.isNode())
    {
      driver.setCollapsed(false);
      Collection<TopologyData> neighbors = fullGraph.getNeighbors(driver);
      for (TopologyData data: neighbors)
      {
        if (data.isNode())
        {
          displayGraph.addVertex(data);
          Number edge = fullGraph.findEdge(driver, data);
          if (edge != null) displayGraph.addEdge(edge, driver, data);
        }
      }
    }
  }
}
