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

package org.jppf.admin.web.topology;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.admin.web.*;
import org.jppf.admin.web.tabletree.*;
import org.jppf.admin.web.topology.loadbalancing.LoadBalancingLink;
import org.jppf.admin.web.topology.nodeconfig.NodeConfigAction;
import org.jppf.admin.web.topology.nodethreads.NodeThreadsAction;
import org.jppf.admin.web.topology.provisioning.ProvisioningAction;
import org.jppf.admin.web.topology.serverstop.DriverStopRestartAction;
import org.jppf.admin.web.topology.systeminfo.SystemInfoAction;
import org.jppf.admin.web.utils.UpdatableAction;
import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.monitoring.node.NodeTreeTableModel;
import org.jppf.ui.treetable.*;
import org.jppf.ui.utils.TopologyUtils;
import org.jppf.utils.collections.*;

/**
 *
 * @author Laurent Cohen
 */
public class TopologyTreeData extends TableTreeData {
  /**
   * Listens to topology events.
   */
  private TopologyListener listener;

  /**
   *
   */
  public TopologyTreeData() {
    super(TreeViewType.TOPOLOGY);
    getSelectionHandler().setFilter(new TreeNodeFilter() {
      @Override
      public boolean accepts(final DefaultMutableTreeNode node) {
        return (node != null) && !((AbstractTopologyComponent) node.getUserObject()).isPeer();
      }
    });
    listener = new TopologyTreeListener(model, getSelectionHandler(), JPPFWebSession.get().getNodeFilter());
    JPPFWebConsoleApplication.get().getTopologyManager().addTopologyListener(listener);
    ActionHandler ah = getActionHandler();
    ah.addAction(TopologyConstants.SYSTEM_INFO_ACTION, new SystemInfoAction());
    ah.addAction(TopologyConstants.PROVISIONING_ACTION, new ProvisioningAction());
    ah.addAction(TopologyConstants.SERVER_STOP_RESTART_ACTION, new DriverStopRestartAction());
    ah.addAction(TopologyConstants.SERVER_RESET_STATS_ACTION, new ServerResetStatsLink.Action());
    ah.addAction(TopologyConstants.SERVER_LOAD_BALANCING_ACTION, new LoadBalancingLink.Action());
    ah.addAction(TopologyConstants.NODE_CONFIG_ACTION, new NodeConfigAction());
    ah.addAction(TopologyConstants.NODE_THREADS_ACTION, new NodeThreadsAction());
    ah.addAction(TopologyConstants.CANCEL_PENDING_ACTION, new CancelPendingActionLink.Action());
    ah.addAction(TopologyConstants.NODE_RESET_TASKS_ACTION, new ResetTaskCounterLink.Action());
    UpdatableAction action = new StopRestartNodeLink.Action();
    ah.addAction(TopologyConstants.NODE_STOP_ACTION, action);
    ah.addAction(TopologyConstants.NODE_RESTART_ACTION, action);
    ah.addAction(TopologyConstants.NODE_STOP_DEFERRED_ACTION, action);
    ah.addAction(TopologyConstants.NODE_RESTART_DEFERRED_ACTION, action);
    ah.addAction(TopologyConstants.NODE_SUSPEND_ACTION, new SuspendNodeLink.Action());
  }

  /**
   * Extract the nodes, if any, from the list of selected elements.
   * @param selected a list of all the selected elements.
   * @return a list of the selected nodes.
   */
  public static List<TopologyNode> getSelectedNodes(final List<DefaultMutableTreeNode> selected) {
    List<TopologyNode> result = new ArrayList<>();
    for (DefaultMutableTreeNode treeNode: selected) {
      AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      if (comp.isNode()) result.add((TopologyNode) comp);
    }
    return result;
  }

  /**
   * Extract the drivers, if any, from the list of selected elements.
   * @param selected a list of all the selected elements.
   * @return a list of the selected drivers.
   */
  public static List<TopologyDriver> getSelectedDrivers(final List<DefaultMutableTreeNode> selected) {
    List<TopologyDriver> result = new ArrayList<>();
    for (DefaultMutableTreeNode treeNode: selected) {
      AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      if (comp.isDriver()) result.add((TopologyDriver) comp);
    }
    return result;
  }

  /**
   * Get a mapping of driver to nodes uuids from the selected elements.
   * @param selected the selected elements.
   * @return mapping of driver to nodes uuids.
   */
  public static CollectionMap<TopologyDriver, String> getNodesMultimap(final List<DefaultMutableTreeNode> selected) {
    CollectionMap<TopologyDriver, String> map = new SetHashMap<>();
    for (DefaultMutableTreeNode treeNode: selected) {
      AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      if (comp.isNode() && (comp.getParent() != null)) map.putValue((TopologyDriver) comp.getParent(), comp.getUuid());
    }
    return map;
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

  @Override
  public void cleanup() {
    super.cleanup();
    if (listener != null) JPPFWebConsoleApplication.get().getTopologyManager().removeTopologyListener(listener);
  }

  @Override
  protected void createTreeTableModel() {
    model = new NodeTreeTableModel(new DefaultMutableTreeNode("topology.tree.root"), JPPFWebSession.get().getLocale());
    // populate the tree table model
    for (TopologyDriver driver : JPPFWebConsoleApplication.get().getTopologyManager().getDrivers()) {
      TopologyUtils.addDriver(model, driver);
      for (AbstractTopologyComponent child : driver.getChildren()) {
        TopologyUtils.addNode(model, driver, (TopologyNode) child);
      }
    }
  }
}
