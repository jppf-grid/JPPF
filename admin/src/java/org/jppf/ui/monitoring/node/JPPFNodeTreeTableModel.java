/*
 * JPPF.
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

import org.jppf.management.JPPFNodeState;
import org.jppf.ui.monitoring.data.NodeInfoHolder;
import org.jppf.ui.treetable.*;
import org.jppf.utils.LocalizationUtils;

/**
 * Tree table model for the tree table.
 */
public class JPPFNodeTreeTableModel extends AbstractTreeTableModel implements TreeTableModel
{
	/**
	 * Base name for localization bundle lookups.
	 */
	private static final String BASE = "org.jppf.ui.i18n.NodeDataPage";
	/**
	 * Column number for the node's url.
	 */
	private static final int NODE_URL = 0;
	/**
	 * Column number for the node's thread pool size.
	 */
	private static final int NODE_THREADS = 1;
	/**
	 * Column number for the node's last event.
	 */
	private static final int NODE_STATUS = 2;
	/**
	 * Column number for the node's last event.
	 */
	private static final int EXECUTION_STATUS = 3;
	/**
	 * Column number for the node's number of tasks executed.
	 */
	private static final int NB_TASKS = 4;
	/**
	 * Column number for the node's latest task event.
	 */
	private static final int TASK_EVENT = 5;

	/**
	 * Initialize this model witht he specified tree.
	 * @param node the root of the tree.
	 */
	public JPPFNodeTreeTableModel(TreeNode node)
	{
		super(node);
	}

	/**
	 * Get the number of columns in the table.
	 * @return the number of columns as an int.
	 * @see org.jppf.ui.treetable.TreeTableModel#getColumnCount()
	 */
	public int getColumnCount()
	{
		return 6;
	}

	/**
	 * Returns which object is displayed in this column.
	 * @param node the node for which to get a value.
	 * @param column the column from which to set a value.
	 * @return the value from the specified node and column.
	 * @see org.jppf.ui.treetable.TreeTableModel#getValueAt(java.lang.Object, int)
	 */
	public Object getValueAt(Object node, int column)
	{
		Object res = "";
		if (node instanceof DefaultMutableTreeNode)
		{
			DefaultMutableTreeNode defNode = (DefaultMutableTreeNode) node;
			if (defNode.getUserObject() instanceof NodeInfoHolder)
			{
				NodeInfoHolder info = (NodeInfoHolder) defNode.getUserObject();
				JPPFNodeState state = info.getState();
				switch (column)
				{
					case NODE_URL:
						res = info.toString();
						break;
					case NODE_THREADS:
						res = "" + state.getThreadPoolSize() + " / " + state.getThreadPriority();
						break;
					case NODE_STATUS:
						res = "" + state.getConnectionStatus();
						break;
					case EXECUTION_STATUS:
						res = "" + state.getExecutionStatus();
						break;
					case NB_TASKS:
						res = "" + state.getNbTasksExecuted();
						break;
					case TASK_EVENT:
						res = "" + state.getTaskNotification();
						break;
				}
			}
			else
			{
				if (column == 0) res = defNode.getUserObject().toString();
			}
		}
		return res;
	}

	/**
	 * What the TableHeader displays when the Table is in a JScrollPane.
	 * @param column the index of the column for which to get a title.
	 * @return the column title as a string.
	 * @see org.jppf.ui.treetable.TreeTableModel#getColumnName(int)
	 */
	public String getColumnName(int column)
	{
		String res = "";
		switch (column)
		{
			case NODE_URL:
				res = localize("column.node.url");
				break;
			case NODE_THREADS:
				res = localize("column.node.threads");
				break;
			case NODE_STATUS:
				res = localize("column.node.status");
				break;
			case EXECUTION_STATUS:
				res = localize("column.node.execution.status");
				break;
			case NB_TASKS:
				res = localize("column.nb.tasks");
				break;
			case TASK_EVENT:
				res = localize("column.task.status");
				break;
		}
		return res;
	}

	/**
	 * Tells if a cell can be edited.
	 * @param node not used.
	 * @param column not used.
	 * @return true if the cell can be edited, false otherwise.
	 * @see org.jppf.ui.treetable.AbstractTreeTableModel#isCellEditable(java.lang.Object, int)
	 */
	public boolean isCellEditable(Object node, int column)
	{
		//return false;
		return super.isCellEditable(node, column);
	}

	/**
	 * Called when done editing a cell. This method has an empty implementation and does nothing.
	 * @param value not used.
	 * @param node not used.
	 * @param column not used.
	 * @see org.jppf.ui.treetable.AbstractTreeTableModel#setValueAt(java.lang.Object, java.lang.Object, int)
	 */
	public void setValueAt(Object value, Object node, int column)
	{
	}

	/**
	 * Get a localized message given its unique name and the current locale.
	 * @param message the unique name of the localized message.
	 * @return a message in the current locale, or the default locale 
	 * if the localization for the current locale is not found. 
	 */
	private String localize(String message)
	{
		return LocalizationUtils.getLocalized(BASE, message);
	}

	/**
	 * Return the child at the spcified index from the specified parent node.
	 * @param parent - the parent to get the child from.
	 * @param index - the index at which to get the child
	 * @return the child node, or null if the index is not valid.
	 * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
	 */
	public Object getChild(Object parent, int index)
	{
		return ((TreeNode) parent).getChildAt(index);
	}

	/**
	 * Get the number of children for the specified node.
	 * @param parent the node for which to get the number of children. 
	 * @return the number of children as an int.
	 * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
	 */
	public int getChildCount(Object parent)
	{
		return ((TreeNode) parent).getChildCount();
	}

	/**
	 * Insert the specified child into the specified parent's list of children at the specified position.
	 * @param child - the node to insert into the parent.
	 * @param parent - the node into which to insert the child.
	 * @param pos - the position at which to insert the node.
	 */
	public void insertNodeInto(DefaultMutableTreeNode child, DefaultMutableTreeNode parent, int pos)
	{
		parent.insert(child, pos);
		fireTreeNodesInserted(parent, parent.getPath(), new int[] { pos }, new Object[] { child } );
	}

	/**
	 * Remove a node from the tree.
	 * @param node - the node to remove from the parent.
	 */
	public void removeNodeFromParent(DefaultMutableTreeNode node)
	{
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
		int pos = parent.getIndex(node);
		parent.remove(node);
		fireTreeNodesRemoved(parent, parent.getPath(), new int[] { pos }, new Object[] { node } );
	}

	/**
	 * Determine the class of th specified column.
	 * @param column - the column index.
	 * @return a <code>Class</code> instance.
	 * @see org.jppf.ui.treetable.AbstractTreeTableModel#getColumnClass(int)
	 */
	public Class getColumnClass(int column)
	{
		return (column == 0) ? TreeTableModel.class : String.class;
	}
}
