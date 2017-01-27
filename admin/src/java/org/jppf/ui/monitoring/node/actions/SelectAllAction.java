/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.ui.monitoring.node.actions;

import java.awt.event.ActionEvent;
import java.util.*;

import javax.swing.tree.*;

import org.jppf.client.monitoring.topology.AbstractTopologyComponent;
import org.jppf.ui.treetable.*;

/**
 * Action performed to select all drivers in the topology view.
 * @author Laurent Cohen
 */
public class SelectAllAction extends AbstractSelectionAction
{
  /**
   * Initialize this action with the specified tree table panel.
   * @param panel the tree table panel to which this action applies.
   */
  public SelectAllAction(final AbstractTreeTableOption panel)
  {
    super(panel);
    setupIcon("/org/jppf/ui/resources/select_all.gif");
    setupNameAndTooltip("select.all");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void actionPerformed(final ActionEvent e) {
    synchronized(panel) {
      JPPFTreeTable treeTable = panel.getTreeTable();
      TreeTableModelAdapter model = (TreeTableModelAdapter) treeTable.getModel();
      List<TreePath> selectionPath = new ArrayList<>();
      for (DefaultMutableTreeNode driver: getDriverNodes()) {
        selectionPath.add(treeTable.getPathForNode(driver));
        for (int i=0; i<driver.getChildCount(); i++) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode) driver.getChildAt(i);
          AbstractTopologyComponent nodeData = (AbstractTopologyComponent) node.getUserObject();
          if ((nodeData == null) || !nodeData.isNode()) continue;
          selectionPath.add(treeTable.getPathForNode(node));
        }
      }
      model.setSelectedPaths(selectionPath.toArray(new TreePath[selectionPath.size()]));
    }
  }
}
