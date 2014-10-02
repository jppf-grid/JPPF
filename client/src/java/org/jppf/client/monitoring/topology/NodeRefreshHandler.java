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

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class hold information about the associations between JPPF drivers and
 * their attached nodes, for management and monitoring purposes.
 * @author Laurent Cohen
 * @since 5.0
 */
class NodeRefreshHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeRefreshHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Timer used to query the driver management data.
   */
  private Timer refreshTimer = null;
  /**
   * Interval, in milliseconds, between refreshes from the server.
   */
  private long refreshInterval = JPPFConfiguration.getProperties().getLong("jppf.admin.refresh.interval.topology", 1000L);
  /**
   * Count of refresh invocations.
   */
  private AtomicLong refreshCount = new AtomicLong(0L);
  /**
   * Determines whether we are currently refreshing.
   */
  private AtomicBoolean refreshing = new AtomicBoolean(false);
  /**
   * The topology manager to which topology change notifications are to be sent. 
   */
  private final TopologyManager manager;

  /**
   * Initialize this node handler.
   * @param manager the topology manager.
   */
  public NodeRefreshHandler(final TopologyManager manager) {
    this.manager = manager;
    initialize();
  }

  /**
   * Initialize this node refresh handler.
   */
  private void initialize() {
    startRefreshTimer();
  }

  /**
   * Refresh the tree structure asynchronously (not in the AWT event thread).
   */
  public void refresh() {
    if (refreshing.compareAndSet(false, true)) {
      try {
        performRefresh();
      } finally {
        refreshing.set(false);
      }
    }
  }

  /**
   * Refresh the tree structure.
   */
  private synchronized void performRefresh() {
    List<TopologyDriver> drivers = manager.getDrivers();
    for (TopologyDriver driver: drivers) {
      refreshNodes(driver);
      if (driver.getChildCount() > 0) refreshNodeStates(driver);
    }
  }

  /**
   * Refresh the nodes currently attached to the specified driver.
   * @param driver the driver for which to refresh the nodes.
   */
  private void refreshNodes(final TopologyDriver driver) {
    Set<String> knownUuids = new HashSet<>();
    for (AbstractTopologyComponent child: driver.getChildrenSynchronized()) knownUuids.add(child.getUuid());
    JMXDriverConnectionWrapper wrapper = driver.getJmx();
    if ((wrapper == null) || !wrapper.isConnected()) return;
    Collection<JPPFManagementInfo> nodesInfo = null;
    try {
      nodesInfo = wrapper.nodesInformation();
    } catch(Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      return;
    }
    Map<String, JPPFManagementInfo> actualMap = new HashMap<>();
    for (JPPFManagementInfo info: nodesInfo) {
      if (info.getPort() >= 0) actualMap.put(info.getUuid(), info);
    }
    List<String> nodesToProcess = new ArrayList<>(knownUuids.size());
    for (String uuid: knownUuids) {
      if (!actualMap.containsKey(uuid)) nodesToProcess.add(uuid);
    }
    for (String uuid: nodesToProcess) {
      TopologyNode node = manager.getNodeOrPeer(uuid);
      if (debugEnabled) log.debug("removing node " + node);
      if (node != null) manager.nodeRemoved(driver, node);
    }
    for (Map.Entry<String, JPPFManagementInfo> entry: actualMap.entrySet()) {
      String uuid = entry.getKey();
      JPPFManagementInfo info = entry.getValue();
      if (!knownUuids.contains(uuid)) {
        if (debugEnabled) log.debug("adding node " + info);
        TopologyNode node = null;
        node = info.isPeer() ? new TopologyPeer(info, driver.getUuid()) : new TopologyNode(info);
        manager.nodeAdded(driver, node);
      } else {
        TopologyNode node = (TopologyNode) manager.getNodeOrPeer(uuid);
        if (node != null) {
          if (info.isActive() != node.getManagementInfo().isActive()) {
            node.getManagementInfo().setActive(entry.getValue().isActive());
            manager.nodeUpdated(driver, node);
          }
        }
      }
    }
  }

  /**
   * Refresh the states of the nodes for the specified driver.
   * @param driver the driver for which to update the nodes.
   */
  private void refreshNodeStates(final TopologyDriver driver) {
    JPPFNodeForwardingMBean forwarder = driver.getForwarder();
    if (forwarder == null) return;
    List<AbstractTopologyComponent> children = driver.getChildrenSynchronized();
    // refresh the nodes execution states
    Map<String, TopologyNode> uuidMap = new HashMap<>();
    for (AbstractTopologyComponent child: children) {
      if (child.isNode()) uuidMap.put(child.getUuid(), (TopologyNode) child); 
    }
    Map<String, Object> result = null;
    try {
      result = forwarder.state(new NodeSelector.UuidSelector(uuidMap.keySet()));
    } catch(IOException e) {
      log.error("error getting node states for driver " + driver.getUuid() + ", reinitializing the connection", e);
      driver.initializeProxies();
    } catch(Exception e) {
      log.error("error getting node states for driver " + driver.getUuid(), e);
    }
    if (result == null) return;
    Set<TopologyNode> changedNodes = new HashSet<>();
    for (Map.Entry<String, Object> entry: result.entrySet()) {
      TopologyNode node = uuidMap.get(entry.getKey());
      if (node == null) continue;
      if (entry.getValue() instanceof Exception) {
        node.setStatus(TopologyNodeStatus.DOWN);
        log.warn("exception raised for node " + entry.getKey() + " : " + ExceptionUtils.getMessage((Exception) entry.getValue()));
      }
      else if (entry.getValue() instanceof JPPFNodeState) {
        JPPFNodeState oldState = (JPPFNodeState) entry.getValue();
        if (!oldState.equals(node.getNodeState())) {
          changedNodes.add(node);
          node.refreshNodeState(oldState);
        }
      }
    }
    // refresh the nodes provisioning states
    uuidMap.clear();
    for (AbstractTopologyComponent child: children) {
      if (child.isNode()) {
        TopologyNode node = (TopologyNode) child;
        if (node.getManagementInfo().isMasterNode()) uuidMap.put(child.getUuid(), (TopologyNode) child);
      }
    }
    try {
      result = forwarder.forwardGetAttribute(new NodeSelector.UuidSelector(uuidMap.keySet()), JPPFNodeProvisioningMBean.MBEAN_NAME, "NbSlaves");
    } catch(IOException e) {
      log.error("error getting number of slaves for driver " + driver.getUuid() + ", reinitializing the connection", e);
      driver.initializeProxies();
    } catch(Exception e) {
      log.error("error getting number of slaves for driver " + driver.getUuid(), e);
    }
    if (result == null) return;
    for (Map.Entry<String, Object> entry: result.entrySet()) {
      TopologyNode node = uuidMap.get(entry.getKey());
      if (node == null) continue;
      if (entry.getValue() instanceof Exception) {
        node.setStatus(TopologyNodeStatus.DOWN);
        log.warn("exception raised for node " + entry.getKey() + " : " + ExceptionUtils.getMessage((Exception) entry.getValue()));
      }
      else if (entry.getValue() instanceof Integer) {
        int n = (Integer) entry.getValue();
        if (n != node.getNbSlaveNodes()) {
          changedNodes.add(node);
          node.setNbSlaveNodes(n);
        }
      }
    }
    
    for (TopologyNode node: changedNodes) manager.nodeUpdated(driver, node);
  }

  /**
   * Stop the automatic refresh of the nodes state through a timer.
   */
  public void stopRefreshTimer() {
    if (refreshTimer != null) {
      refreshTimer.cancel();
      refreshTimer = null;
    }
  }

  /**
   * Start the automatic refresh of the nodes state through a timer.
   */
  public void startRefreshTimer() {
    if (refreshTimer != null) return;
    if (refreshInterval <= 0L) return;
    refreshTimer = new Timer("JPPF Topology Update Timer");
    TimerTask task = new TimerTask() {
      @Override
      public void run() {
        refresh();
      }
    };
    refreshTimer.schedule(task, 1000L, refreshInterval);
  }
}
