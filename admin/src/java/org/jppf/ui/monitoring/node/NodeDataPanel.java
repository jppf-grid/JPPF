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

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.JPPFClientConnection;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.data.*;
import org.jppf.ui.monitoring.node.actions.*;
import org.jppf.ui.monitoring.topology.*;
import org.jppf.ui.treetable.*;
import org.slf4j.*;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
 */
public class NodeDataPanel extends AbstractTreeTableOption implements TopologyListener {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(NodeDataPanel.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Manages the topology updates.
   */
  private final TopologyManager manager;

  /**
   * Initialize this panel with the specified information.
   */
  public NodeDataPanel() {
    BASE = "org.jppf.ui.i18n.NodeDataPage";
    if (debugEnabled) log.debug("initializing NodeDataPanel");
    manager = TopologyManager.getInstance();
    createTreeTableModel();
    manager.addTopologyListener(this);
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private void createTreeTableModel() {
    treeTableRoot = new DefaultMutableTreeNode(localize("tree.root.name"));
    model = new NodeTreeTableModel(treeTableRoot);
    populateTreeTableModel();
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private void populateTreeTableModel() {
    for (TopologyDriver driver: manager.getDrivers()) {
      addDriver(driver);
      for (AbstractTopologyComponent child: driver.getChildren()) addNode(driver, (TopologyNode) child);
    }
  }

  /**
   * Initialize the refresh of tree structure.
   */
  public void init() {
    if (debugEnabled) log.debug("initializing tree model");
    populateTreeTableModel();
    TopologyManager.getInstance().addTopologyListener(this);
  }
  /**
   * Remove all drivers and nodes from the tree table.
   */
  private void clearTreeTableModel() {
    for (TopologyDriver driver: manager.getDrivers()) removeDriver(driver);
  }

  /**
   * Create, initialize and layout the GUI components displayed in this panel.
   */
  @Override
  public void createUI() {
    treeTable = new JPPFTreeTable(model);
    treeTable.getTree().setLargeModel(true);
    treeTable.getTree().setRootVisible(false);
    treeTable.getTree().setShowsRootHandles(true);
    treeTable.expandAll();
    treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
    treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    treeTable.doLayout();
    treeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    treeTable.getTree().setCellRenderer(new NodeRenderer());
    treeTable.setDefaultRenderer(Object.class, new NodeTableCellRenderer());
    JScrollPane sp = new JScrollPane(treeTable);
    setUIComponent(sp);
    treeTable.expandAll();
  }

  /**
   * Initialize all actions used in the panel.
   */
  public void setupActions() {
    actionHandler = new JTreeTableActionHandler(treeTable);
    actionHandler.putAction("shutdown.restart.driver", new ServerShutdownRestartAction());
    actionHandler.putAction("driver.reset.statistics", new ServerStatisticsResetAction());
    actionHandler.putAction("update.configuration", new NodeConfigurationAction());
    actionHandler.putAction("show.information", new SystemInformationAction());
    actionHandler.putAction("update.threads", new NodeThreadsAction());
    actionHandler.putAction("reset.counter", new ResetTaskCounterAction());
    actionHandler.putAction("restart.node", new ShutdownOrRestartNodeAction(true, true, "restart.node"));
    actionHandler.putAction("restart.node.deferred", new ShutdownOrRestartNodeAction(true, false, "restart.node.deferred"));
    actionHandler.putAction("shutdown.node", new ShutdownOrRestartNodeAction(false, true, "shutdown.node"));
    actionHandler.putAction("shutdown.node.deferred", new ShutdownOrRestartNodeAction(false, false, "shutdown.node.deferred"));
    actionHandler.putAction("toggle.active", new ToggleNodeActiveAction());
    actionHandler.putAction("node.provisioning", new ProvisioningAction());
    actionHandler.putAction("select.drivers", new SelectDriversAction(this));
    actionHandler.putAction("select.nodes", new SelectNodesAction(this));
    actionHandler.updateActions();
    treeTable.addMouseListener(new NodeTreeTableMouseListener(actionHandler));
    Runnable r = new ActionsInitializer(this, "/topology.toolbar");
    Runnable r2 = new ActionsInitializer(this, "/topology.toolbar.bottom");
    new Thread(r).start();
    new Thread(r2).start();
  }

  /**
   * Add the specified driver to the treeTable.
   * @param driver the driver to add.
   */
  private void addDriver(final TopologyDriver driver) {
    try {
      if (!driver.getConnection().getStatus().isWorkingStatus()) return;
      String uuid = driver.getUuid();
      if (findDriver(uuid) != null) return;
      JMXDriverConnectionWrapper jmx = driver.getJmx();
      int index = driverInsertIndex(uuid);
      if (index < 0) return;
      ConnectionDataHolder cdh = StatsHandler.getInstance().getConnectionDataHolder(driver.getConnection());
      if (cdh != null) cdh.setDriverData(driver);
      DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(driver);
      if (debugEnabled) log.debug("adding driver: " + driver + " at index " + index);
      model.insertNodeInto(driverNode, treeTableRoot, index);
      if (treeTable != null) {
        treeTable.expand(treeTableRoot);
        treeTable.expand(driverNode);
      }
      repaintTreeTable();
    } catch(RuntimeException | Error e) {
      log.debug(e.getMessage(), e);
    }
  }

  /**
   * Remove the specified driver from the treeTable.
   * @param driverData the driver to add.
   */
  private void removeDriver(final TopologyDriver driverData) {
    if (debugEnabled) log.debug("removing driver: " + driverData);
    String uuid = driverData.getUuid();
    DefaultMutableTreeNode driverNode = findDriver(uuid);
    if (driverNode == null) return;
    model.removeNodeFromParent(driverNode);
    repaintTreeTable();
  }

  /**
   * Add the specified node to the specified driver in the treeTable.
   * @param driverData the driver to add to.
   * @param nodeData the node to add.
   */
  private void addNode(final TopologyDriver driverData, final TopologyNode nodeData) {
    if ((driverData == null) || (nodeData == null)) return;
    DefaultMutableTreeNode driverNode = findDriver(driverData.getUuid());
    if (driverNode == null) return;
    String nodeUuid = nodeData.getUuid();
    if (findNode(driverNode, nodeUuid) != null) return;
    if (debugEnabled) log.debug("attempting to add node={} to driver={}", nodeData, driverData);
    int index = nodeInsertIndex(driverNode, nodeUuid, nodeData.getManagementInfo().toDisplayString());
    if (index < 0) return;
    if (debugEnabled) log.debug("adding node: " + nodeUuid + " at index " + index);
    DefaultMutableTreeNode nodeNode = new DefaultMutableTreeNode(nodeData);
    model.insertNodeInto(nodeNode, driverNode, index);

    for (int i=0; i<treeTableRoot.getChildCount(); i++) {
      DefaultMutableTreeNode driverNode2 = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
      if (driverNode2 == driverNode) continue;
      DefaultMutableTreeNode nodeNode2 = findNode(driverNode2, nodeUuid);
      if (nodeNode2 != null) {
        TopologyNode tmp = (TopologyNode) nodeNode2.getUserObject();
        if (tmp.getManagementInfo().isNode()) {
          tmp.setParent(null);
          model.removeNodeFromParent(nodeNode2);
        }
      }
    }
    if ((driverNode.getChildCount() == 1) && !driverData.isCollapsed()) treeTable.expand(driverNode);
    repaintTreeTable();
  }

  /**
   * Remove the specified node from the specified driver in the treeTable.
   * @param driverData the driver to add to.
   * @param nodeData the node to add.
   */
  private void removeNode(final TopologyDriver driverData, final TopologyNode nodeData) {
    if ((driverData == null) || (nodeData == null)) return;
    if (debugEnabled) log.debug("attempting to remove node=" + nodeData + " from driver=" + driverData);
    DefaultMutableTreeNode driver = findDriver(driverData.getUuid());
    if ((driver == null) || (nodeData == null)) return;
    String nodeUuid = nodeData.getUuid();
    final DefaultMutableTreeNode node = findNode(driver, nodeUuid);
    if (node != null) {
      if (debugEnabled) log.debug("removing node: " + nodeData);
      model.removeNodeFromParent(node);
      repaintTreeTable();
    }
  }

  @Override
  public void driverAdded(final TopologyEvent event) {
    addDriver(event.getDriverData());
  }

  @Override
  public void driverRemoved(final TopologyEvent event) {
    removeDriver(event.getDriverData());
  }

  @Override
  public void nodeAdded(final TopologyEvent event) {
    addNode(event.getDriverData(), nodeFromEvent(event));
  }

  @Override
  public void nodeRemoved(final TopologyEvent event) {
    removeNode(event.getDriverData(),  nodeFromEvent(event));
  }

  @Override
  public void nodeUpdated(final TopologyEvent event) {
    TopologyDriver driverData = event.getDriverData();
    final DefaultMutableTreeNode driverNode = findDriver(driverData.getUuid());
    if (driverNode == null) return;
    TopologyNode nodeData = nodeFromEvent(event);
    if (nodeData == null) return;
    final DefaultMutableTreeNode node = findNode(driverNode, nodeData.getUuid());
    if (node != null) model.changeNode(node);
  }

  /**
   * Get the topology node held by the sepcified event.
   * @param event the event to analyze.
   * @return a {@link TopologyNode} object.
   */
  private TopologyNode nodeFromEvent(final TopologyEvent event) {
    TopologyNode nodeData = event.getNodeData();
    if (nodeData == null) nodeData = event.getPeerData();
    return nodeData;
  }

  /**
   * Find the driver tree node with the specified driver name.
   * @param driverUuid name of the driver to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  private DefaultMutableTreeNode findDriver(final String driverUuid) {
    for (int i=0; i<treeTableRoot.getChildCount(); i++) {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
      TopologyDriver data = (TopologyDriver) driverNode.getUserObject();
      if (data.getUuid().equals(driverUuid)) return driverNode;
    }
    return null;
  }

  /**
   * Find the driver tree node with the specified connection.
   * @param driverConnection the driver connection to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  private DefaultMutableTreeNode findDriver(final JPPFClientConnection driverConnection) {
    if (driverConnection == null) return null;
    for (int i=0; i<treeTableRoot.getChildCount(); i++) {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
      TopologyDriver data = (TopologyDriver) driverNode.getUserObject();
      if (data.getConnection() == driverConnection) return driverNode;
    }
    return null;
  }

  /**
   * Find the node tree node with the specified driver name and node information.
   * @param driverNode name the parent of the node to find.
   * @param nodeUuid the name of the node to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  private DefaultMutableTreeNode findNode(final DefaultMutableTreeNode driverNode, final String nodeUuid) {
    for (int i=0; i<driverNode.getChildCount(); i++) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
      TopologyNode nodeData = (TopologyNode) node.getUserObject();
      if (nodeUuid.equals(nodeData.getUuid())) return node;
    }
    return null;
  }

  /**
   * Find the position at which to insert a driver,
   * using the sorted lexical order of driver names.
   * @param driverUuid the name of the driver to insert.
   * @return the index at which to insert the driver, or -1 if the driver is already in the tree.
   */
  private int driverInsertIndex(final String driverUuid) {
    int n = treeTableRoot.getChildCount();
    for (int i=0; i<n; i++) {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
      TopologyDriver data = (TopologyDriver) driverNode.getUserObject();
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
  private int nodeInsertIndex(final DefaultMutableTreeNode driverNode, final String nodeUuid, final String nodeName) {
    int n = driverNode.getChildCount();
    for (int i=0; i<n; i++) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
      TopologyNode nodeData = (TopologyNode) node.getUserObject();
      if (nodeUuid.equals(nodeData.getUuid())) return -1;
      else {
        String name = nodeData.getManagementInfo().toDisplayString();
        if (nodeName.compareTo(name) < 0) return i;
      }
    }
    return n;
  }

  /**
   * Repaint the tree table area.
   */
  private void repaintTreeTable() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (treeTable != null) {
          treeTable.invalidate();
          treeTable.repaint();
        }
      }
    });
  }
}
