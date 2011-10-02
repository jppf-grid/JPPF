/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import javax.swing.tree.*;

import org.jppf.utils.LocalizationUtils;

/**
 * Abstract tree table model implementation for tree table-based options.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFTreeTableModel extends AbstractTreeTableModel
{
	/**
	 * Base name for localization bundle lookups.
	 */
	protected String BASE = null;

	/**
	 * Initialize this model with the specified tree root.
	 * @param root - the root of the tree.
	 */
	public AbstractJPPFTreeTableModel(TreeNode root)
	{
		super(root);
	}

	/**
	 * Tells if a cell can be edited.
	 * @param node not used.
	 * @param column not used.
	 * @return true if the cell can be edited, false otherwise.
	 * @see org.jppf.ui.treetable.AbstractTreeTableModel#isCellEditable(java.lang.Object, int)
	 */
	@Override
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
	@Override
    public void setValueAt(Object value, Object node, int column)
	{
	}

	/**
	 * Return the child at the specified index from the specified parent node.
	 * @param parent the parent to get the child from.
	 * @param index the index at which to get the child
	 * @return the child node, or null if the index is not valid.
	 * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
	 */
	@Override
    public Object getChild(Object parent, int index)
	{
		try
		{
			return ((TreeNode) parent).getChildAt(index);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Get the number of children for the specified node.
	 * @param parent the node for which to get the number of children. 
	 * @return the number of children as an int.
	 * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
	 */
	@Override
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
	 * Handle a node update.
	 * @param node - the node to update.
	 */
	public void changeNode(DefaultMutableTreeNode node)
	{
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
		int pos = parent.getIndex(node);
		fireTreeNodesChanged(parent, parent.getPath(), new int[] { pos }, new Object[] { node } );
	}

	/**
	 * Determine the class of th specified column.
	 * @param column - the column index.
	 * @return a <code>Class</code> instance.
	 * @see org.jppf.ui.treetable.AbstractTreeTableModel#getColumnClass(int)
	 */
	@Override
    public Class getColumnClass(int column)
	{
		return (column == 0) ? TreeTableModel.class : String.class;
	}

	/**
	 * Get a localized message given its unique name and the current locale.
	 * @param message the unique name of the localized message.
	 * @return a message in the current locale, or the default locale 
	 * if the localization for the current locale is not found. 
	 */
	protected String localize(String message)
	{
		return LocalizationUtils.getLocalized(BASE, message);
	}
}
