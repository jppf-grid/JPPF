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

package org.jppf.admin.web.health;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.admin.web.*;
import org.jppf.admin.web.health.threaddump.ThreadDumpAction;
import org.jppf.admin.web.settings.UserSettings;
import org.jppf.admin.web.tabletree.*;
import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.monitoring.diagnostics.JVMHealthTreeTableModel;
import org.jppf.ui.treetable.*;
import org.jppf.ui.utils.TopologyUtils;
import org.jppf.utils.TypedProperties;

/**
 *
 * @author Laurent Cohen
 */
public class HealthTreeData extends TableTreeData {
  /**
   * Listens to topology events.
   */
  private TopologyListener listener;
  /**
   * Alert thresholds for memory and cpu values.
   */
  private final AlertThresholds memoryThresholds = new AlertThresholds(60d, 80d), cpuThresholds = new AlertThresholds(80d, 90d);

  /**
   *
   */
  public HealthTreeData() {
    super(TreeViewType.HEALTH);
    getSelectionHandler().setFilter(new TreeNodeFilter() {
      @Override
      public boolean accepts(final DefaultMutableTreeNode node) {
        return (node != null) && !((AbstractTopologyComponent) node.getUserObject()).isPeer();
      }
    });
    listener = new HealthTreeListener(model, getSelectionHandler(), JPPFWebSession.get().getNodeFilter());
    JPPFWebConsoleApplication.get().getTopologyManager().addTopologyListener(listener);
    ActionHandler ah = getActionHandler();
    ah.addAction(HealthConstants.GC_ACTION, new GCLink.Action());
    ah.addAction(HealthConstants.THREAD_DUMP_ACTION, new ThreadDumpAction());
    ah.addAction(HealthConstants.HEAP_DUMP_ACTION, new HeapDumpLink.Action());
  }

  /**
   * @return the topology listener.
   */
  public TopologyListener getListener() {
    return listener;
  }

  /**
   * Set the topology listener.
   * @param listener the listener to set.
   */
  public void setListener(final TopologyListener listener) {
    this.listener = listener;
  }

  /**
   * @return the alert thresholds for memory values.
   */
  public AlertThresholds getMemoryThresholds() {
    return memoryThresholds;
  }

  /**
   * @return the alert thresholds for cpu values.
   */
  public AlertThresholds getCpuThresholds() {
    return cpuThresholds;
  }

  /**
   * Initialize the thresholds from the specified settings.
   * @param props the settings to use.
   */
  public void initThresholds(final TypedProperties props) {
    memoryThresholds.setWarning(props.getDouble("thresholds.memory.warning.field", 60d));
    memoryThresholds.setCritical(props.getDouble("thresholds.memory.critical.field", 80d));
    cpuThresholds.setWarning(props.getDouble("thresholds.cpu.warning.field", 80d));
    cpuThresholds.setCritical(props.getDouble("thresholds.cpu.critical.field", 90d));
  }

  /**
   * Update the threshold values from the current user settings.
   */
  public void updateThresholds() {
    UserSettings settings = JPPFWebSession.get().getUserSettings();
    initThresholds(settings.getProperties());
  }

  @Override
  public void cleanup() {
    super.cleanup();
    if (listener != null) JPPFWebConsoleApplication.get().getTopologyManager().removeTopologyListener(listener);
  }

  @Override
  protected void createTreeTableModel() {
    model = new JVMHealthTreeTableModel(new DefaultMutableTreeNode("topology.tree.root"), JPPFWebSession.get().getLocale());
    // populate the tree table model
    for (TopologyDriver driver : JPPFWebConsoleApplication.get().getTopologyManager().getDrivers()) {
      TopologyUtils.addDriver(model, driver);
      for (AbstractTopologyComponent child : driver.getChildren()) {
        if (!child.isPeer()) TopologyUtils.addNode(model, driver, (TopologyNode) child);
      }
    }
  }
}
