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

import java.util.*;

import javax.swing.tree.*;

import org.jppf.ui.treetable.*;

/**
 * Abstract superclass for actions that select nodes in the tree table.
 * @author Laurent Cohen
 */
public abstract class AbstractSelectionAction extends AbstractTopologyAction {
  /**
   * Constant for an empty <code>TreePath</code>.
   */
  protected static final TreePath[] EMPTY_TREE_PATH = new TreePath[0];
  /**
   * The tree table panel to which this action applies.
   */
  protected final AbstractTreeTableOption panel;

  /**
   * Initialize this action with the specified tree table panel.
   * @param panel the tree table panel to which this action applies.
   */
  public AbstractSelectionAction(final AbstractTreeTableOption panel) {
    this.panel = panel;
    setEnabled(true);
  }

  /**
   * Get the list of all tree nodes representing a driver.
   * @return a list of {@link DefaultMutableTreeNode} instances.
   */
  protected List<DefaultMutableTreeNode> getDriverNodes() {
    final List<DefaultMutableTreeNode> list = new ArrayList<>();
    final JPPFTreeTable treeTable = panel.getTreeTable();
    final TreeTableModelAdapter model = (TreeTableModelAdapter) treeTable.getModel();
    final DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getTreeTableModel().getRoot();
    for (int i = 0; i < root.getChildCount(); i++) {
      list.add((DefaultMutableTreeNode) root.getChildAt(i));
    }
    return list;
  }
}
