/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import org.jppf.client.monitoring.topology.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.utils.*;
import org.slf4j.*;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;

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
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The underlying graph that keeps track of all drivers and nodes.
   */
  private SparseMultigraph<AbstractTopologyComponent, Number> fullGraph;
  /**
   * The graph that is actually displayed and accounts for collapsed vertices.
   */
  private SparseMultigraph<AbstractTopologyComponent, Number> displayGraph;
  /**
   * Edge objects as numbers from a sequence.
   */
  private AtomicLong edgeCount = new AtomicLong(0L);
  /**
   * The panel that displays the graph.
   */
  private final GraphOption graphOption;
  /**
   * Mapping of drivers to their uuid.
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
   * Collapsed or expanded state of the vertices.
   */
  enum CollapsedState {
    /**
     * Collapsed state.
     */
    COLLAPSED,
    /**
     * Expanded state.
     */
    EXPANDED
  }
  /**
   * Holds the collapsed/expanded states of the vertices.
   */
  private final Map<String, CollapsedState> collapsedMap = new HashMap<>();

  /**
   * Initialize this graph handler.
   * @param graphOption the panel that displays the graph.
   */
  public GraphTopologyHandler(final GraphOption graphOption) {
    manager = StatsHandler.getInstance().getTopologyManager();
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
      for (final TopologyDriver driver: manager.getDrivers()) {
        driverAdded(new TopologyEvent(manager, driver, null, TopologyEvent.UpdateType.TOPOLOGY));
        for (final AbstractTopologyComponent child: driver.getChildren()) {
          final TopologyNode node = (TopologyNode) child;
          log.debug(String.format("adding %s %s to driver %s", (node.isNode() ? "node" : "peer"), node, driver));
          nodeAdded(new TopologyEvent(manager, driver, node, TopologyEvent.UpdateType.TOPOLOGY));
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
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        final TopologyDriver driver = event.getDriver();
        synchronized(drivers) {
          if (!drivers.containsKey(driver.getUuid())) drivers.put(driver.getUuid(), driver);
          final List<TopologyDriver> list = driversAsNodes.get(driver.getUuid());
          if (list != null) {
            for (final TopologyDriver tmpDriver: list) insertPeerVertex(driver, tmpDriver);
            driversAsNodes.remove(driver.getUuid());
          }
        }
        synchronized(collapsedMap) {
          collapsedMap.put(driver.getUuid(),  CollapsedState.EXPANDED);
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
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        final TopologyDriver driver = event.getDriver();
        synchronized(drivers) {
          drivers.remove(driver.getUuid());
          driversAsNodes.remove(driver.getUuid());
        }
        synchronized(collapsedMap) {
          collapsedMap.remove(driver.getUuid());
        }
        removeVertex(driver);
        graphOption.repaintGraph(graphOption.isAutoLayout());
        if (debugEnabled) log.debug("removed driver " + driver + " from graph");
      }
    };
    SwingUtilities.invokeLater(r);
  }

  @Override
  public void driverUpdated(final TopologyEvent event) {
  }

  @Override
  public void nodeAdded(final TopologyEvent event) {
    SwingUtilities.invokeLater(new NodeAdded(event));
  }

  @Override
  public void nodeRemoved(final TopologyEvent event) {
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        final TopologyDriver driver = event.getDriver();
        final TopologyNode node = event.getNodeOrPeer();
        removeVertex(node);
        synchronized(collapsedMap) {
          collapsedMap.remove(node.getUuid());
        }
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
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        final TopologyDriver driver = event.getDriver();
        final TopologyNode node = event.getNodeOrPeer();
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
    if (isExpanded(driver)) {
      displayGraph.addVertex(node);
      if (displayGraph.findEdge(driver, node) == null && (edge != null)) displayGraph.addEdge(edge, driver, node);
    }
    return node;
  }

  /**
   * Insert a new vertex for a newly added node.
   * @param master vertex representing the driver to which the node is attached.
   * @param slave data for the newly added node.
   * @return the new vertex object.
   */
  TopologyNode insertMasterSlaveVertex(final TopologyNode master, final TopologyNode slave) {
    //fullGraph.addVertex(slave);
    Number edge = null;
    if ((edge = fullGraph.findEdge(slave, master)) == null) {
      edge = edgeCount.incrementAndGet();
      fullGraph.addEdge(edge, slave, master, EdgeType.DIRECTED);
    }
    if (graphOption.isShowMasterSlaveRelationShip()) {
      if (isExpanded(master.getDriver()) && isExpanded(slave.getDriver())) {
        if (displayGraph.findEdge(slave, master) == null) displayGraph.addEdge(edge, slave, master, EdgeType.DIRECTED);
      }
    }
    return slave;
  }

  /**
   * Insert a new vertex for a newly added node.
   * @param driver vertex representing the driver to which the node is attached.
   * @param peer data for the newly added node.
   * @return the new vertex object.
   */
  TopologyDriver insertPeerVertex(final TopologyDriver driver, final TopologyDriver peer) {
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
    if (!isCollapsed(driver) && !driver.isNode()) {
      synchronized(collapsedMap) {
        collapsedMap.put(driver.getUuid(),  CollapsedState.COLLAPSED);
      }
      final Collection<AbstractTopologyComponent> neighbors = displayGraph.getNeighbors(driver);
      for (final AbstractTopologyComponent data: neighbors) {
        if (data.isNode()) displayGraph.removeVertex(data);
      }
    }
  }

  /**
   * Expand the specified driver by adding attached nodes to the display graph.
   * @param driver the driver to expand.
   */
  public void expand(final TopologyDriver driver) {
    if (!isExpanded(driver) && !driver.isNode()) {
      synchronized(collapsedMap) {
        collapsedMap.put(driver.getUuid(),  CollapsedState.EXPANDED);
      }
      final Collection<AbstractTopologyComponent> neighbors = fullGraph.getNeighbors(driver);
      for (final AbstractTopologyComponent data: neighbors) {
        if (data.isNode()) {
          displayGraph.addVertex(data);
          final Number edge = fullGraph.findEdge(driver, data);
          if (edge != null) displayGraph.addEdge(edge, driver, data);
          final JPPFManagementInfo info = data.getManagementInfo();
          if (info.isMasterNode()) {
            final List<TopologyNode> slaves = manager.getSlaveNodes(data.getUuid());
            for (final TopologyNode slave: slaves) insertMasterSlaveVertex((TopologyNode) data, slave);
          } else if (info.isSlaveNode() && (info.getMasterUuid() != null)) {
            final TopologyNode master = manager.getNode(info.getMasterUuid());
            insertMasterSlaveVertex(master, (TopologyNode) data);
          }
        }
      }
    }
  }

  /**
   * Determine whether the specified component is collapsed.
   * @param comp the topology component to check.
   * @return {@code true} if the node is collapsed, {@code false} otherwise.
   */
  public boolean isCollapsed(final AbstractTopologyComponent comp) {
    synchronized(collapsedMap) {
      return collapsedMap.get(comp.getUuid()) == CollapsedState.COLLAPSED;
    }
  }

  /**
   * Determine whether the specified component is expanded.
   * @param comp the topology component to check.
   * @return {@code true} if the node is expanded, {@code false} otherwise.
   */
  public boolean isExpanded(final AbstractTopologyComponent comp) {
    synchronized(collapsedMap) {
      return collapsedMap.get(comp.getUuid()) == CollapsedState.EXPANDED;
    }
  }

  /**
   * Get the vertices that are the endpoints of the specified edge.
   * @param edge the edge number.
   * @return a pair of endpoints.
   */
  public Pair<AbstractTopologyComponent, AbstractTopologyComponent> getVertices(final Number edge) {
    final edu.uci.ics.jung.graph.util.Pair<AbstractTopologyComponent> pair = fullGraph.getEndpoints(edge);
    return new Pair<>(pair.getFirst(), pair.getSecond());
  }

  /**
   * Add the edges that represent master/slave node relationships.
   */
  void addMasterSlaveEdges() {
    final TopologyManager manager = StatsHandler.getInstance().getTopologyManager();
    for (TopologyNode node: manager.getNodes()) {
      if (node.getManagementInfo().isMasterNode()) {
        final List<TopologyNode> slaves = manager.getSlaveNodes(node.getUuid());
        for (final TopologyNode slave: slaves) insertMasterSlaveVertex(node, slave);
      }
    }
  }

  /**
   * Remove the edges that represent master/slave node relationships.
   */
  void removeMasterSlaveEdges() {
    try {
      final List<Number> edges = new ArrayList<>(displayGraph.getEdges(EdgeType.DIRECTED));
      for (final Number edge: edges) displayGraph.removeEdge(edge);
    } catch(final Exception e) {
      e.printStackTrace();
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
      final TopologyDriver driver = event.getDriver();
      final TopologyNode node = event.getNodeOrPeer();
      synchronized(drivers) {
        if (node.isPeer()) {
          final TopologyPeer peer = node.isPeer() ? (TopologyPeer) node : null;
          final TopologyDriver actualDriver = drivers.get(peer.getUuid());
          if (actualDriver != null) insertPeerVertex(driver, actualDriver);
          else {
            List<TopologyDriver> list = driversAsNodes.get(peer.getUuid());
            if (list == null) {
              list = new ArrayList<>();
              driversAsNodes.put(peer.getUuid(), list);
            }
            list.add(driver);
          }
        } else {
          insertNodeVertex(driver, node);
          final JPPFManagementInfo info = node.getManagementInfo();
          if (info.isMasterNode()) {
            final List<TopologyNode> slaves = event.getTopologyManager().getSlaveNodes(node.getUuid());
            for (final TopologyNode slave: slaves) insertMasterSlaveVertex(node, slave);
          } else if (info.isSlaveNode() && (info.getMasterUuid() != null)) {
            final TopologyNode master = event.getTopologyManager().getNode(info.getMasterUuid());
            insertMasterSlaveVertex(master, node);
          }
        }
      }
      synchronized(collapsedMap) {
        collapsedMap.put(node.getUuid(), CollapsedState.EXPANDED);
      }
      graphOption.repaintGraph(graphOption.isAutoLayout());
      if (debugEnabled) log.debug(String.format("added %s %s to driver %s ", (node.isNode() ? "node" : "peer"), node, driver));
    }
  };
}
