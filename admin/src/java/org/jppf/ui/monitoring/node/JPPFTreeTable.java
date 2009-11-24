/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.ui.monitoring.node;

import javax.swing.tree.*;

import org.jppf.ui.treetable.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFTreeTable extends JTreeTable
{
	/**
	 * Inityialize this tree table with the specified model.
	 * @param treeTableModel - a tree table model.
	 */
	public JPPFTreeTable(JPPFNodeTreeTableModel treeTableModel)
	{
		super(treeTableModel);
	}

	/**
	 * Get a tree path corresponding to the node at row n in the tree.
	 * @param n the row index for which to get the path.
	 * @return a <code>TreePath</code> instance.
	 */
	public TreePath getPathForRow(int n)
	{
		//JPPFNodeTreeTableModel model = (JPPFNodeTreeTableModel) getModel();
		Object o = getModel();
		TreeModel model = (TreeModel) getTree().getModel();
	  TreeNode[] path = ((DefaultMutableTreeNode) model.getRoot()).getPath();
		return new TreePath(path);
	}

	/**
	 * Get a tree path corresponding to the node at row n in the tree.
	 * @param node the node for which to get the path.
	 * @return a <code>TreePath</code> instance.
	 */
	public TreePath getPathForNode(DefaultMutableTreeNode node)
	{
		return new TreePath(node.getPath());
	}

	/**
	 * Expand all paths in the tree.
	 */
	public void expandAll()
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) getTree().getModel().getRoot();
		expand(root);
	}

	/**
	 * Expands the leaves of the specified node. 
	 * @param node - the node to expand.
	 */
	private void expand(DefaultMutableTreeNode node)
	{
		if (node.getChildCount() > 0)
		{
			getTree().expandPath(getPathForNode(node));
			for (int i=0; i<node.getChildCount(); i++) expand((DefaultMutableTreeNode) node.getChildAt(i));
		}
	}

	/**
	 * collapse all paths in the tree.
	 */
	public void collapseAll()
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) getTree().getModel().getRoot();
		for (int i=0; i<root.getChildCount(); i++)
		{
			getTree().collapsePath(getPathForNode((DefaultMutableTreeNode) root.getChildAt(i)));
		}
	}
}
