/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.io.IOException;
import java.util.*;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.management.NodeSelector;
import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.node.*;
import org.jppf.ui.monitoring.node.actions.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.treetable.*;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
 */
public class JVMHealthPanel extends AbstractTreeTableOption implements TopologyChangeListener
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JVMHealthPanel.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The main topology tree view.
   */
  protected NodeDataPanel nodePanel = null;
  /**
   * 
   */
  protected RefreshHandler refreshHandler = null;
  /**
   * The threshold values.
   */
  protected Thresholds thresholds = new Thresholds();

  /**
   * Initialize this panel with the specified information.
   */
  public JVMHealthPanel()
  {
    BASE = "org.jppf.ui.i18n.JVMHealthPage";
    if (debugEnabled) log.debug("initializing JVMHealthPanel");
    createTreeTableModel();
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private void createTreeTableModel()
  {
    treeTableRoot = new DefaultMutableTreeNode(localize("tree.root.name"));
    model = new JVMHealthTreeTableModel(treeTableRoot);
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  public void populate()
  {
    DefaultMutableTreeNode mainRoot = nodePanel.getTreeTableRoot();
    for (int i=0; i<mainRoot.getChildCount(); i++)
    {
      DefaultMutableTreeNode driver = (DefaultMutableTreeNode) mainRoot.getChildAt(i);
      TopologyData driverData = (TopologyData) driver.getUserObject();
      DefaultMutableTreeNode newDriver = new DefaultMutableTreeNode(driverData);
      treeTableRoot.add(newDriver);
      for (int j=0; j<driver.getChildCount(); j++)
      {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) driver.getChildAt(j);
        TopologyData nodeData = (TopologyData) node.getUserObject();
        if (nodeData.getType() == TopologyDataType.PEER) continue;
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(nodeData);
        newDriver.add(newNode);
      }
    }
    treeTable.expandAll();
    repaintTreeTable();
  }

  @Override
  public void createUI()
  {
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
  }

  /**
   * Repaint the tree table area.
   */
  void repaintTreeTable()
  {
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
   * Refresh the JVM health status of all displayed drivers and nodes.
   */
  public synchronized void refreshSnapshots() {
    for (int i=0; i<treeTableRoot.getChildCount(); i++) {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
      TopologyData driverData = (TopologyData) driverNode.getUserObject();
      if (driverData.getDiagnostics() == null) continue;
      try {
        HealthSnapshot health = driverData.getDiagnostics().healthSnapshot();
        if (log.isTraceEnabled()) log.trace("got driver health snapshot: " + health);
        driverData.refreshHealthSnapshot(health);
      } catch (IOException e) {
        log.error("error getting health snapshot for driver " + driverData.getUuid() + ", reinitializing the connection", e);
        driverData.initializeProxies();
      } catch (Exception e) {
        log.error("error getting health snapshot for driver " + driverData.getUuid(), e);
      }
      if ((driverNode.getChildCount() <= 0) || (driverData.getNodeForwarder() == null)) continue;
      Map<String, TopologyData> uuidMap = new HashMap<String, TopologyData>();
      for (int j=0; j<driverNode.getChildCount(); j++) {
        DefaultMutableTreeNode nodeNode = (DefaultMutableTreeNode) driverNode.getChildAt(j);
        TopologyData data = (TopologyData) nodeNode.getUserObject();
        uuidMap.put(data.getUuid(), data);
      }
      Map<String, Object> result = null;
      try {
        result = driverData.getNodeForwarder().healthSnapshot(new NodeSelector.UuidSelector(new HashSet<String>(uuidMap.keySet())));
      } catch(IOException e) {
        log.error("error getting node health for driver " + driverData.getUuid() + ", reinitializing the connection", e);
        driverData.initializeProxies();
      } catch(Exception e) {
        log.error("error getting node health for driver " + driverData.getUuid(), e);
      }
      if (result == null) continue;
      for (Map.Entry<String, Object> entry: result.entrySet()) {
        TopologyData data = uuidMap.get(entry.getKey());
        if (data == null) continue;
        if (entry.getValue() instanceof Exception) {
          data.setStatus(TopologyDataStatus.DOWN);
          log.warn("exception raised for node " + entry.getKey() + " : " + ExceptionUtils.getMessage((Exception) entry.getValue()));
        }
        else if (entry.getValue() instanceof HealthSnapshot)
        {
          data.refreshHealthSnapshot((HealthSnapshot) entry.getValue());
          if (log.isTraceEnabled()) log.trace("got node health snapshot: " + entry.getValue());
        }
      }
    }
    repaintTreeTable();
  }

  /**
   * Initialize all actions used in the panel.
   */
  public void setupActions()
  {
    actionHandler = new JTreeTableActionHandler(treeTable);
    actionHandler.putAction("health.gc", new GCAction());
    actionHandler.putAction("health.heap.dump", new HeapDumpAction());
    actionHandler.putAction("health.thread.dump", new ThreadDumpAction());
    actionHandler.putAction("health.select.drivers", new SelectDriversAction(this));
    actionHandler.putAction("health.select.nodes", new SelectNodesAction(this));
    actionHandler.putAction("health.select.all", new SelectAllAction(this));
    actionHandler.putAction("health.update.thresholds", new ThresholdSettingsAction(this));
    actionHandler.updateActions();
    treeTable.addMouseListener(new JVMHealthTreeTableMouseListener(actionHandler));
    //
    Runnable r = new ActionsInitializer(this, "/health.toolbar");
    new Thread(r).start();
  }

  /**
   * Get the main topology tree view.
   * @return a {@link NodeDataPanel} instance.
   */
  public NodeDataPanel getNodePanel()
  {
    return nodePanel;
  }

  /**
   * Set the main topology tree view.
   * @param nodePanel a {@link NodeDataPanel} instance.
   */
  public void setNodePanel(final NodeDataPanel nodePanel)
  {
    this.nodePanel = nodePanel;
  }

  @Override
  public synchronized void driverAdded(final TopologyChangeEvent event)
  {
    if (debugEnabled) log.debug("adding driver " + event.getDriverData());
    TopologyData driverData = event.getDriverData();
    DefaultMutableTreeNode driver = findDriver(driverData.getUuid());
    if (driver != null) return;
    driver = new DefaultMutableTreeNode(driverData);
    model.insertNodeInto(driver, treeTableRoot, treeTableRoot.getChildCount());
  }

  @Override
  public synchronized void driverRemoved(final TopologyChangeEvent event)
  {
    if (debugEnabled) log.debug("removing driver " + event.getDriverData());
    DefaultMutableTreeNode driver = findDriver(event.getDriverData().getUuid());
    if (driver != null) model.removeNodeFromParent(driver);
  }

  @Override
  public synchronized void nodeAdded(final TopologyChangeEvent event)
  {
    if (debugEnabled) log.debug("adding node " + event.getNodeData() + " to driver " + event.getDriverData());
    if ((event.getPeerData() != null) || (event.getNodeData().getType() == TopologyDataType.PEER)) return;
    DefaultMutableTreeNode driver = findDriver(event.getDriverData().getUuid());
    if (driver == null) return;
    DefaultMutableTreeNode node = findNode(driver, event.getNodeData().getUuid());
    if (node != null) return;
    node = new DefaultMutableTreeNode(event.getNodeData());
    model.insertNodeInto(node, driver, driver.getChildCount());
  }

  @Override
  public synchronized void nodeRemoved(final TopologyChangeEvent event)
  {
    if (debugEnabled) log.debug("removing node " + event.getNodeData() + " from driver " + event.getDriverData());
    if (event.getNodeData().getType() == TopologyDataType.PEER) return;
    DefaultMutableTreeNode driver = findDriver(event.getDriverData().getUuid());
    if (driver == null) return;
    DefaultMutableTreeNode node = findNode(driver, event.getNodeData().getUuid());
    if (node != null)
    {
      model.removeNodeFromParent(node);
      repaintTreeTable();
    }
  }

  @Override
  public synchronized void dataUpdated(final TopologyChangeEvent event)
  {
  }

  /**
   * Find the driver tree node with the specified driver name.
   * @param driverUuid name of the driver to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  DefaultMutableTreeNode findDriver(final String driverUuid) {
    for (int i=0; i<treeTableRoot.getChildCount(); i++) {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
      TopologyData data = (TopologyData) driverNode.getUserObject();
      if (data.getUuid().equals(driverUuid)) return driverNode;
    }
    return null;
  }

  /**
   * Find the node tree node with the specified driver name and node information.
   * @param driver name the parent of the node to find.
   * @param nodeUuid the name of the node to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  DefaultMutableTreeNode findNode(final DefaultMutableTreeNode driver, final String nodeUuid) {
    for (int i=0; i<driver.getChildCount(); i++) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) driver.getChildAt(i);
      TopologyData nodeData = (TopologyData) node.getUserObject();
      if (nodeUuid.equals(nodeData.getUuid())) return node;
    }
    return null;
  }

  /**
   * 
   */
  public void initRefreshHandler()
  {
    refreshHandler = new RefreshHandler(this);
  }

  /**
   * Save the threshold values to the preferences store.
   */
  public void saveThresholds()
  {
    Preferences pref = OptionsHandler.getPreferences().node("thresholds");
    for (Map.Entry<Thresholds.Name, Double> entry: thresholds.getValues().entrySet()) pref.putDouble(entry.getKey().toString().toLowerCase(), entry.getValue());
  }

  /**
   * Load the threshold values from the preferences store.
   */
  public void loadThresholds()
  {
    Preferences pref = OptionsHandler.getPreferences().node("thresholds");
    Map<Thresholds.Name, Double> values = thresholds.getValues();
    List<Thresholds.Name> list = new ArrayList<Thresholds.Name>(values.keySet());
    for (Thresholds.Name name: list)
    {
      Double value = pref.getDouble(name.toString().toLowerCase(), -1d);
      if (value <= 0d) continue;
      values.put(name, value);
    }
  }
}
