/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.util.Collection;

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
public class NodeDataPanelManager
{
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
   * Initialize this manager.
   * @param panel the container for the tree table.
   */
  NodeDataPanelManager(final NodeDataPanel panel)
  {
    this.panel = panel;
  }

  /**
   * Called when the state information of a node has changed.
   * @param driverName the name of the driver to which the node is attached.
   * @param nodeName the name of the node to update.
   */
  void nodeDataUpdated(final String driverName, final String nodeName)
  {
    final DefaultMutableTreeNode driverNode = findDriver(driverName);
    if (driverNode == null) return;
    final DefaultMutableTreeNode node = findNode(driverNode, nodeName);
    if (node != null)
    {
      panel.getModel().changeNode(node);
      if (panel.getGraphOption() != null)
        panel.getGraphOption().getGraphHandler().nodeDataUpdated((TopologyData) driverNode.getUserObject(), (TopologyData) node.getUserObject());
    }
  }

  /**
   * Called to notify that a driver was added.
   * @param connection a reference to the driver connection.
   */
  void driverAdded(final JPPFClientConnection connection)
  {
    JMXDriverConnectionWrapper wrapper = ((JPPFClientConnectionImpl) connection).getJmxConnection();
    String driverName = wrapper.getId();
    int index = driverInsertIndex(driverName);
    if (index < 0) return;
    TopologyData driverData = new TopologyData(connection);
    DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(driverData);
    if (debugEnabled) log.debug("adding driver: " + driverName + " at index " + index);
    panel.getModel().insertNodeInto(driverNode, panel.getTreeTableRoot(), index);
    if (panel.getGraphOption() != null) panel.getGraphOption().getGraphHandler().driverAdded(driverData);
    if (panel.getListenerMap().get(wrapper.getId()) == null)
    {
      ConnectionStatusListener listener = new ConnectionStatusListener(panel, wrapper.getId());
      connection.addClientConnectionStatusListener(listener);
      panel.getListenerMap().put(wrapper.getId(), listener);
    }
    Collection<JPPFManagementInfo> nodes = null;
    try
    {
      nodes = wrapper.nodesInformation();
    }
    catch(Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
      return;
    }
    if (nodes != null) for (JPPFManagementInfo nodeInfo: nodes) nodeAdded(driverNode, nodeInfo);
    JPPFTreeTable treeTable = panel.getTreeTable();
    if (treeTable != null)
    {
      treeTable.expand(panel.getTreeTableRoot());
      treeTable.expand(driverNode);
    }
    panel.updateStatusBar("/StatusNbServers", 1);
    repaintTreeTable();
  }

  /**
   * Called to notify that a driver was removed.
   * @param driverName the name of the driver to remove.
   * @param removeNodesOnly true if only the nodes attached to the driver are to be removed.
   */
  void driverRemoved(final String driverName, final boolean removeNodesOnly)
  {
    if (debugEnabled) log.debug("removing driver: " + driverName);
    final DefaultMutableTreeNode driverNode = findDriver(driverName);
    if (driverNode == null) return;
    TopologyData driverData = (TopologyData) driverNode.getUserObject();
    try
    {
      int n = driverNode.getChildCount();
      int count = 0;
      for (int i=n-1; i>=0; i--)
      {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode ) driverNode.getChildAt(i);
        TopologyData nodeData = (TopologyData) node.getUserObject();
        if (nodeData.isNode())
        {
          try
          {
            if (nodeData.getJmxWrapper() != null) nodeData.getJmxWrapper().close();
          }
          catch (Exception e)
          {
            log.error(e.getMessage(), e);
          }
          panel.getModel().removeNodeFromParent(node);
          if (panel.getGraphOption() != null) panel.getGraphOption().getGraphHandler().nodeRemoved(driverData, nodeData);
          count++;
        }
      }
      panel.updateStatusBar("/StatusNbNodes", -count);
      if (!removeNodesOnly)
      {
        panel.getModel().removeNodeFromParent(driverNode);
        if (panel.getGraphOption() != null) panel.getGraphOption().getGraphHandler().driverRemoved(driverData);
        panel.updateStatusBar("/StatusNbServers", -1);
      }
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
    repaintTreeTable();
  }

  /**
   * Called to notify that a node was added to a driver.
   * @param driverName the name of the driver to which the node is added.
   * @param nodeInfo the object that encapsulates the node addition.
   */
  void nodeAdded(final String driverName, final JPPFManagementInfo nodeInfo)
  {
    final DefaultMutableTreeNode driverNode = findDriver(driverName);
    if (driverNode == null) return;
    nodeAdded(driverNode, nodeInfo);
  }

