/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.client.monitoring.topology;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.management.NodeSelector;
import org.jppf.utils.concurrent.JPPFThreadFactory;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Instances of this class discover and maintain a representation of a JPPF grid topology.
 * @author Laurent Cohen
 * @since 5.0
 */
public class TopologyManager extends ConnectionPoolListenerAdapter implements AutoCloseable {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(TopologyManager.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Mapping of driver uuids to the corresponding {@link TopologyDriver} objects.
   */
  private final Map<String, TopologyDriver> driverMap = new Hashtable<>();
  /**
   * Mapping of peer driver uuids to the corresponding {@link TopologyPeer} objects.
   */
  private final Map<String, TopologyPeer> peerMap = new Hashtable<>();
  /**
   * Mapping of node uuids to the corresponding {@link TopologyNode} objects.
   */
  private final Map<String, TopologyNode> nodeMap = new Hashtable<>();
  /**
   * Mapping of the driver connections to their assocated status listener.
   */
  private final Map<JPPFClientConnection, ClientConnectionStatusListener> statusListenerMap = new Hashtable<>();
  /**
   * List of listeners to the changes in the topology.
   */
  private final List<TopologyListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * Separate thread used to sequentialize events emitted by this topology manager.
   */
  private ExecutorService executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("TopologyEvents"));
  /**
   * The JPPF client.
   */
  private final JPPFClient client;
  /**
   * Refreshes the states of the nodes at rehular intervals.
   */
  final NodeRefreshHandler refreshHandler;
  /**
   * Refreshes the latests JVM health snapshots of the drivers and nodes at rehular intervals.
   */
  final JVMHealthRefreshHandler jvmHealthRefreshHandler;
  /**
   * Use to filter the nodes and associated events.
   * @since 5.2
   */
  private NodeSelector nodeFilter;

  /**
   * Initialize this topology manager with a new {@link JPPFClient} and the specified listeners.
   * The refresh intervals are determined from the configuration, or take a default value of 1000L if they are not configured.
   * @param listeners a set of listeners to subscribe immediately for topology events.
   */
  public TopologyManager(final TopologyListener...listeners) {
    this(null, listeners);
  }

  /**
   * Initialize this topology manager with a new {@link JPPFClient} and the specified listeners.
   * @param topologyRefreshInterval the interval in millis between refreshes of the topology.
   * @param jvmHealthRefreshInterval the interval in millis between refreshes of the JVM health data.
   * @param listeners a set of listeners to subscribe immediately for topology events.
   */
  public TopologyManager(final long topologyRefreshInterval, final long jvmHealthRefreshInterval, final TopologyListener...listeners) {
    this(topologyRefreshInterval, jvmHealthRefreshInterval, null, false, listeners);
  }

  /**
   * Initialize this topology manager with the specified {@link JPPFClient} and listeners.
   * The refresh intervals are determined from the configuration, or take a default value of 1000L if they are not configured.
   * @param client the JPPF client used to discover and monitor the grid topology.
   * @param listeners a set of listeners to subscribe immediately for topology events.
   */
  public TopologyManager(final JPPFClient client, final TopologyListener...listeners) {
    this(client == null ? -1 : client.getConfig().get(JPPFProperties.ADMIN_REFRESH_INTERVAL_TOPOLOGY),
      client == null ? -1 : client.getConfig().get(JPPFProperties.ADMIN_REFRESH_INTERVAL_HEALTH), client, false, listeners);
  }

  /**
   * Initialize this topology manager with the specified {@link JPPFClient} and listeners.
   * @param topologyRefreshInterval the interval in millis between refreshes of the topology.
   * @param jvmHealthRefreshInterval the interval in millis between refreshes of the JVM health data.
   * @param client the JPPF client used to discover and monitor the grid topology.
   * @param listeners a set of listeners to subscribe immediately for topology events.
   */
  public TopologyManager(final long topologyRefreshInterval, final long jvmHealthRefreshInterval, final JPPFClient client, final TopologyListener...listeners) {
    this(topologyRefreshInterval, jvmHealthRefreshInterval, client, false, listeners);
  }

