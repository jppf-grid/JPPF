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

package org.jppf.ui.monitoring.node;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.ui.treetable.JPPFTreeTable;
import org.slf4j.*;

/**
 * This class manages updates to, and navigation within, the tree table
 * for the node data panel.
 * @author Laurent Cohen
 */
public class NodeDataPanelManager {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(NodeDataPanelManager.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The container for the tree table.
   */
  private NodeDataPanel panel;
  /**
   * Mapping of driver uuids to the corresponding {@link TopologyData} objects.
   */
  private final Map<String, TopologyData> driverMap = new Hashtable<>();
  /**
   * Mapping of peer driver uuids to the corresponding {@link TopologyData} objects.
   */
  private final Map<String, TopologyData> peerMap = new Hashtable<>();
  /**
   * Mapping of node uuids to the corresponding {@link TopologyData} objects.
   */
  private final Map<String, TopologyData> nodeMap = new Hashtable<>();
  /**
   * 
   */
  private final List<TopologyChangeListener> listeners = new CopyOnWriteArrayList<>();

  /**
   * Initialize this manager.
   * @param panel the container for the tree table.
   */
  NodeDataPanelManager(final NodeDataPanel panel) {
    this.panel = panel;
  }

  /**
   * Called when the state information of a node has changed.
   * @param driverName the name of the driver to which the node is attached.
   * @param nodeName the name of the node to update.
   */
  void nodeDataUpdated(final String driverName, final String nodeName) {
    final DefaultMutableTreeNode driverNode = findDriver(driverName);
    if (driverNode == null) return;
    final DefaultMutableTreeNode node = findNode(driverNode, nodeName);
    if (node != null) {
      panel.getModel().changeNode(node);
      if (panel.getGraphOption() != null) fireDataUpdated((TopologyData) driverNode.getUserObject(), (TopologyData) node.getUserObject());
    }
  }

  /**
   * Called to notify that a driver was added.
   * @param connection a reference to the driver connection.
   */
  void driverAdded(final JPPFClientConnection connection) {
    try {
      if (!connection.getStatus().isWorkingStatus()) return;
      if (findDriver(connection.getDriverUuid()) != null) return;
      JMXDriverConnectionWrapper jmx = connection.getConnectionPool().getJmxConnection();
      String driverName = connection.getDriverUuid();
      int index = driverInsertIndex(driverName);
      if (index < 0) return;
      TopologyData driverData = new TopologyData(connection);
      driverMap.put(driverData.getUuid(), driverData);
      DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(driverData);
      if (debugEnabled) log.debug("adding driver: " + driverName + " at index " + index);
      panel.getModel().insertNodeInto(driverNode, panel.getTreeTableRoot(), index);
      fireDriverAdded(driverData);
      panel.updateStatusBar("/StatusNbServers", 1);
      if (jmx != null) {
        if ((jmx != null) && (panel.getListenerMap().get(jmx.getId()) == null)) {
          ConnectionStatusListener listener = new ConnectionStatusListener(panel, driverData.getUuid());
          connection.addClientConnectionStatusListener(listener);
          panel.getListenerMap().put(jmx.getId(), listener);
        }
        Collection<JPPFManagementInfo> nodes = null;
        try {
          nodes = jmx.nodesInformation();
        } catch(Exception e) {
          if (debugEnabled) log.debug(e.getMessage(), e);
          return;
        }
        if (nodes != null) for (JPPFManagementInfo nodeInfo: nodes) nodeAdded(driverNode, nodeInfo);
      }
      JPPFTreeTable treeTable = panel.getTreeTable();
      if (treeTable != null) {
        treeTable.expand(panel.getTreeTableRoot());
        treeTable.expand(driverNode);
      }
      repaintTreeTable();
    } catch(RuntimeException | Error e) {
      log.debug(e.getMessage(), e);
    }
  }

  /**
   * Called to notify that a driver was removed.
   * @param driverUuid the name of the driver to remove.
   * @param removeNodesOnly true if only the nodes attached to the driver are to be removed.
   */
  void driverRemoved(final String driverUuid, final boolean removeNodesOnly)
  {
    if (debugEnabled) log.debug("removing driver: " + driverUuid);
    final DefaultMutableTreeNode driverNode = findDriver(driverUuid);
    if (driverNode == null) return;
    TopologyData driverData = (TopologyData) driverNode.getUserObject();
    driverMap.remove(driverData.getUuid());
    try {
      int n = driverNode.getChildCount();
      int count = 0;
      for (int i=n-1; i>=0; i--) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode ) driverNode.getChildAt(i);
        TopologyData nodeData = (TopologyData) node.getUserObject();
        if (nodeData.isNode()) {
          try {
            if (nodeData.getJmxWrapper() != null) nodeData.getJmxWrapper().close();
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }
          panel.getModel().removeNodeFromParent(node);
          fireNodeRemoved(driverData, nodeData);
          count++;
        }
      }
      panel.updateStatusBar("/StatusNbNodes", -count);
      if (!removeNodesOnly) {
        panel.getModel().removeNodeFromParent(driverNode);
        fireDriverRemoved(driverData);
        panel.updateStatusBar("/StatusNbServers", -1);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    repaintTreeTable();
  }

  /**
   * Called to notify that a node was added to a driver.
   * @param driverNode the driver to which the node is added.
   * @param nodeInfo the object that encapsulates the node addition.
   */
  void nodeAdded(final DefaultMutableTreeNode driverNode, final JPPFManagementInfo nodeInfo) {
    if (findNode(driverNode, nodeInfo.getUuid()) != null) return;
    String nodeUuid = nodeInfo.getUuid();
    if (debugEnabled) log.debug("attempting to add node={} to driver={}", nodeInfo, driverNode);
    int index = nodeInsertIndex(driverNode, nodeUuid, nodeInfo.toDisplayString());
    if (index < 0) return;
    if (debugEnabled) log.debug("adding node: " + nodeUuid + " at index " + index);
    TopologyData data = null;
    TopologyData peerData = null;
    if (nodeInfo.isPeer()) {
      DefaultMutableTreeNode tmpNode = findDriver(nodeInfo.getUuid());
      if (tmpNode != null) {
        if (debugEnabled) log.debug("adding peer node: " + nodeUuid + " at index " + index);
        peerData = (TopologyData) tmpNode.getUserObject();
      }
      data = new TopologyData(nodeInfo, peerData == null ? null : peerData.getJmxWrapper());
      peerMap.put(data.getUuid(), data);
    } else {
      data = new TopologyData(nodeInfo);
      nodeMap.put(data.getUuid(), data);
    }
    data.setParent((TopologyData) driverNode.getUserObject());
    DefaultMutableTreeNode nodeNode = new DefaultMutableTreeNode(data);
    panel.getModel().insertNodeInto(nodeNode, driverNode, index);
    fireNodeAdded((TopologyData) driverNode.getUserObject(), data, peerData);
    if (data.isNode()) panel.updateStatusBar("/StatusNbNodes", 1);

    for (int i=0; i<panel.getTreeTableRoot().getChildCount(); i++) {
      DefaultMutableTreeNode driverNode2 = (DefaultMutableTreeNode) panel.getTreeTableRoot().getChildAt(i);
      if (driverNode2 == driverNode) continue;
      DefaultMutableTreeNode nodeNode2 = findNode(driverNode2, nodeInfo.getUuid());
      if (nodeNode2 != null) {
        TopologyData tmp = (TopologyData) nodeNode2.getUserObject();
        if (tmp.getNodeInformation().isNode()) {
          tmp.setParent(null);
          panel.getModel().removeNodeFromParent(nodeNode2);
        }
      }
    }
    TopologyData driverData = (TopologyData) driverNode.getUserObject();
    if ((driverNode.getChildCount() == 1) && !driverData.isCollapsed()) panel.getTreeTable().expand(driverNode);
    repaintTreeTable();
  }

  /**
   * Called to notify that a node was removed from a driver.
   * @param driverUuid the name of the driver from which the node is removed.
   * @param nodeUuid the name of the node to remove.
   */
  void nodeRemoved(final String driverUuid, final String nodeUuid) {
    if (debugEnabled) log.debug("attempting to remove node=" + nodeUuid + " from driver=" + driverUuid);
    DefaultMutableTreeNode driver = findDriver(driverUuid);
    if (driver == null) return;
    final DefaultMutableTreeNode node = findNode(driver, nodeUuid);
    if (node == null) return;
    if (debugEnabled) log.debug("removing node: " + nodeUuid);
    TopologyData nodeData = (TopologyData) node.getUserObject();
    panel.getModel().removeNodeFromParent(node);
    if (nodeData != null) {
      nodeData.setParent(null);
      fireNodeRemoved((TopologyData) driver.getUserObject(), nodeData);
      if (nodeData.isNode()) {
        nodeMap.remove(nodeData.getUuid());
        panel.updateStatusBar("/StatusNbNodes", -1);
      }
      else peerMap.remove(nodeData.getUuid());
    }
    repaintTreeTable();
  }

  /**
   * Find the driver tree node with the specified driver name.
   * @param driverUuid name of the driver to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  DefaultMutableTreeNode findDriver(final String driverUuid) {
    for (int i=0; i<panel.getTreeTableRoot().getChildCount(); i++) {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) panel.getTreeTableRoot().getChildAt(i);
      TopologyData data = (TopologyData) driverNode.getUserObject();
      if (data.getUuid().equals(driverUuid)) return driverNode;
    }
    return null;
  }

  /**
   * Find the driver tree node with the specified connection.
   * @param driverConnection the driver connection to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  DefaultMutableTreeNode findDriver(final JPPFClientConnection driverConnection) {
    if (driverConnection == null) return null;
    for (int i=0; i<panel.getTreeTableRoot().getChildCount(); i++) {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) panel.getTreeTableRoot().getChildAt(i);
      TopologyData data = (TopologyData) driverNode.getUserObject();
      if (data.getClientConnection() == driverConnection) return driverNode;
    }
    return null;
  }

  /**
   * Find the node tree node with the specified driver name and node information.
   * @param driverNode name the parent of the node to find.
   * @param nodeUuid the name of the node to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  DefaultMutableTreeNode findNode(final DefaultMutableTreeNode driverNode, final String nodeUuid) {
    for (int i=0; i<driverNode.getChildCount(); i++) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
      TopologyData nodeData = (TopologyData) node.getUserObject();
      if (nodeUuid.equals(nodeData.getUuid())) return node;
    }
    return null;
  }

  /**
   * Find the driver to which the specified node is attached.
   * @param nodeUuid the uuid of the node to look for.
   * @return a {@link DefaultMutableTreeNode} instance, or null if node driver could be found for the node.
   */
  public DefaultMutableTreeNode findDriverForNode(final String nodeUuid)
  {
    for (int i=0; i<panel.getTreeTableRoot().getChildCount(); i++) {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) panel.getTreeTableRoot().getChildAt(i);
      DefaultMutableTreeNode nodeNode = findNode(driverNode, nodeUuid);
      if (nodeNode != null) return driverNode;
    }
    return null;
  }

