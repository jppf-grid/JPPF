/*
 * %W% %E%
 *
 * Copyright 1997, 1998 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer. 
 *   
 * - Redistribution in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution. 
 *   
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.  
 * 
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
 * DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT OF OR
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THIS SOFTWARE OR
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE 
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,   
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER  
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF 
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS 
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */
package org.jppf.ui.treetable;

import javax.swing.event.*;
import javax.swing.tree.TreePath;

import org.slf4j.*;

/**
 * An abstract implementation of the TreeTableModel interface, handling the list of listeners.
 * 
 * @version %I% %G%
 * 
 * @author Philip Milne
 */

public abstract class AbstractTreeTableModel implements TreeTableModel
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(AbstractTreeTableModel.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The tree root.
	 */
	protected Object root;

	/**
	 * The list of event listeners for this tree table model.
	 */
	protected EventListenerList listenerList = new EventListenerList();

	/**
	 * Initialize this model with the specified tree root.
	 * @param root the root of the tree.
	 */
	public AbstractTreeTableModel(Object root)
	{
		this.root = root;
	}

	//
	// Default implementations for methods in the TreeModel interface.
	//

	/**
	 * {@inheritDoc}
	 */
	public Object getRoot()
	{
		return root;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isLeaf(Object node)
	{
		return getChildCount(node) == 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public void valueForPathChanged(TreePath path, Object newValue)
	{
	}

	/**
	 * {@inheritDoc}
	 */
	public int getIndexOfChild(Object parent, Object child)
	{
		// This is not called in the JTree's default mode: use a naive implementation.
		for (int i = 0; i < getChildCount(parent); i++)
		{
			if (getChild(parent, i).equals(child))
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addTreeModelListener(TreeModelListener l)
	{
		listenerList.add(TreeModelListener.class, l);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeTreeModelListener(TreeModelListener l)
	{
		listenerList.remove(TreeModelListener.class, l);
	}

	/**
	 * Notify all listeners that have registered interest for notification on this event type. The event instance is
	 * lazily created using the parameters passed into the fire method.
	 * @param source
	 * @param path
	 * @param childIndices
	 * @param children
	 * @see EventListenerList
	 */
	protected void fireTreeNodesChanged(Object source, Object[] path, int[] childIndices, Object[] children)
	{
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == TreeModelListener.class)
			{
				// Lazily create the event:
				if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
				((TreeModelListener) listeners[i + 1]).treeNodesChanged(e);
			}
		}
	}

	/**
	 * Notify all listeners that have registered interest for notification on this event type. The event instance is
	 * lazily created using the parameters passed into the fire method.
	 * @param source
	 * @param path
	 * @param childIndices
	 * @param children
	 * @see EventListenerList
	 */
	protected void fireTreeNodesInserted(Object source, Object[] path, int[] childIndices, Object[] children)
	{
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == TreeModelListener.class)
			{
				// Lazily create the event:
				if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
				((TreeModelListener) listeners[i + 1]).treeNodesInserted(e);
			}
		}
	}

	/**
	 * Notify all listeners that have registered interest for notification on this event type. The event instance is
	 * lazily created using the parameters passed into the fire method.
	 * @param source
	 * @param path
	 * @param childIndices
	 * @param children
	 * @see EventListenerList
	 */
	protected void fireTreeNodesRemoved(Object source, Object[] path, int[] childIndices, Object[] children)
	{
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == TreeModelListener.class)
			{
				// Lazily create the event:
				try
				{
					if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
					((TreeModelListener) listeners[i + 1]).treeNodesRemoved(e);
				}
				catch(Exception ex)
				{
					log.error(ex.getMessage(), ex);
				}
			}
		}
	}

	/**
	 * Notify all listeners that have registered interest for notification on this event type. The event instance is
	 * lazily created using the parameters passed into the fire method.
	 * @param source
	 * @param path
	 * @param childIndices
	 * @param children
	 * @see EventListenerList
	 */
	protected void fireTreeStructureChanged(Object source, Object[] path, int[] childIndices, Object[] children)
	{
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == TreeModelListener.class)
			{
				// Lazily create the event:
				if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
				((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
			}
		}
	}

	//
	// Default implementations for methods in the TreeTableModel interface.
	//

	/**
	 * {@inheritDoc}
	 */
	public Class getColumnClass(int column)
	{
		return Object.class;
	}

	/**
	 * By default, make the column with the Tree in it the only editable one. Making this column editable causes the
	 * JTable to forward mouse and keyboard events in the Tree column to the underlying JTree.
	 * {inheritDoc}
	 */
	public boolean isCellEditable(Object node, int column)
	{
		return getColumnClass(column) == TreeTableModel.class;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setValueAt(Object aValue, Object node, int column)
	{
	}

	// Left to be implemented in the subclass:

	/*
	 * public Object getChild(Object parent, int index) public int getChildCount(Object parent) public int
	 * getColumnCount() public String getColumnName(Object node, int column) public Object getValueAt(Object node, int
	 * column)
	 */

}
