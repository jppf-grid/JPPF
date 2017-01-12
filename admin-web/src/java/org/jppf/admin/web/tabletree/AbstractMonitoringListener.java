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

package org.jppf.admin.web.tabletree;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.admin.web.JPPFWebConsoleApplication;
import org.jppf.admin.web.filter.*;
import org.jppf.client.monitoring.topology.*;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.ui.treetable.AbstractJPPFTreeTableModel;
import org.jppf.ui.utils.*;
import org.jppf.utils.collections.*;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class AbstractMonitoringListener implements TopologyFilterListener {
  /**
   * The tree table model.
   */
  protected final AbstractJPPFTreeTableModel treeModel;
  /**
   * Handles the selection of rows in the tree table.
   */
  protected final SelectionHandler selectionHandler;
  /**
   * The table tree to update.
   */
  protected JPPFTableTree tableTree;
  /**
   * The node filter to use.
   */
  protected TopologyFilter nodeFilter;

  /**
   * Initialize with the specified tree model and selection handler.
   * @param treeModel the tree table model.
   * @param selectionHandler handles the selection of rows in the tree table.
   * @param nodeFilter the node filter to use.
   */
  public AbstractMonitoringListener(final AbstractJPPFTreeTableModel treeModel, final SelectionHandler selectionHandler, final TopologyFilter nodeFilter) {
    this.treeModel = treeModel;
    this.selectionHandler = selectionHandler;
    this.nodeFilter = nodeFilter;
    this.nodeFilter.addListener(this);
  }

  /**
   * @return the table tree to update.
   */
  public synchronized JPPFTableTree getTableTree() {
    return tableTree;
  }

  /**
   * Set the table tree to update.
   * @param tableTree the table tree to set.
   */
  public synchronized void setTableTree(final JPPFTableTree tableTree) {
    this.tableTree = tableTree;
  }

  /**
   * Determine whether the specified node passes the ode filter.
   * @param node the node to evaluate.
   * @return {@code true} if the node passes the filter, {@code false} otherwise.
   */
  protected boolean isAccepted(final TopologyNode node) {
    if ((nodeFilter == null) || !nodeFilter.isActive()) return true;
    ExecutionPolicy policy = nodeFilter.getPolicy();
    if (policy == null) return true;
    JPPFSystemInformation info = node.getManagementInfo().getSystemInfo();
    if (info == null) return true;
    return policy.accepts(info);
  }

  /**
   * Update the topology represented by the specified model based on the specified filter event.
   * @param event the event encapsulating a change in the topology filter.
   */
  protected void updateTopology(final TopologyFilterEvent event) {
    TopologyManager mgr = JPPFWebConsoleApplication.get().getTopologyManager();
    List<TopologyDriver> allDrivers= mgr.getDrivers();
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
    CollectionMap<TopologyDriver, TopologyNode> toRemove = new ArrayListHashMap<>();
    CollectionMap<TopologyDriver, TopologyNode> toAdd = new ArrayListHashMap<>();
    for (TopologyDriver driver: allDrivers) {
      DefaultMutableTreeNode driverDmtn = TreeTableUtils.findComponent(root, driver.getUuid());
      if (driverDmtn == null) continue;
      for (TopologyNode node: driver.getNodes()) {
        boolean accepted = isAccepted(node);
        DefaultMutableTreeNode nodeDmtn = TreeTableUtils.findComponent(driverDmtn, node.getUuid());
        boolean present = nodeDmtn!= null;
        if (accepted && !present) toAdd.putValue(driver, node);
        else if (!accepted && present) toRemove.putValue(driver, node);
      }
    }
    for (Map.Entry<TopologyDriver, Collection<TopologyNode>> entry: toRemove.entrySet()) {
      for (TopologyNode node: entry.getValue()) {
        TopologyUtils.removeNode(treeModel, entry.getKey(), node);
        selectionHandler.unselect(node.getUuid());
      }
    }
    for (Map.Entry<TopologyDriver, Collection<TopologyNode>> entry: toAdd.entrySet()) {
      for (TopologyNode node: entry.getValue()) addNode(entry.getKey(), node);
    }
  }

  /**
   * Add a node to the specified driver in the tree model.
   * @param driver the driver to which the node is attached.
   * @param node the node to add.
   */
  protected void addNode(final TopologyDriver driver, final TopologyNode node) {
    DefaultMutableTreeNode nodeDmtn = TopologyUtils.addNode(treeModel, driver, node);
    if ((nodeDmtn != null) && (getTableTree() != null)) {
      DefaultMutableTreeNode parent = (DefaultMutableTreeNode) nodeDmtn.getParent();
      if (parent.getChildCount() == 1) getTableTree().expand(parent);
    }
  }

  /*
  protected void updateTopology(final TopologyFilterEvent event) {
    TopologyManager mgr = JPPFWebConsoleApplication.get().getTopologyManager();
    List<TopologyDriver> allDrivers= mgr.getDrivers();
    List<TopologyNode> allNodes= mgr.getNodes();
    
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
    for (int i=0; i<root.getChildCount(); i++) {
      DefaultMutableTreeNode driverDmtn = (DefaultMutableTreeNode) root.getChildAt(i);
      TopologyDriver driver = (TopologyDriver) driverDmtn.getUserObject();
      List<TopologyNode> toRemove = new ArrayList<>();
      for (int j=0; j<driverDmtn.getChildCount(); j++) {
        DefaultMutableTreeNode nodeDmtn = (DefaultMutableTreeNode) driverDmtn.getChildAt(j);
        TopologyNode node = (TopologyNode) nodeDmtn.getUserObject();
        if (!isAccepted(node)) {
          if (debugEnabled) log.debug("filtering out node {}", node);
          toRemove.add(node);
        } else if (debugEnabled) log.debug("keeping node {}", node);
      }
      for (TopologyNode node: toRemove) {
        TopologyUtils.removeNode(treeModel, driver, node);
        selectionHandler.unselect(node.getUuid());
      }
    }
  }
  */
}
