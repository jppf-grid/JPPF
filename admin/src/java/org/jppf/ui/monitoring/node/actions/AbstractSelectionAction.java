/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.ui.monitoring.node.NodeDataPanel;
import org.jppf.ui.treetable.*;

/**
 * Abstract superclass for actions that select nodes in the tree table.
 * @author Laurent Cohen
 */
public abstract class AbstractSelectionAction extends AbstractTopologyAction
{

	/**
	 * The tree table panel to which this action applies.
	 */
	protected NodeDataPanel panel = null;

	/**
	 * Initialize this action with the specified tree table panel.
	 * @param panel the tree table panel to which this action applies.
	 */
	public AbstractSelectionAction(NodeDataPanel panel)
	{
		this.panel = panel;
		setEnabled(true);
	}


	/**
	 * Get the list of all tree nodes representing a driver.
	 * @return a list of {@link DefaultMutableTreeNode} instances.
	 */
	protected List<DefaultMutableTreeNode> getDriverNodes()
	{
		List<DefaultMutableTreeNode> list = new ArrayList<DefaultMutableTreeNode>();
		JPPFTreeTable treeTable = panel.getTreeTable();
		TreeTableModelAdapter model = (TreeTableModelAdapter) treeTable.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getTreeTableModel().getRoot();
		for (int i=0; i<root.getChildCount(); i++)
		{
			list.add((DefaultMutableTreeNode) root.getChildAt(i));
		}
		return list;
	}

}