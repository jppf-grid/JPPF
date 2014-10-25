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
package org.jppf.ui.utils;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.monitoring.topology.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.ui.monitoring.data.StatsHandler;


/**
 * Collection utility methods for manipulating a JTreeTable and its model.
 * @author Laurent Cohen
 */
public final class TreeTableUtils {
  /**
   * Find the position at which to insert a driver, using the sorted lexical order of driver names.
   * @param root the parent tree node for the driver to insert.
   * @param driver the driver to insert.
   * @return the index at which to insert the driver, or -1 if the driver is already in the tree.
   */
  public static int driverInsertIndex(final DefaultMutableTreeNode root, final TopologyDriver driver) {
    int n = root.getChildCount();
    for (int i=0; i<n; i++) {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) root.getChildAt(i);
      TopologyDriver data = (TopologyDriver) driverNode.getUserObject();
      if (data.getUuid().equals(driver.getUuid())) return -1;
      else if (driver.getDisplayName().compareTo(data.getDisplayName()) < 0) return i;
    }
    return n;
  }

  /**
   * Find the position at which to insert a node, using the sorted lexical order of node names.
   * @param driverNode the parent of the node to insert.
   * @param topologyNode the node to insert.
   * @return the index at which to insert the node, or -1 if the node is already in the tree.
   */
  public static int nodeInsertIndex(final DefaultMutableTreeNode driverNode, final TopologyNode topologyNode) {
    int n = driverNode.getChildCount();
    for (int i=0; i<n; i++) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
      TopologyNode nodeData = (TopologyNode) node.getUserObject();
      if (topologyNode.getUuid().equals(nodeData.getUuid())) return -1;
      else {
        if (topologyNode.getDisplayName().compareTo(nodeData.getDisplayName()) < 0) return i;
      }
    }
    return n;
  }

  /**
   * Find the driver tree node with the specified driver uuid.
   * @param root the parent tree node for the driver to find.
   * @param driverUuid uuid of the driver to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  public static DefaultMutableTreeNode findDriver(final DefaultMutableTreeNode root, final String driverUuid) {
    for (int i=0; i<root.getChildCount(); i++) {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) root.getChildAt(i);
      TopologyDriver data = (TopologyDriver) driverNode.getUserObject();
      if (data.getUuid().equals(driverUuid)) return driverNode;
    }
    return null;
  }

  /**
   * Find the node tree node with the specified driver uuid and node uuid.
   * @param driverNode uuid of the parent of the node to find.
   * @param nodeUuid the uuid of the node to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  public static DefaultMutableTreeNode findNode(final DefaultMutableTreeNode driverNode, final String nodeUuid) {
    for (int i=0; i<driverNode.getChildCount(); i++) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
      TopologyNode nodeData = (TopologyNode) node.getUserObject();
      if (nodeUuid.equals(nodeData.getUuid())) return node;
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
}
