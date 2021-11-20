/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.node.actions.*;
import org.jppf.ui.treetable.*;
import org.jppf.ui.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
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
   * Whether auto-refresh is on or off.
   */
  private boolean autoRefresh = true;

  /**
   * Initialize this panel with the specified information.
   */
  public NodeDataPanel() {
    BASE = "org.jppf.ui.i18n.NodeDataPage";
    if (debugEnabled) log.debug("initializing NodeDataPanel");
    manager = StatsHandler.getInstance().getTopologyManager();
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
  private synchronized  void populateTreeTableModel() {
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
    manager.addTopologyListener(this);
  }
  /**
   * Remove all drivers and nodes from the tree table.
   */
  public synchronized void refreshTreeTableModel() {
    for (TopologyDriver driver: manager.getDrivers()) {
      for (AbstractTopologyComponent child: driver.getChildren()) removeNode(driver, (TopologyNode) child);
      removeDriver(driver);
    }
    populateTreeTableModel();
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
    treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
    treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    treeTable.doLayout();
    treeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    treeTable.getTree().setCellRenderer(new NodeRenderer());
    treeTable.setDefaultRenderer(Object.class, new NodeTableCellRenderer(this));
    final JScrollPane sp = new JScrollPane(treeTable);
    GuiUtils.adjustScrollbarsThickness(sp);
    setUIComponent(sp);
    treeTable.expandAll();
    StatsHandler.getInstance().getShowIPHandler().addShowIPListener((event) -> treeTable.repaint());
  }

  /**
   * Initialize all actions used in the panel.
   */
  public void setupActions() {
    actionHandler = new JTreeTableActionHandler(treeTable);
    actionHandler.putAction("shutdown.restart.driver", new ServerShutdownRestartAction());
    actionHandler.putAction("load.balancing.settings", new LoadBalancingAction());
    actionHandler.putAction("driver.reset.statistics", new ServerStatisticsResetAction());
    actionHandler.putAction("update.configuration", new NodeConfigurationAction());
    actionHandler.putAction("show.information", new SystemInformationAction());
    actionHandler.putAction("update.threads", new NodeThreadsAction());
    actionHandler.putAction("reset.counter", new ResetTaskCounterAction());
    actionHandler.putAction("restart.node", new ShutdownOrRestartNodeAction(true, true, "restart.node"));
    actionHandler.putAction("restart.node.deferred", new ShutdownOrRestartNodeAction(true, false, "restart.node.deferred"));
    actionHandler.putAction("shutdown.node", new ShutdownOrRestartNodeAction(false, true, "shutdown.node"));
    actionHandler.putAction("shutdown.node.deferred", new ShutdownOrRestartNodeAction(false, false, "shutdown.node.deferred"));
    actionHandler.putAction("reconnect.node", new ReconnectNodeAction(true, "reconnect.node"));
    actionHandler.putAction("reconnect.node.deferred", new ReconnectNodeAction(false, "reconnect.node.deferred"));
    actionHandler.putAction("toggle.active", new ToggleNodeActiveAction());
    actionHandler.putAction("node.provisioning", new ProvisioningAction());
    actionHandler.putAction("select.drivers", new SelectDriversAction(this));
    actionHandler.putAction("select.nodes", new SelectNodesAction(this));
    actionHandler.putAction("node.show.hide", new ShowHideColumnsAction(this));
    actionHandler.putAction("cancel.deferred.action", new CancelDeferredAction());
    actionHandler.updateActions();
    treeTable.addMouseListener(new NodeTreeTableMouseListener(actionHandler));
    ThreadUtils.startThread(new ActionsInitializer(this, "/topology.toolbar"), "/topology.toolbar");
    ThreadUtils.startThread(new ActionsInitializer(this, "/topology.toolbar.bottom"), "/topology.toolbar.bottom");
  }

  /**
   * Add the specified driver to the treeTable.
   * @param driver the driver to add.
   */
  private synchronized void addDriver(final TopologyDriver driver) {
    try {
      final DefaultMutableTreeNode driverNode = TopologyUtils.addDriver(model, driver);
      if ((driverNode != null) && (treeTable != null)) {
        treeTable.expand(treeTableRoot);
        treeTable.expand(driverNode);
      }
    } catch(final RuntimeException | Error e) {
      log.error(e.getMessage(), e);
    }
    TopologyUtils.addDriver(model, driver);
  }

  /**
   * Remove the specified driver from the treeTable.
   * @param driverData the driver to add.
   */
  private synchronized void removeDriver(final TopologyDriver driverData) {
    TopologyUtils.removeDriver(model, driverData);
  }

  /**
   * Add the specified node to the specified driver in the treeTable.
   * @param driverData the driver to add to.
   * @param nodeData the node to add.
   */
  private synchronized void addNode(final TopologyDriver driverData, final TopologyNode nodeData) {
    final DefaultMutableTreeNode nodeNode = TopologyUtils.addNode(model, driverData, nodeData);
    if (nodeNode != null) {
      final DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) nodeNode.getParent();
      //if ((driverNode.getChildCount() == 1) && !treeTable.isCollapsed(driverNode)) treeTable.expand(driverNode);
      if (driverNode.getChildCount() == 1) treeTable.expand(driverNode);
    }
  }

  /**
   * Remove the specified node from the specified driver in the treeTable.
   * @param driverData the driver to add to.
   * @param nodeData the node to add.
   */
  private synchronized void removeNode(final TopologyDriver driverData, final TopologyNode nodeData) {
    if ((driverData == null) || (nodeData == null)) return;
    if (debugEnabled) log.debug("attempting to remove node=" + nodeData + " from driver=" + driverData);
    final DefaultMutableTreeNode driver = TreeTableUtils.findComponent(treeTableRoot, driverData.getUuid());
    if (driver == null) return;
    final String nodeUuid = nodeData.getUuid();
    final DefaultMutableTreeNode node = TreeTableUtils.findComponent(driver, nodeUuid);
    if (node != null) {
      if (debugEnabled) log.debug("removing node: " + nodeData);
      model.removeNodeFromParent(node);
    }
  }

  @Override
  public void driverAdded(final TopologyEvent event) {
    if (isAutoRefresh()) addDriver(event.getDriver());
  }

  @Override
  public void driverRemoved(final TopologyEvent event) {
    if (isAutoRefresh()) removeDriver(event.getDriver());
  }

  @Override
  public void driverUpdated(final TopologyEvent event) {
  }

  @Override
  public void nodeAdded(final TopologyEvent event) {
    if (isAutoRefresh()) addNode(event.getDriver(), event.getNodeOrPeer());
  }

  @Override
  public void nodeRemoved(final TopologyEvent event) {
    if (isAutoRefresh()) removeNode(event.getDriver(), event.getNodeOrPeer());
  }

  @Override
  public synchronized void nodeUpdated(final TopologyEvent event) {
    if (!isAutoRefresh()) return;
    if (event.getUpdateType() == TopologyEvent.UpdateType.NODE_STATE) {
      final TopologyDriver driverData = event.getDriver();
      final DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, driverData.getUuid());
      if (driverNode == null) return;
      final TopologyNode nodeData = event.getNodeOrPeer();
      if (nodeData == null) return;
      final DefaultMutableTreeNode node = TreeTableUtils.findComponent(driverNode, nodeData.getUuid());
      if (node != null) model.changeNode(node);
    }
  }

  /**
   * Determine whether auto-refresh is on or off.
   * @return {@code true} if auto refresh is {@code on}, false otherwise.
   */
  public synchronized boolean isAutoRefresh() {
    return autoRefresh;
  }

  /**
   * Specify whether auto-refresh is on or off.
   * @param autoRefresh {@code true} to turn auto-refresh on, {@code false} otherwise.
   */
  public synchronized void setAutoRefresh(final boolean autoRefresh) {
    this.autoRefresh = autoRefresh;
  }
}
