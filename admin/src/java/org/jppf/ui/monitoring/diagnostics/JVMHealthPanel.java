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

package org.jppf.ui.monitoring.diagnostics;

import java.util.*;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.node.actions.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.treetable.*;
import org.jppf.ui.utils.*;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.concurrent.ThreadUtils;
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
    BASE = "org.jppf.ui.i18n.NodeDataPage";
    if (debugEnabled) log.debug("initializing JVMHealthPanel");
    manager = StatsHandler.getInstance().getTopologyManager();
    try {
      createTreeTableModel();
      populate();
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    manager.addTopologyListener(this);
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private void createTreeTableModel() {
    treeTableRoot = new DefaultMutableTreeNode(localize("tree.root.name"));
    model = new JVMHealthTreeTableModel(treeTableRoot, Locale.getDefault());
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  public void populate() {
    for (TopologyDriver driver: manager.getDrivers()) {
      if (debugEnabled) log.debug("adding driver {}", driver);
      driverAdded(new TopologyEvent(manager, driver, null, TopologyEvent.UpdateType.TOPOLOGY));
      for (final AbstractTopologyComponent child: driver.getChildren()) {
        if (child.isPeer()) continue;
        final TopologyNode node = (TopologyNode) child;
        if (debugEnabled) log.debug("adding node {} to driver {}", node, driver);
        nodeAdded(new TopologyEvent(manager, driver, node, TopologyEvent.UpdateType.TOPOLOGY));
      }
    }
    if (treeTable != null) treeTable.expandAll();
    repaintTreeTable();
  }

  @Override
  public void createUI() {
    try {
      treeTable = new JPPFTreeTable(model);
      if (debugEnabled) log.debug("tree table created");
      treeTable.getTree().setLargeModel(true);
      treeTable.getTree().setRootVisible(false);
      treeTable.getTree().setShowsRootHandles(true);
      treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
      treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
      treeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      treeTable.getTree().setCellRenderer(new HealthTreeCellRenderer());
      treeTable.setDefaultRenderer(Object.class, new HealthTableCellRenderer(this));
      final JScrollPane sp = new JScrollPane(treeTable);
      GuiUtils.adjustScrollbarsThickness(sp);
      setUIComponent(sp);
      treeTable.doLayout();
      treeTable.expandAll();
      StatsHandler.getInstance().getShowIPHandler().addShowIPListener((event) -> treeTable.repaint());
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Repaint the tree table area.
   */
  void repaintTreeTable() {
    SwingUtilities.invokeLater(() -> {
      if (treeTable != null) {
        treeTable.invalidate();
        treeTable.repaint();
      }
    });
  }

  /**
   * Initialize all actions used in the panel.
   */
  public void setupActions() {
    try {
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
      ThreadUtils.startThread(new ActionsInitializer(this, "/health.toolbar"), "/health.toolbar");
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public synchronized void driverAdded(final TopologyEvent event) {
    if (debugEnabled) log.debug("adding {}", event.getDriver());
    final TopologyDriver driverData = event.getDriver();
    final int index = TreeTableUtils.insertIndex(treeTableRoot, driverData);
    if (index < 0) return;
    if (debugEnabled) log.debug("adding at index {}", index);
    final DefaultMutableTreeNode driver = new DefaultMutableTreeNode(driverData);
    model.insertNodeInto(driver, treeTableRoot, index);
    if (index == 0) treeTable.expand(treeTableRoot, true);
  }

  @Override
  public synchronized void driverRemoved(final TopologyEvent event) {
    if (debugEnabled) log.debug("removing {}", event.getDriver());
    final DefaultMutableTreeNode driver = TreeTableUtils.findComponent(treeTableRoot, event.getDriver().getUuid());
    if (driver != null) model.removeNodeFromParent(driver);
  }

  @Override
  public synchronized void driverUpdated(final TopologyEvent event) {
    if (event.getUpdateType() == TopologyEvent.UpdateType.JVM_HEALTH) {
      final DefaultMutableTreeNode driver = TreeTableUtils.findComponent(treeTableRoot, event.getDriver().getUuid());
      if (driver != null) model.changeNode(driver);
    }
  }

  @Override
  public synchronized void nodeAdded(final TopologyEvent event) {
    if (debugEnabled) log.debug("adding {} to {}", event.getNodeOrPeer(), event.getDriver());
    if (event.getNodeOrPeer().isPeer()) return;
    final DefaultMutableTreeNode driver = TreeTableUtils.findComponent(treeTableRoot, event.getDriver().getUuid());
    if (driver == null) return;
    final int index = TreeTableUtils.insertIndex(driver, event.getNodeOrPeer());
    if (index < 0) return;
    if (debugEnabled) log.debug("adding at index {}", index);
    final DefaultMutableTreeNode node = new DefaultMutableTreeNode(event.getNodeOrPeer());
    model.insertNodeInto(node, driver, index);
    if (index == 0) treeTable.expand(driver, true);
  }

  @Override
  public synchronized void nodeRemoved(final TopologyEvent event) {
    if (debugEnabled) log.debug("removing {} from {}", event.getNodeOrPeer(), event.getDriver());
    if (event.getNodeOrPeer().isPeer()) return;
    final DefaultMutableTreeNode driver = TreeTableUtils.findComponent(treeTableRoot, event.getDriver().getUuid());
    if (driver == null) return;
    final DefaultMutableTreeNode node = TreeTableUtils.findComponent(driver, event.getNodeOrPeer().getUuid());
    if (node != null) model.removeNodeFromParent(node);
  }

  @Override
  public synchronized void nodeUpdated(final TopologyEvent event) {
    if (event.getUpdateType() == TopologyEvent.UpdateType.JVM_HEALTH) {
      final DefaultMutableTreeNode driver = TreeTableUtils.findComponent(treeTableRoot, event.getDriver().getUuid());
      if (driver != null) {
        final DefaultMutableTreeNode node = TreeTableUtils.findComponent(driver, event.getNodeOrPeer().getUuid());
        if (node != null) {
          model.changeNode(node);
          repaintTreeTable();
        }
      }
    }
  }

  /**
   * Save the threshold values to the preferences store.
   */
  public void saveThresholds() {
    final Preferences pref = OptionsHandler.getPreferences().node("thresholds");
    getThresholds().getValues().forEach((name, value) -> pref.putDouble(name.toString().toLowerCase(), value));
  }

  /**
   * Load the threshold values from the preferences store.
   */
  public void loadThresholds() {
    try {
      final Preferences pref = OptionsHandler.getPreferences().node("thresholds");
      final Map<Thresholds.Name, Double> values = getThresholds().getValues();
      final List<Thresholds.Name> list = new ArrayList<>(values.keySet());
      for (final Thresholds.Name name: list) {
        final Double value = pref.getDouble(name.toString().toLowerCase(), -1d);
        if (value <= 0d) continue;
        values.put(name, value);
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Get the threshold values.
   * @return a {@link Thresholds} object.
   */
  public Thresholds getThresholds() {
    return StatsHandler.getInstance().getClientHandler().getThresholds();
  }
}
