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

package org.jppf.client.monitoring.topology;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.utils.JPPFThreadFactory;
import org.slf4j.*;

/**
 * Instances of this class discover and maintain a representation of a JPPF grid topology.
 * @author Laurent Cohen
 * @since 5.0
 */
public class TopologyManager implements ClientListener {
  /**
   * The drivers in the topology.
   */
  private final List<TopologyDriver> drivers = new CopyOnWriteArrayList<>();
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
  private final NodeRefreshHandler refreshHandler;

  /**
   * Initialize this toplogy manager with a new {@link JPPFClient}.
   */
  public TopologyManager() {
    this.refreshHandler = new NodeRefreshHandler(this);
    refreshHandler.startRefreshTimer();
    this.client = new JPPFClient(this);
  }

  /**
   * Initialize this toplogy manager with the specified {@link JPPFClient}.
   * @param client the JPPF client used to discover and monitor the grid topology.
   */
  public TopologyManager(final JPPFClient client) {
    this.refreshHandler = new NodeRefreshHandler(this);
    refreshHandler.startRefreshTimer();
    this.client = client;
    this.client.addClientListener(this);
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
    return driverMap.get(uuid);
  }

  /**
   * Get the number of drivers currently handled.
   * @return the number of drivers.
   */
  public int getDriverCount() {
    return drivers.size();
  }

  /**
   * Get the number of nodes currently handled.
   * @return the number of nodes.
   */
  public int getNodeCount() {
    return nodeMap.size();
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

  @Override
  public void newConnection(final ClientEvent event) {
    final JPPFClientConnection c = event.getConnection();
    if (!c.getStatus().isWorkingStatus()) {
      c.addClientConnectionStatusListener(new ClientConnectionStatusListener() {
        @Override
        public void statusChanged(final ClientConnectionStatusEvent event) {
          if (c.getStatus().isWorkingStatus()) {
            c.removeClientConnectionStatusListener(this);
            driverAdded(new TopologyDriver(c));
          }
        }
      });
    }
    else driverAdded(new TopologyDriver(c));
  }

  @Override
  public void connectionFailed(final ClientEvent event) {
    final JPPFClientConnection c = event.getConnection();
    String uuid = c.getDriverUuid();
    if (uuid != null) {
      TopologyDriver driver = driverMap.remove(uuid);
      if (driver != null) driverRemoved(driver);
    }
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
    driverMap.put(driver.getUuid(), driver);
    TopologyEvent event = new TopologyEvent(this, driver, null, null);
    dispatchEvent(event, TopologyEvent.Type.DRIVER_ADDED);
  }

  /**
   * Notify all listeners that a driver was added.
   * @param driver the driver that was removed.
   */
  void driverRemoved(final TopologyDriver driver) {
    if (debugEnabled) log.debug("removing driver {}", driver);
    for (AbstractTopologyComponent child: driver.getChildrenSynchronized()) nodeRemoved(driver, (TopologyNode) child);
    driverMap.remove(driver.getUuid());
    TopologyEvent event = new TopologyEvent(this, driver, null, null);
    for (TopologyListener listener: listeners) listener.driverRemoved(event);
    dispatchEvent(event, TopologyEvent.Type.DRIVER_REMOVED);
  }

  /**
   * Notify all listeners that a driver was added.
   * @param driver the driver to which the node is attached.
   * @param node the node that was added.
   */
  void nodeAdded(final TopologyDriver driver, final TopologyNode node) {
    if (debugEnabled) log.debug(String.format("adding %s %s to driver %s", node.isPeer() ? "peer" : "node", node, driver));
    driver.add(node);
    if (node.isNode()) nodeMap.put(node.getUuid(), node);
    else peerMap.put(node.getUuid(), (TopologyPeer) node);
    TopologyEvent event = node.isPeer() ? new TopologyEvent(this, driver, null, (TopologyPeer) node) : new TopologyEvent(this, driver, node, null);
    dispatchEvent(event, TopologyEvent.Type.NODE_ADDED);
  }

  /**
   * Notify all listeners that a node was removed.
   * @param driver the driver to which the node is attached.
   * @param node the node that was removed.
   */
  void nodeRemoved(final TopologyDriver driver, final TopologyNode node) {
    if (debugEnabled) log.debug("removing node {} from driver {}", node, driver);
    driver.remove(node);
    if (node.isNode()) nodeMap.remove(node.getUuid());
    else peerMap.remove(node.getUuid());
    TopologyEvent event = new TopologyEvent(this, driver, node, null);
    dispatchEvent(event, TopologyEvent.Type.NODE_REMOVED);
  }

  /**
   * Notify all listeners that a driver was added.
   * @param driverData the driver that was updated or to which the updated node is attached.
   * @param node the node that was updated, or <code>null</code> if it is a driver that was updated.
   */
  void nodeUpdated(final TopologyDriver driverData, final TopologyNode node) {
    TopologyEvent event = new TopologyEvent(this, driverData, node, null);
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
        switch (type) {
          case DRIVER_ADDED: for (TopologyListener listener: listeners) listener.driverAdded(event);
          break;
          case DRIVER_REMOVED: for (TopologyListener listener: listeners) listener.driverRemoved(event);
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
    executor.submit(dispatchTask);
  }

  /**
   * Get the JPPF client.
   * @return a {@link JPPFClient} object.
   */
  public JPPFClient getJPPFClient() {
    return client;
  }
}