  /**
   * Called to notify that a node was added to a driver.
   * @param driverNode the driver to which the node is added.
   * @param nodeInfo the object that encapsulates the node addition.
   */
  void nodeAdded(final DefaultMutableTreeNode driverNode, final JPPFManagementInfo nodeInfo)
  {
    String nodeName = nodeInfo.getHost() + ':' + nodeInfo.getPort();
    if (debugEnabled) log.debug("attempting to add node=" + nodeName + " to driver=" + driverNode);
    int index = nodeInsertIndex(driverNode, nodeName);
    if (index < 0) return;
    if (debugEnabled) log.debug("adding node: " + nodeName + " at index " + index);
    TopologyData data = null;
    if (!nodeInfo.isNode())
    {
      DefaultMutableTreeNode tmpNode = findDriver(nodeName);
      if (tmpNode != null)
      {
        if (debugEnabled) log.debug("adding peer node: " + nodeName + " at index " + index);
        TopologyData tmpData = (TopologyData) tmpNode.getUserObject();
        data = new TopologyData(nodeInfo, tmpData.getJmxWrapper());
      }
    }
    if (data == null) data = new TopologyData(nodeInfo);
    //if (debugEnabled) log.debug("created TopologyData instance");
    DefaultMutableTreeNode nodeNode = new DefaultMutableTreeNode(data);
    panel.getModel().insertNodeInto(nodeNode, driverNode, index);
    if (panel.getGraphOption() != null) panel.getGraphOption().getGraphHandler().nodeAdded((TopologyData) driverNode.getUserObject(), data);
    if (nodeInfo.getType() == JPPFManagementInfo.NODE) panel.updateStatusBar("/StatusNbNodes", 1);

    for (int i=0; i<panel.getTreeTableRoot().getChildCount(); i++)
    {
      DefaultMutableTreeNode driverNode2 = (DefaultMutableTreeNode) panel.getTreeTableRoot().getChildAt(i);
      if (driverNode2 == driverNode) continue;
      DefaultMutableTreeNode nodeNode2 = findNode(driverNode2, nodeName);
      if (nodeNode2 != null)
      {
        TopologyData tmp = (TopologyData) nodeNode2.getUserObject();
        if (tmp.getNodeInformation().getType() == JPPFManagementInfo.NODE) panel.getModel().removeNodeFromParent(nodeNode2);
      }
    }
    TopologyData driverData = (TopologyData) driverNode.getUserObject();
    if ((driverNode.getChildCount() == 1) && !driverData.isCollapsed()) panel.getTreeTable().expand(driverNode);
    repaintTreeTable();
  }

  /**
   * Called to notify that a node was removed from a driver.
   * @param driverName the name of the driver from which the node is removed.
   * @param nodeName the name of the node to remove.
   */
  void nodeRemoved(final String driverName, final String nodeName)
  {
    if (debugEnabled) log.debug("attempting to remove node=" + nodeName + " from driver=" + driverName);
    DefaultMutableTreeNode driverNode = findDriver(driverName);
    if (driverNode == null) return;
    final DefaultMutableTreeNode node = findNode(driverNode, nodeName);
    if (node == null) return;
    if (debugEnabled) log.debug("removing node: " + nodeName);
    panel.getModel().removeNodeFromParent(node);
    TopologyData data = (TopologyData) node.getUserObject();
    if (panel.getGraphOption() != null) panel.getGraphOption().getGraphHandler().nodeRemoved((TopologyData) driverNode.getUserObject(), data);
    if ((data != null) && (data.getNodeInformation().getType() == JPPFManagementInfo.NODE)) panel.updateStatusBar("/StatusNbNodes", -1);
    repaintTreeTable();
  }

  /**
   * Find the driver tree node with the specified driver name.
   * @param driverName name of the driver to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  DefaultMutableTreeNode findDriver(final String driverName)
  {
    for (int i=0; i<panel.getTreeTableRoot().getChildCount(); i++)
    {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) panel.getTreeTableRoot().getChildAt(i);
      TopologyData data = (TopologyData) driverNode.getUserObject();
      String name = data.getJmxWrapper().getId();
      if (name.equals(driverName)) return driverNode;
    }
    return null;
  }

  /**
   * Find the node tree node with the specified driver name and node information.
   * @param driverNode name the parent of the node to find.
   * @param nodeName the name of the node to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  DefaultMutableTreeNode findNode(final DefaultMutableTreeNode driverNode, final String nodeName)
  {
    for (int i=0; i<driverNode.getChildCount(); i++)
    {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
      TopologyData nodeData = (TopologyData) node.getUserObject();
      if (nodeName.equals(nodeData.getJmxWrapper().getId())) return node;
    }
    return null;
  }

  /**
   * Find the position at which to insert a driver,
   * using the sorted lexical order of driver names.
   * @param driverName the name of the driver to insert.
   * @return the index at which to insert the driver, or -1 if the driver is already in the tree.
   */
  int driverInsertIndex(final String driverName)
  {
    int n = panel.getTreeTableRoot().getChildCount();
    for (int i=0; i<n; i++)
    {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) panel.getTreeTableRoot().getChildAt(i);
      TopologyData data = (TopologyData) driverNode.getUserObject();
      String name = data.getJmxWrapper().getId();
      if (name.equals(driverName)) return -1;
      else if (driverName.compareTo(name) < 0) return i;
    }
    return n;
  }

  /**
   * Find the position at which to insert a node, using the sorted lexical order of node names.
   * @param driverNode name the parent of the node to insert.
   * @param nodeName the name of the node to insert.
   * @return the index at which to insert the node, or -1 if the node is already in the tree.
   */
  int nodeInsertIndex(final DefaultMutableTreeNode driverNode, final String nodeName)
  {
    int n = driverNode.getChildCount();
    for (int i=0; i<n; i++)
    {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
      TopologyData nodeData = (TopologyData) node.getUserObject();
      String name = nodeData.getJmxWrapper().getId();
      if (nodeName.equals(name)) return -1;
      else if (nodeName.compareTo(name) < 0) return i;
    }
    return n;
  }

  /**
   * Repaint the tree table area.
   */
  void repaintTreeTable()
  {
    JPPFTreeTable treeTable = panel.getTreeTable();
    if (treeTable != null)
    {
      treeTable.invalidate();
      treeTable.repaint();
    }
  }
}
