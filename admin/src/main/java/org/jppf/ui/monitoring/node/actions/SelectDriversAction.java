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

package org.jppf.ui.monitoring.node.actions;

import java.awt.event.ActionEvent;
import java.util.*;

import javax.swing.tree.*;

import org.jppf.ui.treetable.*;

/**
 * Action performed to select all drivers in the topology view.
 * @author Laurent Cohen
 */
public class SelectDriversAction extends AbstractSelectionAction {
  /**
   * Initialize this action with the specified tree table panel.
   * @param panel the tree table panel to which this action applies.
   */
  public SelectDriversAction(final AbstractTreeTableOption panel) {
    super(panel);
    setupIcon("/org/jppf/ui/resources/select_drivers.gif");
    setupNameAndTooltip("select.drivers");
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    synchronized (panel) {
      final JPPFTreeTable treeTable = panel.getTreeTable();
      final TreeTableModelAdapter model = (TreeTableModelAdapter) treeTable.getModel();
      final List<TreePath> selectionPath = new ArrayList<>();
      for (final DefaultMutableTreeNode driverNode: getDriverNodes()) selectionPath.add(treeTable.getPathForNode(driverNode));
      model.setSelectedPaths(selectionPath.toArray(new TreePath[selectionPath.size()]));
    }
  }
}
