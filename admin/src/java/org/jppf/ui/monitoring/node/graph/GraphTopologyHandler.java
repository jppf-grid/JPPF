/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import javax.swing.SwingUtilities;

import org.jppf.ui.monitoring.topology.*;
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
public class GraphTopologyHandler implements TopologyListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(GraphTopologyHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The underlying graph that keeps track of all drivers and nodes.
   */
  private SparseMultigraph<AbstractTopologyComponent, Number> fullGraph = null;
  /**
   * The graph that is actually displayed and accounts for collapsed vertices.
   */
  private SparseMultigraph<AbstractTopologyComponent, Number> displayGraph = null;
  /**
   * Edge objects as numbers from a sequence.
   */
  private AtomicLong edgeCount = new AtomicLong(0L);
  /**
   * The panel that displays the graph.
   */
  private final GraphOption graphOption;
  /**
   * 
   */
  private Map<String, TopologyDriver> drivers = new HashMap<>();
  /**
   * 
   */
  private Map<String, List<TopologyDriver>> driversAsNodes = new HashMap<>();
  /**
   * Manages the topology updates.
   */
  private final TopologyManager manager;

  /**
   * Initialize this graph handler.
   * @param graphOption the panel that displays the graph.
   */
  public GraphTopologyHandler(final GraphOption graphOption) {
    manager = TopologyManager.getInstance();
    this.graphOption = graphOption;
    fullGraph = new SparseMultigraph<>();
    displayGraph = new SparseMultigraph<>();
  }

  /**
   * Get the underlying graph that keeps track of all drivers and nodes.
   * @return a <code>SparseMultigraph</code> instance.
   */
  public SparseMultigraph<AbstractTopologyComponent, Number> getFullGraph() {
    return fullGraph;
  }

  /**
   * Get the graph that is actually displayed and accounts for collapsed vertices.
   * @return a <code>SparseMultigraph</code> instance.
   */
  public SparseMultigraph<AbstractTopologyComponent, Number> getDisplayGraph() {
    return displayGraph;
  }

  /**
   * Redraw the graph.
   */
  public void populate() {
    if (debugEnabled) log.debug("start populate");
    graphOption.repaintFlag.set(false);
    try {
      for (TopologyDriver driver: manager.getDrivers()) {
        driverAdded(new TopologyEvent(manager, driver, null, null));
        for (AbstractTopologyComponent child: driver.getChildren()) {
          TopologyNode node = (TopologyNode) child;
          if (node.isNode()) {
            log.debug("adding node " + node + " to driver " + driver);
            nodeAdded(new TopologyEvent(manager, driver, node, null));
          } else {
            log.debug("adding peer " + node + " to driver " + driver);
            nodeAdded(new TopologyEvent(manager, driver, null, (TopologyPeer) node));
          }
        }
      }
    } finally {
      graphOption.repaintFlag.set(true);
    }
    graphOption.repaintGraph(graphOption.isAutoLayout());
    if (debugEnabled) log.debug("end populate");
  }

  @Override
  public void driverAdded(final TopologyEvent event) {
    Runnable r = new Runnable() {
      @Override
      public void run() {
        TopologyDriver driver = event.getDriverData();
        synchronized(drivers) {
          if (!drivers.containsKey(driver.getUuid())) drivers.put(driver.getUuid(), driver);
          List<TopologyDriver> list = driversAsNodes.get(driver.getUuid());
          if (list != null) {
            for (TopologyDriver tmpDriver: list) {
              insertPeerVertex(driver, tmpDriver);
            }
            driversAsNodes.remove(driver.getUuid());
          }
        }
        insertDriverVertex(driver);
        graphOption.repaintGraph(graphOption.isAutoLayout());
        if (debugEnabled) log.debug("added driver " + driver + " to graph");
      }
    };
    SwingUtilities.invokeLater(r);
  }

  @Override
  public void driverRemoved(final TopologyEvent event) {
    Runnable r = new Runnable() {
      @Override
      public void run() {
        TopologyDriver driver = event.getDriverData();
        synchronized(drivers) {
          drivers.remove(driver.getUuid());
          driversAsNodes.remove(driver.getUuid());
        }
        removeVertex(driver);
        graphOption.repaintGraph(graphOption.isAutoLayout());
        if (debugEnabled) log.debug("removed driver " + driver + " from graph");
      }
    };
    SwingUtilities.invokeLater(r);
  }

  @Override
  public void nodeAdded(final TopologyEvent event) {
    SwingUtilities.invokeLater(new NodeAdded(event));
  }

  @Override
  public void nodeRemoved(final TopologyEvent event) {
    Runnable r = new Runnable() {
      @Override
      public void run() {
        TopologyDriver driver = event.getDriverData();
        TopologyNode node = event.getNodeData();
        removeVertex(node);
        graphOption.repaintGraph(graphOption.isAutoLayout());
        if (debugEnabled) log.debug("removed node " + node + " from driver " + driver);
      }
    };
    SwingUtilities.invokeLater(r);
  }

  /**
   * Called when the state information of a node has changed.
   * {@inheritDoc}
   */
  @Override
  public void nodeUpdated(final TopologyEvent event) {
    Runnable r = new Runnable() {
      @Override
      public void run() {
        TopologyDriver driver = event.getDriverData();
        TopologyNode node = event.getNodeData();
        if (debugEnabled) log.debug("driver=" + driver + ", node=" + node);
        graphOption.repaintGraph(false);
      }
    };
    SwingUtilities.invokeLater(r);
  }

  /**
   * Insert a new vertex for a newly added driver.
   * @param driver data for the driver to add.
   * @return the new vertex object.
   */
  TopologyDriver insertDriverVertex(final TopologyDriver driver) {
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
  TopologyNode insertNodeVertex(final TopologyDriver driver, final TopologyNode node) {
    fullGraph.addVertex(node);
    Number edge = null;
    if (fullGraph.findEdge(driver, node) == null) {
      edge = edgeCount.incrementAndGet();
      fullGraph.addEdge(edge, driver, node);
    }
    if (!driver.isCollapsed()) {
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
  TopologyDriver insertPeerVertex(final TopologyDriver driver, final TopologyDriver peer) {
    //fullGraph.addVertex(peer);
    Number edge = null;
    if (fullGraph.findEdge(driver, peer) == null) {
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
  void removeVertex(final AbstractTopologyComponent vertex) {
    fullGraph.removeVertex(vertex);
    displayGraph.removeVertex(vertex);
  }

  /**
   * Collapse the specified driver by removing attached nodes from the display graph.
   * @param driver the driver to collapse.
   */
  public void collapse(final TopologyDriver driver) {
    if (!driver.isCollapsed() && !driver.isNode())
    {
      driver.setCollapsed(true);
      Collection<AbstractTopologyComponent> neighbors = displayGraph.getNeighbors(driver);
      for (AbstractTopologyComponent data: neighbors) {
        if (data.isNode()) displayGraph.removeVertex(data);
      }
    }
  }

  /**
   * Expand the specified driver by adding attached nodes to the display graph.
   * @param driver the driver to expand.
   */
  public void expand(final TopologyDriver driver) {
    if (driver.isCollapsed() && !driver.isNode()) {
      driver.setCollapsed(false);
      Collection<AbstractTopologyComponent> neighbors = fullGraph.getNeighbors(driver);
      for (AbstractTopologyComponent data: neighbors) {
        if (data.isNode()) {
          displayGraph.addVertex(data);
          Number edge = fullGraph.findEdge(driver, data);
          if (edge != null) displayGraph.addEdge(edge, driver, data);
        }
      }
    }
  }

  /**
   * Action run when a new node is added. 
   */
  private class NodeAdded implements Runnable {
    /**
     * The event that encapsulates information about the added node.
     */
    private final TopologyEvent event;

    /**
     * Initialize this action with the specified event.
     * @param event the event that encapsulates information about the added node.
     */
    public NodeAdded(final TopologyEvent event) {
      this.event = event;
    }

    @Override
    public void run() {
      TopologyDriver driver = event.getDriverData();
      TopologyNode node = event.getNodeData();
      TopologyPeer peer = event.getPeerData();
      synchronized(drivers) {
        if (peer != null) {
          TopologyDriver actualDriver = drivers.get(peer.getUuid());
          if (actualDriver != null) insertPeerVertex(driver, actualDriver);
          else {
            List<TopologyDriver> list = driversAsNodes.get(peer.getUuid());
            if (list == null) {
              list = new ArrayList<>();
              driversAsNodes.put(peer.getUuid(), list);
            }
            list.add(driver);
          }
        } else if (node.isNode()) {
          insertNodeVertex(driver, node);
        }
      }
      graphOption.repaintGraph(graphOption.isAutoLayout());
      if (debugEnabled) {
        if (node != null) log.debug("added node " + node + " to driver " + driver);
        else log.debug("added peer " + peer + " to driver " + driver);
      }
    }
  };
}
