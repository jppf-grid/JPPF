/*
 * @(#)TreeTableModelAdapter.java       1.2 98/10/27
 *
 * Copyright 1997, 1998 Sun Microsystems, Inc.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Sun Microsystems nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jppf.ui.treetable;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.*;

import org.slf4j.*;

/**
 * This is a wrapper class takes a TreeTableModel and implements the table model interface. The implementation is
 * trivial, with all of the event dispatching support provided by the superclass: the AbstractTableModel.
 * 
 * @version 1.2 10/27/98
 * 
 * @author Philip Milne
 * @author Scott Violet
 */
public class TreeTableModelAdapter extends AbstractTableModel
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(TreeTableModelAdapter.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The underlying JTree.
	 */
	JTree tree;
	/**
	 * The underlying JTable.
	 */
	JTable table;
	/**
	 * An empty tree path array.
	 */
	private static final TreePath[] ZERO_PATH = new TreePath[0];
	/**
	 * The tree table model.
	 */
	TreeTableModel treeTableModel;

	/**
	 * Initialize this model adapter with the specified tree table model and JTree.
	 * @param treeTableModel the tree table model.
	 * @param tree the underlying JTree.
	 */
	public TreeTableModelAdapter(TreeTableModel treeTableModel, final JTree tree)
	{
		this.tree = tree;
		this.treeTableModel = treeTableModel;

		tree.addTreeExpansionListener(new TreeExpansionListener()
		{
			/**
			 * Don't use fireTableRowsInserted() here; the selection model would get updated twice.
			 * {@inheritDoc}
			 */
			public void treeExpanded(TreeExpansionEvent event)
			{
				TreePath[] paths = getSelectedPaths();
				if (debugEnabled) log.debug("selected paths = " + dumpTreePaths(paths));
				fireTableDataChanged();
				setSelectedPaths(paths);
			}

			/**
			 * {@inheritDoc}
			 */
			public void treeCollapsed(TreeExpansionEvent event)
			{
				TreePath[] paths = getSelectedPaths();
				if (debugEnabled) log.debug("selected paths = " + dumpTreePaths(paths));
				fireTableDataChanged();
				setSelectedPaths(paths);
			}
		});

		// Install a TreeModelListener that can update the table when tree changes. We use delayedFireTableDataChanged
		// as we can not be guaranteed the tree will have finished processing the event before us.
		treeTableModel.addTreeModelListener(new TreeModelListener()
		{
			/**
			 * {@inheritDoc}
			 */
			public void treeNodesChanged(TreeModelEvent e)
			{
				delayedFireTableDataChanged();
			}

			/**
			 * {@inheritDoc}
			 */
			public void treeNodesInserted(TreeModelEvent e)
			{
				delayedFireTableDataChanged();
			}

			/**
			 * {@inheritDoc}
			 */
			public void treeNodesRemoved(TreeModelEvent e)
			{
				delayedFireTableDataChanged();
			}

			/**
			 * {@inheritDoc}
			 */
			public void treeStructureChanged(TreeModelEvent e)
			{
				delayedFireTableDataChanged();
			}
		});
	}

	// Wrappers, implementing TableModel interface.

	/**
	 * Get the number of columns in the model.
	 * @return the number of columns in the model.
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount()
	{
		return treeTableModel.getColumnCount();
	}

	/**
	 * Get the name of the column at the specified index.
	 * @param column the index of the column.
	 * @return the default name of the column as a string.
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	public String getColumnName(int column)
	{
		return treeTableModel.getColumnName(column);
	}

	/**
   * Returns <code>Object.class</code> regardless of <code>columnIndex</code>.
   * @param column the column being queried.
   * @return the <code>Object.class</code> object.
	 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
	 */
	public Class getColumnClass(int column)
	{
		return treeTableModel.getColumnClass(column);
	}

	/**
	 * Get the number of rows in the model.
	 * @return the number of rows in the model.
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	public int getRowCount()
	{
		return tree.getRowCount();
	}

	/**
	 * Get the node for the specified row.
	 * @param row the row index.
	 * @return the corresponding node object, or null if this row doesn't exist in the tree.
	 */
	protected Object nodeForRow(int row)
	{
		TreePath treePath = null;
		try
		{
			treePath = tree.getPathForRow(row);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		if (treePath == null) return null;
		return treePath.getLastPathComponent();
	}

	/**
	 * Get the value of the cell at the specified coordinates.
	 * @param row the row coordinate.
	 * @param column the column coordinate.
	 * @return the value of the specified cell.
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	public Object getValueAt(int row, int column)
	{
		Object o = nodeForRow(row);
		if (o == null) return null;
		return treeTableModel.getValueAt(o, column);
	}

	/**
	 * Determine whether the cell at the specified coordinates is editable.
	 * @param row the row coordinate.
	 * @param column the column coordinate.
	 * @return true if the cell is editable, false otherwise.
	 * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
	 */
	public boolean isCellEditable(int row, int column)
	{
		return treeTableModel.isCellEditable(nodeForRow(row), column);
	}

	/**
	 * Set the value of the cell at the specified coordinates.
	 * @param value the value to set on the specified cell.
	 * @param row the row coordinate.
	 * @param column the column coordinate.
	 * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
	 */
	public void setValueAt(Object value, int row, int column)
	{
		treeTableModel.setValueAt(value, nodeForRow(row), column);
	}

	/**
	 * Invokes fireTableDataChanged after all the pending events have been processed. SwingUtilities.invokeLater is used
	 * to handle this.
	 */
	protected void delayedFireTableDataChanged()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				//fireTableRowsUpdated( 0, getRowCount() - 1 );
				TreePath[] paths = getSelectedPaths();
				fireTableDataChanged();
				setSelectedPaths(paths);
			}
		});
	}

	/**
	 * Get the currently selected paths in the tree.
	 * @return an array of <code>TreePath</code> objects.
	 */
	public TreePath[] getSelectedPaths()
	{
		return tree.getSelectionPaths();
	}

	/**
	 * Set the currently selected paths in the tree.
	 * @param paths an array of <code>TreePath</code> objects.
	 */
	public void setSelectedPaths(TreePath[] paths)
	{
		if (paths == null) return;
		List<TreePath> validPaths = new ArrayList<TreePath>();
		for (TreePath path: paths)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			if ((node != null) && (node.getParent() != null)) validPaths.add(path);
		}
		tree.setSelectionPaths(validPaths.toArray(ZERO_PATH));
	}

	/**
	 * Dump the specified tree paths to a string.
	 * @param paths the paths to dump.
	 * @return a string representation of the array of <code>TreePath</code> objects.
	 */
	public String dumpTreePaths(TreePath[] paths)
	{
		StringBuilder sb = new StringBuilder();
		if (paths == null) sb.append("null");
		else
		{
			sb.append("[");
			for (int i=0; i<paths.length; i++)
			{
				if (i > 0) sb.append(", ");
				sb.append(paths[i]);
			}
			sb.append("]");
		}
		return sb.toString();
	}

	/**
	 * Get the tree table model wrapped by this class.
	 * @return a {@link TreeTableModel} instance.
	 */
	public TreeTableModel getTreeTableModel()
	{
		return treeTableModel;
	}
}
