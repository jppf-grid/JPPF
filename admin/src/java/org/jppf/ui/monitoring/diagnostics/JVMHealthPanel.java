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

package org.jppf.ui.monitoring.diagnostics;

import java.util.*;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.event.*;
import org.jppf.ui.monitoring.node.actions.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.treetable.*;
import org.jppf.ui.utils.TreeTableUtils;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
 */
public class JVMHealthPanel extends AbstractTreeTableOption implements TopologyListener {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JVMHealthPanel.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Manages the topology updates.
   */
  private final TopologyManager manager;

  /**
   * Initialize this panel with the specified information.
   */
  public JVMHealthPanel() {
    BASE = "org.jppf.ui.i18n.JVMHealthPage";
    if (debugEnabled) log.debug("initializing JVMHealthPanel");
    manager = StatsHandler.getInstance().getTopologyManager();
    createTreeTableModel();
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private void createTreeTableModel() {
    treeTableRoot = new DefaultMutableTreeNode(localize("tree.root.name"));
    model = new JVMHealthTreeTableModel(treeTableRoot);
  }

  /**
   * Initialize the refresh of tree structure.
   */
  public void init() {
    populate();
    manager.addTopologyListener(this);
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  public void populate() {
    for (TopologyDriver driver: manager.getDrivers()) {
      driverAdded(new TopologyEvent(manager, driver, null, TopologyEvent.UpdateType.TOPOLOGY));
      for (AbstractTopologyComponent child: driver.getChildren()) {
        if (child.isPeer()) continue;
        TopologyNode node = (TopologyNode) child;
        log.debug("adding node " + node+ " to driver " + driver);
        nodeAdded(new TopologyEvent(manager, driver, node, TopologyEvent.UpdateType.TOPOLOGY));
      }
    }
    treeTable.expandAll();
    repaintTreeTable();
  }

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
    treeTable.getTree().setCellRenderer(new HealthTreeCellRenderer());
    treeTable.setDefaultRenderer(Object.class, new HealthTableCellRenderer(this));
    JScrollPane sp = new JScrollPane(treeTable);
    setUIComponent(sp);
    treeTable.expandAll();
    StatsHandler.getInstance().addShowIPListener(new ShowIPListener() {
      @Override
      public void stateChanged(final ShowIPEvent event) {
        treeTable.repaint();
      }
    });
  }

  /**
   * Repaint the tree table area.
   */
  void repaintTreeTable() {
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

  /**
   * Initialize all actions used in the panel.
   */
  public void setupActions() {
    actionHandler = new JTreeTableActionHandler(treeTable);
    actionHandler.putAction("health.gc", new GCAction());
    actionHandler.putAction("health.heap.dump", new HeapDumpAction());
    actionHandler.putAction("health.thread.dump", new ThreadDumpAction());
    actionHandler.putAction("health.select.drivers", new SelectDriversAction(this));
    actionHandler.putAction("health.select.nodes", new SelectNodesAction(this));
    actionHandler.putAction("health.select.all", new SelectAllAction(this));
    actionHandler.putAction("health.update.thresholds", new ThresholdSettingsAction(this));
    actionHandler.putAction("health.show.hide", new ShowHideColumnsAction(this));
    actionHandler.updateActions();
    treeTable.addMouseListener(new JVMHealthTreeTableMouseListener(actionHandler));
    new Thread(new ActionsInitializer(this, "/health.toolbar")).start();
  }

  @Override
  public synchronized void driverAdded(final TopologyEvent event) {
    if (debugEnabled) log.debug("adding driver " + event.getDriver());
    TopologyDriver driverData = event.getDriver();
    DefaultMutableTreeNode driver = TreeTableUtils.findDriver(treeTableRoot, driverData.getUuid());
    if (driver != null) return;
    int index = TreeTableUtils.driverInsertIndex(treeTableRoot, driverData);
    if (index < 0) return;
    driver = new DefaultMutableTreeNode(driverData);
    model.insertNodeInto(driver, treeTableRoot, index);
    if (index == 0) treeTable.expand(treeTableRoot);
  }

  @Override
  public synchronized void driverRemoved(final TopologyEvent event) {
    if (debugEnabled) log.debug("removing driver " + event.getDriver());
    DefaultMutableTreeNode driver = TreeTableUtils.findDriver(treeTableRoot, event.getDriver().getUuid());
    if (driver != null) model.removeNodeFromParent(driver);
  }

  @Override
  public synchronized void driverUpdated(final TopologyEvent event) {
    if (event.getUpdateType() == TopologyEvent.UpdateType.JVM_HEALTH) {
      DefaultMutableTreeNode driver = TreeTableUtils.findDriver(treeTableRoot, event.getDriver().getUuid());
      if (driver != null) model.changeNode(driver);
    }
  }

  @Override
  public synchronized void nodeAdded(final TopologyEvent event) {
    if (debugEnabled) log.debug("adding node " + event.getNodeOrPeer() + " to driver " + event.getDriver());
    if (event.getNodeOrPeer().isPeer()) return;
    DefaultMutableTreeNode driver = TreeTableUtils.findDriver(treeTableRoot, event.getDriver().getUuid());
    if (driver == null) return;
    DefaultMutableTreeNode node = TreeTableUtils.findNode(driver, event.getNodeOrPeer().getUuid());
    if (node != null) return;
    int index = TreeTableUtils.nodeInsertIndex(driver, event.getNodeOrPeer());
    if (index < 0) return;
    node = new DefaultMutableTreeNode(event.getNodeOrPeer());
    model.insertNodeInto(node, driver, index);
    if (index == 0) treeTable.expand(driver);
  }

  @Override
  public synchronized void nodeRemoved(final TopologyEvent event) {
    if (debugEnabled) log.debug("removing node " + event.getNodeOrPeer() + " from driver " + event.getDriver());
    if (event.getNodeOrPeer().isPeer()) return;
    DefaultMutableTreeNode driver = TreeTableUtils.findDriver(treeTableRoot, event.getDriver().getUuid());
    if (driver == null) return;
    DefaultMutableTreeNode node = TreeTableUtils.findNode(driver, event.getNodeOrPeer().getUuid());
    if (node != null) {
      model.removeNodeFromParent(node);
      repaintTreeTable();
    }
  }

  @Override
  public synchronized void nodeUpdated(final TopologyEvent event) {
    if (event.getUpdateType() == TopologyEvent.UpdateType.JVM_HEALTH) {
      DefaultMutableTreeNode driver = TreeTableUtils.findDriver(treeTableRoot, event.getDriver().getUuid());
      if (driver != null) {
        DefaultMutableTreeNode node = TreeTableUtils.findNode(driver, event.getNodeOrPeer().getUuid());
        if (node != null) model.changeNode(node);
      }
    }
  }

  /**
   * Save the threshold values to the preferences store.
   */
  public void saveThresholds() {
    Preferences pref = OptionsHandler.getPreferences().node("thresholds");
    for (Map.Entry<Thresholds.Name, Double> entry: getThresholds().getValues().entrySet()) pref.putDouble(entry.getKey().toString().toLowerCase(), entry.getValue());
  }

  /**
   * Load the threshold values from the preferences store.
   */
  public void loadThresholds() {
    Preferences pref = OptionsHandler.getPreferences().node("thresholds");
    Map<Thresholds.Name, Double> values = getThresholds().getValues();
    List<Thresholds.Name> list = new ArrayList<>(values.keySet());
    for (Thresholds.Name name: list) {
      Double value = pref.getDouble(name.toString().toLowerCase(), -1d);
      if (value <= 0d) continue;
      values.put(name, value);
    }
  }

  /**
   * Get the threshold values.
   * @return a {@link Thresholds} object.
   */
  public Thresholds getThresholds() {
    return StatsHandler.getInstance().getClientHandler().getThresholds();
  }

  /**
   * Hide the columns.
   */
  public void hideColumns() {
    hideColumn(3);
    hideColumn(4);
  }

  /**
   * Restore the hidden columns.
   */
  public void restoreColumns() {
    restoreColumn(3);
    restoreColumn(4);
  }
}