  /**
   * Initialize this topology manager with the specified {@link JPPFClient} and listeners.
   * @param topologyRefreshInterval the interval in millis between refreshes of the topology.
   * @param jvmHealthRefreshInterval the interval in millis between refreshes of the JVM health data.
   * @param client the JPPF client used to discover and monitor the grid topology.
   * @param listeners a set of listeners to subscribe immediately for topology events.
   * @param loadSystemInfo whether the system info of the nodes should be loaded.
   */
  public TopologyManager(final long topologyRefreshInterval, final long jvmHealthRefreshInterval, final JPPFClient client, final boolean loadSystemInfo, final TopologyListener...listeners) {
    long n1 = 0, n2 = 0;
    if (client == null) {
      this.client = new JPPFClient(this);
      n1 = this.client.getConfig().get(JPPFProperties.ADMIN_REFRESH_INTERVAL_TOPOLOGY);
      n2 = this.client.getConfig().get(JPPFProperties.ADMIN_REFRESH_INTERVAL_HEALTH);
    } else {
      this.client = client;
      n1 = topologyRefreshInterval;
      n2 = jvmHealthRefreshInterval;
    }
    this.refreshHandler = new NodeRefreshHandler(this, n1, loadSystemInfo);
    this.jvmHealthRefreshHandler = new JVMHealthRefreshHandler(this, n2);
    if (client != null) client.addConnectionPoolListener(this);
    if (listeners != null) for (TopologyListener listener: listeners) addTopologyListener(listener);
    init();
  }

  /**
   * Initialize the topology tree.
   */
  private void init() {
    for (final JPPFConnectionPool pool: client.getConnectionPools()) {
      List<JPPFClientConnection> list = pool.getConnections(JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING);
      if (list.isEmpty()) list = pool.getConnections();
      if (!list.isEmpty()) {
        final JPPFClientConnection c = list.get(0);
        connectionAdded(new ConnectionPoolEvent(pool, c));
      }
    }
  }

  /**
   * Get the drivers currently handled.
   * @return a list of {@link TopologyDriver} instances.
   */
  public List<TopologyDriver> getDrivers() {
    synchronized(driverMap) {
      return new ArrayList<>(driverMap.values());
    }
  }

  /**
   * Get the driver with the specified uuid.
   * @param uuid the uuid of the driver to lookup.
   * @return a {@link TopologyDriver} instance.
   */
  public TopologyDriver getDriver(final String uuid) {
    synchronized(driverMap) {
      return driverMap.get(uuid);
    }
  }

  /**
   * Get the nodes currently handled.
   * @return a list of {@link TopologyNode} instances.
   */
  public List<TopologyNode> getNodes() {
    synchronized(nodeMap) {
      return new ArrayList<>(nodeMap.values());
    }
  }

  /**
   * Get the nodes that are slaves of the specified master node.
   * @param masterNodeUuid the UUID of the master node whose slaves to lookup.
   * @return a list of {@link TopologyNode} instances, possibly empty but never {@code null}.
   * @since 6.0
   */
  public List<TopologyNode> getSlaveNodes(final String masterNodeUuid) {
    final List<TopologyNode> result = new ArrayList<>(getNodeCount());
    if (masterNodeUuid != null) {
      synchronized(nodeMap) {
        for (final Map.Entry<String, TopologyNode> entry: nodeMap.entrySet()) {
          final TopologyNode node = entry.getValue();
          final String uuid = node.getMasterUuid();
          if ((uuid != null) && uuid.equals(masterNodeUuid)) result.add(node);
        }
      }
    }
    return result;
  }

  /**
   * Get the node with the specified uuid.
   * @param uuid the uuid of the driver to lookup.
   * @return a {@link TopologyNode} instance.
   */
  public TopologyNode getNode(final String uuid) {
    synchronized(nodeMap) {
      return nodeMap.get(uuid);
    }
  }

  /**
   * Get the peers currently handled.
   * @return a list of {@link TopologyPeer} instances.
   */
  public List<TopologyPeer> getPeers() {
    synchronized(peerMap) {
      return new ArrayList<>(peerMap.values());
    }
  }

  /**
   * Get the peer with the specified uuid.
   * @param uuid the uuid of the driver to lookup.
   * @return a {@link TopologyPeer} instance.
   */
  public TopologyPeer getPeer(final String uuid) {
    synchronized(peerMap) {
      return peerMap.get(uuid);
    }
  }

  /**
   * Get the number of drivers currently handled.
   * @return the number of drivers.
   */
  public int getDriverCount() {
    synchronized(driverMap) {
      return driverMap.size();
    }
  }

  /**
   * Get the number of nodes currently handled.
   * @return the number of nodes.
   */
  public int getNodeCount() {
    synchronized(nodeMap) {
      return nodeMap.size();
    }
  }

  /**
   * Get the number of peers currently handled.
   * @return the number of peers.
   */
  public int getPeerCount() {
    synchronized(peerMap) {
      return peerMap.size();
    }
  }

  /**
   * Get the node with the psecified uuid.
   * @param uuid the uuid of the node to lookup.
   * @return a {@link TopologyNode} instance.
   */
  public TopologyNode getNodeOrPeer(final String uuid) {
    TopologyNode node = getNode(uuid);
    if (node == null) node = getPeer(uuid);
    return node;
  }

