/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import org.jppf.client.monitoring.AbstractRefreshHandler;
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
 * @exclude
 */
class NodeRefreshHandler extends AbstractRefreshHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeRefreshHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The topology manager to which topology change notifications are to be sent. 
   */
  private final TopologyManager manager;

  /**
   * Initialize this node handler.
   * @param manager the topology manager.
   * @param period the interval between refreshes in millis.
   */
  NodeRefreshHandler(final TopologyManager manager, final long period) {
    super("JPPF Topology Update Timer", period);
    this.manager = manager;
    startRefreshTimer();
  }

  /**
   * Refresh the tree structure.
   * @exclude
   */
  protected synchronized void performRefresh() {
    for (TopologyDriver driver: manager.getDrivers()) {
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
    for (AbstractTopologyComponent child: driver.getChildren()) knownUuids.add(child.getUuid());
    JMXDriverConnectionWrapper jmx = driver.getJmx();
    if ((jmx == null) || !jmx.isConnected()) return;
    Collection<JPPFManagementInfo> nodesInfo = null;
    try {
      nodesInfo = jmx.nodesInformation(null, true);
    } catch(Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      return;
    }
    Map<String, JPPFManagementInfo> actualMap = new HashMap<>();
    if (nodesInfo != null) {
      for (JPPFManagementInfo info: nodesInfo) {
        if (info.getPort() >= 0) actualMap.put(info.getUuid(), info);
      }
    }
    List<String> nodesToProcess = new ArrayList<>(knownUuids.size());
    for (String uuid: knownUuids) {
      if (!actualMap.containsKey(uuid)) nodesToProcess.add(uuid);
    }
    for (String uuid: nodesToProcess) {
      //TopologyNode node = manager.getNodeOrPeer(uuid);
      TopologyNode node = (TopologyNode) driver.getChild(uuid);
      //if (node == null) node = new TopologyNode(uuid);
      if (debugEnabled) log.debug("removing node " + node);
      if (node != null) manager.nodeRemoved(driver, node);
    }
    for (Map.Entry<String, JPPFManagementInfo> entry: actualMap.entrySet()) {
      String uuid = entry.getKey();
      JPPFManagementInfo info = entry.getValue();
      if (!knownUuids.contains(uuid)) {
        if (debugEnabled) log.debug("adding node " + info);
        TopologyNode node = null;
        node = info.isPeer() ? new TopologyPeer(info) : new TopologyNode(info);
        manager.nodeAdded(driver, node);
      } else {
        TopologyNode node = (TopologyNode) manager.getNodeOrPeer(uuid);
        if (node != null) {
          if (info.isActive() != node.getManagementInfo().isActive()) {
            node.getManagementInfo().setIsActive(entry.getValue().isActive());
            manager.nodeUpdated(driver, node, TopologyEvent.UpdateType.NODE_STATE);
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
    List<AbstractTopologyComponent> children = driver.getChildren();
    // refresh the nodes execution states
    Map<String, TopologyNode> uuidMap = new HashMap<>();
    for (AbstractTopologyComponent child: children) {
      if (child.isNode()) uuidMap.put(child.getUuid(), (TopologyNode) child); 
    }
    Map<String, Object> result = null;
    try {
      result = forwarder.state(new UuidSelector(uuidMap.keySet()));
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
        if (debugEnabled) log.debug("exception raised for node " + entry.getKey() + " : " + ExceptionUtils.getMessage((Exception) entry.getValue()));
      } else if (entry.getValue() instanceof JPPFNodeState) {
        JPPFNodeState oldState = (JPPFNodeState) entry.getValue();
        if (!oldState.equals(node.getNodeState())) {
          changedNodes.add(node);
          node.refreshNodeState(oldState);
        }
      }
    }
    refreshProvisioningStates(driver, forwarder, changedNodes);
    for (TopologyNode node: changedNodes) manager.nodeUpdated(driver, node, TopologyEvent.UpdateType.NODE_STATE);
  }

  /**
   * Refresh the provisioning state of the master nodes attached to the specified driver.
   * @param driver the driver for which to refresh the nodes.
   * @param forwarder used to forward the request to get the number of slaves to the nodes.
   * @param changedNodes collects the nodes for which an update occurred.
   */
  private void refreshProvisioningStates(final TopologyDriver driver, final JPPFNodeForwardingMBean forwarder, final Set<TopologyNode> changedNodes) {
    Map<String, TopologyNode> uuidMap = new HashMap<>();
    for (AbstractTopologyComponent child: driver.getChildren()) {
      if (child.isNode()) {
        TopologyNode node = (TopologyNode) child;
        if (node.getManagementInfo().isMasterNode()) uuidMap.put(child.getUuid(), (TopologyNode) child);
      }
    }
    Map<String, Object> result = null;
    try {
      result = forwarder.forwardGetAttribute(new UuidSelector(uuidMap.keySet()), JPPFNodeProvisioningMBean.MBEAN_NAME, "NbSlaves");
    } catch(Exception e) {
      if (debugEnabled) log.debug("error getting number of slaves for driver " + driver.getUuid(), e);
    }
    if (result == null) return;
    for (Map.Entry<String, Object> entry: result.entrySet()) {
      TopologyNode node = uuidMap.get(entry.getKey());
      if (node == null) continue;
      if (entry.getValue() instanceof Exception) {
        node.setStatus(TopologyNodeStatus.DOWN);
        if (debugEnabled) log.debug("exception raised for node " + entry.getKey() + " : " + ExceptionUtils.getMessage((Exception) entry.getValue()));
      } else if (entry.getValue() instanceof Integer) {
        node.setStatus(TopologyNodeStatus.UP);
        int n = (Integer) entry.getValue();
        if (n != node.getNbSlaveNodes()) {
          changedNodes.add(node);
          node.setNbSlaveNodes(n);
        }
      }
    }
  }
}
