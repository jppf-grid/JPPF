/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
package org.jppf.ui.utils;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.monitoring.AbstractComponent;
import org.jppf.client.monitoring.topology.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.treetable.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;


/**
 * Collection utility methods for manipulating a JTreeTable and its model.
 * @author Laurent Cohen
 */
public final class TreeTableUtils {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(TreeTableUtils.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Find the position at which to insert a driver, using the sorted lexical order of driver names.
   * @param parent the parent tree node for the driver to insert.
   * @param comp the driver to insert.
   * @return the index at which to insert the driver, or -1 if the driver is already in the tree.
   */
  public static int insertIndex(final DefaultMutableTreeNode parent, final AbstractComponent<?> comp) {
    //if (findComponent(root, comp.getUuid()) == null) return -1;
    int n = parent.getChildCount();
    for (int i=0; i<n; i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
      AbstractComponent<?> childData = (AbstractComponent<?>) child.getUserObject();
      if (childData == null) return -1;
      if (childData.getUuid().equals(comp.getUuid())) return -1;
      else if (comp.getDisplayName().compareTo(childData.getDisplayName()) < 0) return i;
    }
    return n;
  }

  /**
   * Find the tree node with the specified component uuid int he children of the specified parent.
   * @param parent the parent tree node for the component to find.
   * @param uuid uuid of the component to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  public static DefaultMutableTreeNode findComponent(final DefaultMutableTreeNode parent, final String uuid) {
    if (uuid == null) return null;
    for (int i=0; i<parent.getChildCount(); i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
      AbstractComponent<?> data = (AbstractComponent<?>) child.getUserObject();
      if (data == null) continue;
      if (data.getUuid().equals(uuid)) return child;
    }
    return null;
  }

  /**
   * COmpute a display name for the given topology component.
   * @param comp the ocmponent for which to get a display name.
   * @return the display name as a string.
   */
  public static String getDisplayName(final AbstractTopologyComponent comp) {
    StatsHandler handler = StatsHandler.getInstance();
    JPPFManagementInfo info = null;
    if (comp.isPeer()) {
      TopologyDriver driver = handler.getTopologyManager().getDriver(comp.getUuid());
      if (driver != null) info = driver.getManagementInfo();
    } else info = comp.getManagementInfo();
    if (info != null) return (handler.isShowIP() ? info.getIpAddress() : info.getHost()) + ":" + info.getPort();
    return comp.getDisplayName();
  }

  /**
   * Get the path to an icon for the node given its state.
   * @param info represents the the node.
   * @return the path to an icon.
   */
  public static String getNodeIconPath(final JPPFManagementInfo info) {
    if (info.isMasterNode()) return info.isDotnetCapable() ? AbstractTreeCellRenderer.NODE_MASTER_DOTNET_ICON : AbstractTreeCellRenderer.NODE_MASTER_ICON;
    return info.isDotnetCapable() ? AbstractTreeCellRenderer.NODE_DOTNET_ICON : AbstractTreeCellRenderer.NODE_ICON;
  }

  /**
   * Add the specified driver to the treeTable.
   * @param model the tree table model.
   * @param driver the driver to add.
   * @return the newly created {@link DefaultMutableTreeNode}, if any.
   */
  public static synchronized DefaultMutableTreeNode addDriver(final AbstractJPPFTreeTableModel model, final TopologyDriver driver) {
    DefaultMutableTreeNode driverNode = null;
    if (!driver.getConnection().getStatus().isWorkingStatus()) return null;
    String uuid = driver.getUuid();
    DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    driverNode = TreeTableUtils.findComponent(treeTableRoot, uuid);
    if (driverNode == null) {
      int index = TreeTableUtils.insertIndex(treeTableRoot, driver);
      if (index >= 0) {
        driverNode = new DefaultMutableTreeNode(driver);
        if (debugEnabled) log.debug("adding driver: " + driver + " at index " + index);
        model.insertNodeInto(driverNode, treeTableRoot, index);
      }
    }
    return driverNode;
  }

  /**
   * Remove the specified driver from the treeTable.
   * @param model the tree table model.
   * @param driverData the driver to add.
   */
  public static synchronized void removeDriver(final AbstractJPPFTreeTableModel model, final TopologyDriver driverData) {
    if (debugEnabled) log.debug("removing driver: " + driverData);
    DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    String uuid = driverData.getUuid();
    DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, uuid);
    if (driverNode == null) return;
    model.removeNodeFromParent(driverNode);
  }

  /**
   * Update the specified node.
   * @param model the tree table model.
   * @param driverData the driver to add to.
   * @param nodeData the node to add.
   */
  public static synchronized void updateNode(final AbstractJPPFTreeTableModel model, final TopologyDriver driverData, final TopologyNode nodeData) {
    DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    final DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, driverData.getUuid());
    if ((driverNode != null) && (nodeData != null)) {
      final DefaultMutableTreeNode node = TreeTableUtils.findComponent(driverNode, nodeData.getUuid());
      if (node != null) model.changeNode(node);
    }
  }

  /**
   * Add the specified node to the specified driver in the treeTable.
   * @param model the tree table model.
   * @param driverData the driver to add to.
   * @param nodeData the node to add.
   * @return the newly created {@link DefaultMutableTreeNode}, if any.
   */
  public static synchronized DefaultMutableTreeNode addNode(final AbstractJPPFTreeTableModel model, final TopologyDriver driverData, final TopologyNode nodeData) {
    if ((driverData == null) || (nodeData == null)) return null;
    DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, driverData.getUuid());
    if (driverNode == null) return null;
    String nodeUuid = nodeData.getUuid();
    if (TreeTableUtils.findComponent(driverNode, nodeUuid) != null) return null;
    if (debugEnabled) log.debug("attempting to add node={} to driver={}", nodeData, driverData);
    int index = TreeTableUtils.insertIndex(driverNode, nodeData);
    if (index < 0) return null;
    if (debugEnabled) log.debug("adding node: " + nodeUuid + " at index " + index);
    DefaultMutableTreeNode nodeNode = new DefaultMutableTreeNode(nodeData);
    model.insertNodeInto(nodeNode, driverNode, index);
    /*
    if ((driverNode.getChildCount() == 1) && !driverData.isCollapsed()) treeTable.expand(driverNode);
    */
    return nodeNode;
  }

  /**
   * Remove the specified node from the specified driver in the treeTable.
   * @param model the tree table model.
   * @param driverData the driver to add to.
   * @param nodeData the node to add.
   */
  public static synchronized void removeNode(final AbstractJPPFTreeTableModel model, final TopologyDriver driverData, final TopologyNode nodeData) {
    if ((driverData == null) || (nodeData == null)) return;
    if (debugEnabled) log.debug("attempting to remove node=" + nodeData + " from driver=" + driverData);
    DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    DefaultMutableTreeNode driver = TreeTableUtils.findComponent(treeTableRoot, driverData.getUuid());
    if (driver == null) return;
    String nodeUuid = nodeData.getUuid();
    final DefaultMutableTreeNode node = TreeTableUtils.findComponent(driver, nodeUuid);
    if (node != null) {
      if (debugEnabled) log.debug("removing node: " + nodeData);
      model.removeNodeFromParent(node);
    }
  }
}