  /**
   * {@inheritDoc}}
   * @exclude
   */
  @Override
  public void connectionAdded(final ConnectionPoolEvent event) {
    final JPPFClientConnection c = event.getConnection();
    final StatusListener listener = new StatusListener();
    if (c.getStatus().isWorkingStatus()) {
      final TopologyDriver driver = new TopologyDriver(c);
      if (debugEnabled) log.debug("before adding driver {}", driver);
      driverAdded(driver);
    }
    statusListenerMap.put(c, listener);
    c.addClientConnectionStatusListener(listener);
  }

  /**
   * {@inheritDoc}}
   * @exclude
   */
  @Override
  public void connectionRemoved(final ConnectionPoolEvent event) {
    final JPPFClientConnection c = event.getConnection();
    final String uuid = c.getDriverUuid();
    if (uuid != null) {
      final TopologyDriver driver = driverMap.remove(uuid);
      if (driver != null) driverRemoved(driver);
    }
    final StatusListener listener = (StatusListener) statusListenerMap.remove(c);
    if (listener != null) c.removeClientConnectionStatusListener(listener);
  }

  /**
   * Add a topology change listener.
   * @param listener the listener to add.
   */
  public void addTopologyListener(final TopologyListener listener) {
    if (listener == null) throw new IllegalArgumentException("cannot add a null listener");
    listeners.add(listener);
  }

  /**
   * Remove a topology change listener.
   * @param listener the listener to remove.
   */
  public void removeTopologyListener(final TopologyListener listener) {
    if (listener == null) throw new IllegalArgumentException("cannot remove a null listener");
    listeners.remove(listener);
  }

  /**
   * Notify all listeners that a driver was added.
   * @param driver the driver to add.
   */
  void driverAdded(final TopologyDriver driver) {
    if (debugEnabled) log.debug("adding driver {}, uuid={}", driver, driver.getUuid());
    TopologyDriver other = null;
    synchronized(driverMap) {
      other = driverMap.get(driver.getUuid());
      if (debugEnabled && (other != null)) log.debug("driver already exists with same uuid: {}", other);
      if (other == null) {
        other = driverMap.get(driver.getManagementInfo().toDisplayString());
        if (debugEnabled && (other != null)) log.debug("driver already exists with same jmx id: {}", other);
      }
    }
    if (other != null) {
      driverRemoved(other);
    }
    synchronized(driverMap) {
      driverMap.put(driver.getUuid(), driver);
    }
    final TopologyEvent event = new TopologyEvent(this, driver, null, TopologyEvent.UpdateType.TOPOLOGY);
    dispatchEvent(event, TopologyEvent.Type.DRIVER_ADDED);
  }

  /**
   * Notify all listeners that a driver was added.
   * @param driver the driver that was removed.
   */
  void driverRemoved(final TopologyDriver driver) {
    if (debugEnabled) log.debug("removing driver {}", driver);
    final JPPFClientConnection c = driver.getConnection();
    final ClientConnectionStatusListener listener = statusListenerMap.remove(c);
    if (listener != null) c.removeClientConnectionStatusListener(listener);
    for (final AbstractTopologyComponent child: driver.getChildren()) nodeRemoved(driver, (TopologyNode) child);
    synchronized(driverMap) {
      driverMap.remove(driver.getUuid());
    }
    final TopologyEvent event = new TopologyEvent(this, driver, null, TopologyEvent.UpdateType.TOPOLOGY);
    dispatchEvent(event, TopologyEvent.Type.DRIVER_REMOVED);
  }

  /**
   * Notify all listeners that the state of a driver has changed.
   * @param driver the driver to add.
   * @param updateType the type of update.
   */
  void driverUpdated(final TopologyDriver driver, final TopologyEvent.UpdateType updateType) {
    final TopologyEvent event = new TopologyEvent(this, driver, null, updateType);
    dispatchEvent(event, TopologyEvent.Type.DRIVER_UPDATED);
  }

  /**
   * Notify all listeners that a driver was added.
   * @param driver the driver to which the node is attached.
   * @param node the node that was added.
   */
  void nodeAdded(final TopologyDriver driver, final TopologyNode node) {
    if (debugEnabled) log.debug("adding {} {} to driver {}", node.isPeer() ? "peer" : "node", node, driver);
    if (node.isNode()) {
      final TopologyNode other = getNodeOrPeer(node.getUuid());
      if (other != null) nodeRemoved((TopologyDriver) other.getParent(), other);
    }
    driver.add(node);
    if (node.isNode()) {
      synchronized(nodeMap) {
        nodeMap.put(node.getUuid(), node);
      }
    } else {
      synchronized(peerMap) {
        peerMap.put(node.getUuid(), (TopologyPeer) node);
      }
    }
    final TopologyEvent event = new TopologyEvent(this, driver, node, TopologyEvent.UpdateType.TOPOLOGY);
    dispatchEvent(event, TopologyEvent.Type.NODE_ADDED);
  }

