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

package org.jppf.client.monitoring.topology;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.management.NodeSelector;
import org.jppf.utils.*;
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
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Mapping of driver uuids to the corresponding {@link TopologyDriver} objects.
   */
  private final Map<String, TopologyDriver> driverMap = new Hashtable<>();
  /**
   * Synchronization lock.
   */
  private final Object driversLock = new Object();
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
    for (JPPFConnectionPool pool: client.getConnectionPools()) {
      List<JPPFClientConnection> list = pool.getConnections(JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING);
      if (list.isEmpty()) list = pool.getConnections();
      if (!list.isEmpty()) {
        JPPFClientConnection c = list.get(0);
        connectionAdded(new ConnectionPoolEvent(pool, c));
      }
    }
  }

  /**
   * Get the drivers currently handled.
   * @return a list of {@link TopologyDriver} instances.
   */
  public List<TopologyDriver> getDrivers() {
    synchronized(driversLock) {
      return new ArrayList<>(driverMap.values());
    }
  }

  /**
   * Get the driver with the specified uuid.
   * @param uuid the uuid of the driver to lookup.
   * @return a {@link TopologyDriver} instance.
   */
  public TopologyDriver getDriver(final String uuid) {
    synchronized(driversLock) {
      return driverMap.get(uuid);
    }
  }

  /**
   * Get the drivers currently handled.
   * @return a list of {@link TopologyNode} instances.
   */
  public List<TopologyNode> getNodes() {
    synchronized(nodeMap) {
      return new ArrayList<>(nodeMap.values());
    }
  }

  /**
   * Get the driver with the specified uuid.
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
    synchronized(driversLock) {
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
    TopologyNode node = nodeMap.get(uuid);
    if (node == null) node = peerMap.get(uuid);
    return node;
  }

  /**
   * {@inheritDoc}}
   * @exclude
   */
  @Override
  public void connectionAdded(final ConnectionPoolEvent event) {
    final JPPFClientConnection c = event.getConnection();
    StatusListener listener = new StatusListener();
    if (c.getStatus().isWorkingStatus()) {
      TopologyDriver driver = new TopologyDriver(c);
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
    String uuid = c.getDriverUuid();
    if (uuid != null) {
      TopologyDriver driver = driverMap.remove(uuid);
      if (driver != null) driverRemoved(driver);
    }
    StatusListener listener = (StatusListener) statusListenerMap.remove(c);
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
    synchronized(driversLock) {
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
    synchronized(driversLock) {
      driverMap.put(driver.getUuid(), driver);
    }
    TopologyEvent event = new TopologyEvent(this, driver, null, TopologyEvent.UpdateType.TOPOLOGY);
    dispatchEvent(event, TopologyEvent.Type.DRIVER_ADDED);
  }

  /**
   * Notify all listeners that a driver was added.
   * @param driver the driver that was removed.
   */
  void driverRemoved(final TopologyDriver driver) {
    if (debugEnabled) log.debug("removing driver {}", driver);
    JPPFClientConnection c = driver.getConnection();
    ClientConnectionStatusListener listener = statusListenerMap.remove(c);
    if (listener != null) c.removeClientConnectionStatusListener(listener);
    for (AbstractTopologyComponent child: driver.getChildren()) nodeRemoved(driver, (TopologyNode) child);
    synchronized(driversLock) {
      driverMap.remove(driver.getUuid());
    }
    TopologyEvent event = new TopologyEvent(this, driver, null, TopologyEvent.UpdateType.TOPOLOGY);
    dispatchEvent(event, TopologyEvent.Type.DRIVER_REMOVED);
  }

  /**
   * Notify all listeners that the state of a driver has changed.
   * @param driver the driver to add.
   * @param updateType the type of update.
   */
  void driverUpdated(final TopologyDriver driver, final TopologyEvent.UpdateType updateType) {
    TopologyEvent event = new TopologyEvent(this, driver, null, updateType);
    dispatchEvent(event, TopologyEvent.Type.DRIVER_UPDATED);
  }

  /**
   * Notify all listeners that a driver was added.
   * @param driver the driver to which the node is attached.
   * @param node the node that was added.
   */
  void nodeAdded(final TopologyDriver driver, final TopologyNode node) {
    if (debugEnabled) log.debug(String.format("adding %s %s to driver %s", node.isPeer() ? "peer" : "node", node, driver));
    if (node.isNode()) {
      TopologyNode other = getNodeOrPeer(node.getUuid());
      if (other != null) nodeRemoved((TopologyDriver) other.getParent(), other);
    }
    driver.add(node);
    if (node.isNode()) nodeMap.put(node.getUuid(), node);
    else peerMap.put(node.getUuid(), (TopologyPeer) node);
    TopologyEvent event = new TopologyEvent(this, driver, node, TopologyEvent.UpdateType.TOPOLOGY);
    dispatchEvent(event, TopologyEvent.Type.NODE_ADDED);
  }

  /**
   * Notify all listeners that a node was removed.
   * @param driver the driver to which the node is attached.
   * @param node the node that was removed.
   */
  void nodeRemoved(final TopologyDriver driver, final TopologyNode node) {
    if (debugEnabled) log.debug(String.format("removing %s %s from driver %s", (node.isNode() ? "node" : "peer"), node, driver));
    driver.remove(node);
    TopologyEvent event = null;
    if (node.isNode()) nodeMap.remove(node.getUuid());
    else peerMap.remove(node.getUuid());
    event = new TopologyEvent(this, driver, node, TopologyEvent.UpdateType.TOPOLOGY);
    dispatchEvent(event, TopologyEvent.Type.NODE_REMOVED);
  }

  /**
   * Notify all listeners that a driver was added.
   * @param driverData the driver that was updated or to which the updated node is attached.
   * @param node the node that was updated, or <code>null</code> if it is a driver that was updated.
   * @param updateType the type of update.
   */
  void nodeUpdated(final TopologyDriver driverData, final TopologyNode node, final TopologyEvent.UpdateType updateType) {
    TopologyEvent event = new TopologyEvent(this, driverData, node, updateType);
    dispatchEvent(event, TopologyEvent.Type.NODE_UPDATED);
  }

  /**
   * Dispatch the specified event to all listeners.
   * @param event the event to dispatch.
   * @param type the type of event.
   */
  private void dispatchEvent(final TopologyEvent event, final TopologyEvent.Type type) {
    Runnable dispatchTask = new Runnable() {
      @Override public void run() {
        if (log.isTraceEnabled()) log.trace("dispatching event type={} : {}", type, event);
        switch (type) {
          case DRIVER_ADDED: for (TopologyListener listener: listeners) listener.driverAdded(event);
          break;
          case DRIVER_REMOVED: for (TopologyListener listener: listeners) listener.driverRemoved(event);
          break;
          case DRIVER_UPDATED: for (TopologyListener listener: listeners) listener.driverUpdated(event);
          break;
          case NODE_ADDED: for (TopologyListener listener: listeners) listener.nodeAdded(event);
          break;
          case NODE_REMOVED: for (TopologyListener listener: listeners) listener.nodeRemoved(event);
          break;
          case NODE_UPDATED: for (TopologyListener listener: listeners) listener.nodeUpdated(event);
          break;
        }
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
      JPPFClientConnection c = (JPPFClientConnection) event.getClientConnectionStatusHandler();
      JPPFClientConnectionStatus newStatus = c.getStatus();
      JPPFClientConnectionStatus oldStatus = event.getOldStatus();
      if (newStatus.isWorkingStatus() && !oldStatus.isWorkingStatus()) {
        TopologyDriver driver = new TopologyDriver(c);
        if (debugEnabled) log.debug("before adding driver {}", driver);
        driverAdded(driver);
      } else if (!newStatus.isWorkingStatus() && (c.getDriverUuid() != null)) {
        TopologyDriver driver = getDriver(c.getDriverUuid());
        if (driver != null) {
          if (oldStatus.isWorkingStatus()) {
            for (AbstractTopologyComponent child: driver.getChildren()) nodeRemoved(driver, (TopologyNode) child);
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
