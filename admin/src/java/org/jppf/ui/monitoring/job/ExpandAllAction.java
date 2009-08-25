/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.ui.monitoring.job;

import java.awt.event.ActionEvent;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.ui.actions.AbstractUpdatableAction;
import org.jppf.ui.treetable.JTreeTable;

/**
 * Action handler associated with the "expand all" button.
 * @author Laurent Cohen
 */
public class ExpandAllAction extends AbstractUpdatableAction
{
	/**
	 * The tree table this action is for.
	 */
	private JTreeTable treeTable = null;

	/**
	 * Initialize this action with the specified tree table component.
	 * @param treeTable - the tree table this action is for.
	 */
	public ExpandAllAction(JTreeTable treeTable)
	{
		this.treeTable = treeTable;
	}

	/**
   * Invoked when the "Expand all" button is pressed.
	 * @param event - encapsulates the event data.
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event)
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeTable.getTree().getModel().getRoot();
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
			for (int i=0; i<node.getChildCount(); i++) expand((DefaultMutableTreeNode) node.getChildAt(i));
		}
	}
}