  /**
   * Notify all listeners that a node was removed.
   * @param driver the driver to which the node is attached.
   * @param node the node that was removed.
   */
  void nodeRemoved(final TopologyDriver driver, final TopologyNode node) {
    if (debugEnabled) log.debug("removing {} {} from driver {}", (node.isNode() ? "node" : "peer"), node, driver);
    driver.remove(node);
    if (node.isNode()) {
      synchronized(nodeMap) {
        nodeMap.remove(node.getUuid());
      }
    } else {
      synchronized(peerMap) {
        peerMap.remove(node.getUuid());
      }
    }
    final TopologyEvent event = new TopologyEvent(this, driver, node, TopologyEvent.UpdateType.TOPOLOGY);
    dispatchEvent(event, TopologyEvent.Type.NODE_REMOVED);
  }

  /**
   * Notify all listeners that a driver was added.
   * @param driverData the driver that was updated or to which the updated node is attached.
   * @param node the node that was updated, or <code>null</code> if it is a driver that was updated.
   * @param updateType the type of update.
   */
  void nodeUpdated(final TopologyDriver driverData, final TopologyNode node, final TopologyEvent.UpdateType updateType) {
    final TopologyEvent event = new TopologyEvent(this, driverData, node, updateType);
    dispatchEvent(event, TopologyEvent.Type.NODE_UPDATED);
  }

  /**
   * Dispatch the specified event to all listeners.
   * @param event the event to dispatch.
   * @param type the type of event.
   */
  private void dispatchEvent(final TopologyEvent event, final TopologyEvent.Type type) {
    final Runnable dispatchTask = () -> {
      if (log.isTraceEnabled()) log.trace("dispatching event type={} : {}", type, event);
      switch (type) {
        case DRIVER_ADDED: for (final TopologyListener listener: listeners) listener.driverAdded(event);
        break;
        case DRIVER_REMOVED: for (final TopologyListener listener: listeners) listener.driverRemoved(event);
        break;
        case DRIVER_UPDATED: for (final TopologyListener listener: listeners) listener.driverUpdated(event);
        break;
        case NODE_ADDED: for (final TopologyListener listener: listeners) listener.nodeAdded(event);
        break;
        case NODE_REMOVED: for (final TopologyListener listener: listeners) listener.nodeRemoved(event);
        break;
        case NODE_UPDATED: for (final TopologyListener listener: listeners) listener.nodeUpdated(event);
        break;
      }
    };
    executor.execute(dispatchTask);
  }

  /**
   * Get the JPPF client.
   * @return a {@link JPPFClient} object.
   */
  public JPPFClient getJPPFClient() {
    return client;
  }

  /**
   * Listens for the status of a driver connection and updates the tree accordingly.
   */
  private class StatusListener implements ClientConnectionStatusListener {
    @Override
    public void statusChanged(final ClientConnectionStatusEvent event) {
      final JPPFClientConnection c = event.getClientConnection();
      final JPPFClientConnectionStatus newStatus = c.getStatus();
      final JPPFClientConnectionStatus oldStatus = event.getOldStatus();
      if (newStatus.isWorkingStatus() && !oldStatus.isWorkingStatus()) {
        final TopologyDriver driver = new TopologyDriver(c);
        if (debugEnabled) log.debug("before adding driver {}", driver);
        driverAdded(driver);
      } else if (!newStatus.isWorkingStatus() && (c.getDriverUuid() != null)) {
        final TopologyDriver driver = getDriver(c.getDriverUuid());
        if (driver != null) {
          if (oldStatus.isWorkingStatus()) {
            for (final AbstractTopologyComponent child: driver.getChildren()) nodeRemoved(driver, (TopologyNode) child);
          }
          if (newStatus.isTerminatedStatus() && !oldStatus.isTerminatedStatus()) driverRemoved(driver);
        }
      }
    }
  }

  /**
   * Get the node selector used to filter the nodes and associated events.
   * @return a {@link NodeSelector} instance.
   * @since 5.2
   */
  public synchronized NodeSelector getNodeFilter() {
    return nodeFilter;
  }

  /**
   * Set the node selector used to filter the nodes and associated events.
   * @param nodeFilter a {@link NodeSelector} instance.
   * @since 5.2
   */
  public synchronized void setNodeFilter(final NodeSelector nodeFilter) {
    this.nodeFilter = nodeFilter;
  }

  @Override
  public void close() {
    refreshHandler.stopRefreshTimer();;
    jvmHealthRefreshHandler.stopRefreshTimer();;
    listeners.clear();
    client.removeConnectionPoolListener(this);
  }
}
