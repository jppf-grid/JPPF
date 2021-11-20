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

package org.jppf.ui.treetable;

import javax.swing.table.TableColumn;
import javax.swing.tree.*;

import org.slf4j.*;

/**
 * Common super class for all tree tables in the admin console.
 * @author Laurent Cohen
 */
public class JPPFTreeTable extends JTreeTable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFTreeTable.class);
  /**
   * Initialize this tree table with the specified model.
   * @param treeTableModel - a tree table model.
   */
  public JPPFTreeTable(final AbstractJPPFTreeTableModel treeTableModel) {
    super(treeTableModel);
    for (int i = 0; i < treeTableModel.getColumnCount(); i++) {
      final TableColumn column = getColumnModel().getColumn(i);
      column.setIdentifier(Integer.valueOf(i));
    }
  }

  /**
   * Get a tree path corresponding to the node at row n in the tree.
   * @param n the row index for which to get the path.
   * @return a <code>TreePath</code> instance.
   */
  public TreePath getPathForRow(final int n) {
    if (getTree().getRowCount() <= n) return null;
    try {
      return getTree().getPathForRow(n);
    } catch (final Exception e) {
      log.debug(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Get a tree path corresponding to the node at row n in the tree.
   * @param node the node for which to get the path.
   * @return a <code>TreePath</code> instance.
   */
  public TreePath getPathForNode(final DefaultMutableTreeNode node) {
    return new TreePath(node.getPath());
  }

  /**
   * Expand all paths in the tree.
   */
  public void expandAll() {
    final DefaultMutableTreeNode root = (DefaultMutableTreeNode) getTree().getModel().getRoot();
    expand(root, true);
  }

  /**
   * Expands the leaves of the specified node.
   * @param node - the node to expand.
   */
  public void expand(final DefaultMutableTreeNode node) {
    expand(node, false);
  }

  /**
   * Expands the leaves of the specified node.
   * @param node the node to expand.
   * @param recursive specifies whether expansion should recurse down to the leaf nodes.
   */
  public void expand(final DefaultMutableTreeNode node, final boolean recursive) {
    getTree().expandPath(getPathForNode(node));
    if (recursive && (node.getChildCount() > 0)) {
      for (int i = 0; i < node.getChildCount(); i++) expand((DefaultMutableTreeNode) node.getChildAt(i), recursive);
    }
  }

  /**
   * Collapse all paths in the tree.
   */
  public void collapseAll() {
    final DefaultMutableTreeNode root = (DefaultMutableTreeNode) getTree().getModel().getRoot();
    for (int i = 0; i < root.getChildCount(); i++) getTree().collapsePath(getPathForNode((DefaultMutableTreeNode) root.getChildAt(i)));
  }

  /**
   * Determine whether the specified node is collapsed.
   * @param node the tree node to check.
   * @return {@code true} if the node is collapsed, {@code false} otherwise.
   */
  public boolean isCollapsed(final DefaultMutableTreeNode node) {
    return getTree().isCollapsed(new TreePath(node.getPath()));
  }

  /**
   * Determine whether the specified node is expanded.
   * @param node the tree node to check.
   * @return {@code true} if the node is expanded, {@code false} otherwise.
   */
  public boolean isExpanded(final DefaultMutableTreeNode node) {
    return getTree().isExpanded(new TreePath(node.getPath()));
  }
}
