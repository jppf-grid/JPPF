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

import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.monitoring.topology.*;
import org.jppf.management.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.treetable.AbstractJPPFTreeTableModel;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Utility methods for manipulating the topology tree model.
 * @author Laurent Cohen
 */
public class TopologyUtils {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(TopologyUtils.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

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
        if (TreeTableUtils.debugEnabled) TreeTableUtils.log.debug("adding driver: " + driver + " at index " + index);
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
    if (TreeTableUtils.debugEnabled) TreeTableUtils.log.debug("removing driver: " + driverData);
    DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    String uuid = driverData.getUuid();
    DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, uuid);
    if (driverNode == null) return;
    model.removeNodeFromParent(driverNode);
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
    if (TreeTableUtils.debugEnabled) TreeTableUtils.log.debug("attempting to add node={} to driver={}", nodeData, driverData);
    int index = TreeTableUtils.insertIndex(driverNode, nodeData);
    if (index < 0) return null;
    if (TreeTableUtils.debugEnabled) TreeTableUtils.log.debug("adding node: " + nodeUuid + " at index " + index);
    DefaultMutableTreeNode nodeNode = new DefaultMutableTreeNode(nodeData);
    model.insertNodeInto(nodeNode, driverNode, index);
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
    if (TreeTableUtils.debugEnabled) TreeTableUtils.log.debug("attempting to remove node=" + nodeData + " from driver=" + driverData);
    DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    DefaultMutableTreeNode driver = TreeTableUtils.findComponent(treeTableRoot, driverData.getUuid());
    if (driver == null) return;
    String nodeUuid = nodeData.getUuid();
    final DefaultMutableTreeNode node = TreeTableUtils.findComponent(driver, nodeUuid);
    if (node != null) {
      if (TreeTableUtils.debugEnabled) TreeTableUtils.log.debug("removing node: " + nodeData);
      model.removeNodeFromParent(node);
    }
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
   * Retrieve the system information for the specified topology object.
   * @param data the topology object for which to get the information.
   * @return a {@link JPPFSystemInformation} or <code>null</code> if the information could not be retrieved.
   */
  public static JPPFSystemInformation retrieveSystemInfo(final AbstractTopologyComponent data) {
    JPPFSystemInformation info = null;
    try {
      if (data.isNode()) {
        TopologyDriver parent = (TopologyDriver) data.getParent();
        Map<String, Object> result = parent.getForwarder().systemInformation(new UuidSelector(data.getUuid()));
        Object o = result.get(data.getUuid());
        if (o instanceof JPPFSystemInformation) info = (JPPFSystemInformation) o;
      } else {
        if (data.isPeer()) {
          String uuid = ((TopologyPeer) data).getUuid();
          if (uuid != null) {
            TopologyDriver driver = StatsHandler.getInstance().getTopologyManager().getDriver(uuid);
            if (driver != null) info = driver.getJmx().systemInformation();
          }
        }
        else info = ((TopologyDriver) data).getJmx().systemInformation();
      }
    } catch (Exception e) {
      if (TreeTableUtils.debugEnabled) TreeTableUtils.log.debug(e.getMessage(), e);
    }
    return info;
  }
}