  /**
   * Find the position at which to insert a driver,
   * using the sorted lexical order of driver names.
   * @param driverUuid the name of the driver to insert.
   * @return the index at which to insert the driver, or -1 if the driver is already in the tree.
   */
  int driverInsertIndex(final String driverUuid) {
    int n = panel.getTreeTableRoot().getChildCount();
    for (int i=0; i<n; i++) {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) panel.getTreeTableRoot().getChildAt(i);
      TopologyData data = (TopologyData) driverNode.getUserObject();
      if (data.getUuid().equals(driverUuid)) return -1;
      else if (driverUuid.compareTo(data.getUuid()) < 0) return i;
    }
    return n;
  }

  /**
   * Find the position at which to insert a node, using the sorted lexical order of node names.
   * @param driverNode name the parent of the node to insert.
   * @param nodeUuid the uuid of the node to insert.
   * @param nodeName the name of the node to insert.
   * @return the index at which to insert the node, or -1 if the node is already in the tree.
   */
  int nodeInsertIndex(final DefaultMutableTreeNode driverNode, final String nodeUuid, final String nodeName) {
    int n = driverNode.getChildCount();
    for (int i=0; i<n; i++) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
      TopologyData nodeData = (TopologyData) node.getUserObject();
      if (nodeUuid.equals(nodeData.getUuid())) return -1;
      else {
        String name = nodeData.getNodeInformation().toDisplayString();
        if (nodeName.compareTo(name) < 0) return i;
      }
    }
    return n;
  }

  /**
   * Repaint the tree table area.
   */
  void repaintTreeTable() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        JPPFTreeTable treeTable = panel.getTreeTable();
        if (treeTable != null) {
          treeTable.invalidate();
          treeTable.repaint();
        }
      }
    });
  }

  /**
   * Get the mapping of driver uuids to the corresponding {@link TopologyData} objects.
   * @return a map of driver uuids.
   */
  public Map<String, TopologyData> getDriverMap() {
    return driverMap;
  }

  /**
   * Get the mapping of peer driver uuids to the corresponding {@link TopologyData} objects.
   * @return a map of peer driver uuids.
   */
  public Map<String, TopologyData> getPeerMap() {
    return peerMap;
  }

  /**
   * Get the mapping of node uuids to the corresponding {@link TopologyData} objects.
   * @return a map of node uuids.
   */
  public Map<String, TopologyData> getNodeMap() {
    return nodeMap;
  }

  /**
   * Add a topology change listener.
   * @param listener the listener to add.
   */
  void addTopologyChangeListener(final TopologyChangeListener listener) {
    if (listener == null) throw new IllegalArgumentException("cannot add a null listener");
    listeners.add(listener);
  }

  /**
   * Remove a topology change listener.
   * @param listener the listener to remove.
   */
  void removeTopologyChangeListener(final TopologyChangeListener listener) {
    if (listener == null) throw new IllegalArgumentException("cannot remove a null listener");
    listeners.add(listener);
  }

  /**
   * Notify all listeners that a driver was added.
   * @param driverData the driver that was added.
   */
  void fireDriverAdded(final TopologyData driverData) {
    if (debugEnabled) log.debug("adding driver {}", driverData);
    TopologyChangeEvent event = new TopologyChangeEvent(panel, driverData, null, null);
    for (TopologyChangeListener listener: listeners) listener.driverAdded(event);
  }

  /**
   * Notify all listeners that a driver was added.
   * @param driverData the driver that was removed.
   */
  void fireDriverRemoved(final TopologyData driverData) {
    if (debugEnabled) log.debug("removing driver {}", driverData);
    TopologyChangeEvent event = new TopologyChangeEvent(panel, driverData, null, null);
    for (TopologyChangeListener listener: listeners) listener.driverRemoved(event);
  }

  /**
   * Notify all listeners that a driver was added.
   * @param driverData the driver to which the node is attached.
   * @param nodeData the node that was added.
   * @param peerData the peer driver that was added, if the node is a peer, <code>null</code> otherwise.
   */
  void fireNodeAdded(final TopologyData driverData, final TopologyData nodeData, final TopologyData peerData) {
    if (debugEnabled) log.debug("adding node {} to driver {}", nodeData, driverData);
    TopologyChangeEvent event = new TopologyChangeEvent(panel, driverData, nodeData, peerData);
    for (TopologyChangeListener listener: listeners) listener.nodeAdded(event);
  }

  /**
   * Notify all listeners that a node was removed.
   * @param driverData the driver to which the node is attached.
   * @param nodeData the node that was removed.
   */
  void fireNodeRemoved(final TopologyData driverData, final TopologyData nodeData) {
    if (debugEnabled) log.debug("removing node {} from driver {}", nodeData, driverData);
    TopologyChangeEvent event = new TopologyChangeEvent(panel, driverData, nodeData, null);
    for (TopologyChangeListener listener: listeners) listener.nodeRemoved(event);
  }

  /**
   * Notify all listeners that a driver was added.
   * @param driverData the driver that was updated or to which the updated node is attached.
   * @param nodeData the node that was updated, or <code>null</code> if it is a driver that was updated.
   */
  void fireDataUpdated(final TopologyData driverData, final TopologyData nodeData) {
    TopologyChangeEvent event = new TopologyChangeEvent(panel, driverData, nodeData, null);
    for (TopologyChangeListener listener: listeners) listener.dataUpdated(event);
  }
}
