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

import org.jppf.admin.web.filter.*;
import org.jppf.admin.web.tabletree.*;
import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.treetable.AbstractJPPFTreeTableModel;
import org.jppf.ui.utils.TopologyUtils;

/**
 * Listens to topology events so as to update the JVM health view.
 * @author Laurent Cohen
 */
public class HealthTreeListener extends AbstractMonitoringListener implements TopologyListener {
  /**
   * Initialize with the specified tree model and selection handler.
   * @param treeModel the tree table model.
   * @param selectionHandler handles the selection of rows in the tree table.
   * @param nodeFilter the node filter to use.
   */
  public HealthTreeListener(final AbstractJPPFTreeTableModel treeModel, final SelectionHandler selectionHandler, final TopologyFilter nodeFilter) {
    super(treeModel, selectionHandler, nodeFilter);
  }

  @Override
  public void driverAdded(final TopologyEvent event) {
    TopologyUtils.addDriver(treeModel, event.getDriver());
  }

  @Override
  public void driverRemoved(final TopologyEvent event) {
    TopologyDriver driver = event.getDriver();
    selectionHandler.unselect(driver.getUuid());
    for (TopologyNode node: driver.getNodesAndPeers()) selectionHandler.unselect(node.getUuid());
    TopologyUtils.removeDriver(treeModel, driver);
  }

  @Override
  public void driverUpdated(final TopologyEvent event) {
  }

  @Override
  public void nodeAdded(final TopologyEvent event) {
    if (!isAccepted(event.getNodeOrPeer())) return;
    TopologyNode nodeComp = event.getNodeOrPeer();
    if (!nodeComp.isPeer()) {
      addNode(event.getDriver(), nodeComp);
      /*
      DefaultMutableTreeNode node = TopologyUtils.addNode(treeModel, event.getDriver(), nodeComp);
      if ((node != null) && (getTableTree() != null)) {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        if (parent.getChildCount() == 1) getTableTree().expand(parent);
      }
      */
    }
  }

  @Override
  public void nodeRemoved(final TopologyEvent event) {
    TopologyNode nodeComp = event.getNodeOrPeer();
    if (!nodeComp.isPeer()) {
      TopologyUtils.removeNode(treeModel, event.getDriver(), nodeComp);
      selectionHandler.unselect(event.getNodeOrPeer().getUuid());
    }
  }

  @Override
  public synchronized void nodeUpdated(final TopologyEvent event) {
    if (!isAccepted(event.getNodeOrPeer())) return;
    TopologyNode nodeComp = event.getNodeOrPeer();
    if (!nodeComp.isPeer()) {
      if (event.getUpdateType() == TopologyEvent.UpdateType.NODE_STATE) {
        TopologyUtils.updateNode(treeModel, event.getDriver(), nodeComp);
      }
    }
  }

  @Override
  public void onFilterChange(final TopologyFilterEvent event) {
    updateTopology(event);
  }
}
