/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

package org.jppf.ui.monitoring.node;

import org.jdesktop.swingx.treetable.*;
import org.jppf.management.JPPFNodeState;
import org.jppf.ui.monitoring.data.*;
import org.jppf.utils.LocalizationUtils;

/**
 * Tree table model for the tree table.
 */
public class JPPFNodeTreeTableModel extends DefaultTreeTableModel
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
	public JPPFNodeTreeTableModel(TreeTableNode node)
	{
		super(node);
	}

	/**
	 * Get the number of columns in the table.
	 * @return the number of columns as an int.
	 * @see org.jdesktop.swingx.treetable.DefaultTreeTableModel#getColumnCount()
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
	 * @see org.jdesktop.swingx.treetable.DefaultTreeTableModel#getValueAt(java.lang.Object, int)
	 */
	public Object getValueAt(Object node, int column)
	{
		Object res = "";
		if (node instanceof DefaultMutableTreeTableNode)
		{
			DefaultMutableTreeTableNode defNode = (DefaultMutableTreeTableNode) node;
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
						res = state.getThreadPoolSize();
						break;
					case NODE_STATUS:
						res = state.getConnectionStatus();
						break;
					case EXECUTION_STATUS:
						res = state.getExecutionStatus();
						break;
					case NB_TASKS:
						res = state.getNbTasksExecuted();
						break;
					case TASK_EVENT:
						res = state.getTaskNotification();
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
	 * @see org.jdesktop.swingx.treetable.DefaultTreeTableModel#getColumnName(int)
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
	 * @see org.jdesktop.swingx.treetable.DefaultTreeTableModel#isCellEditable(java.lang.Object, int)
	 */
	public boolean isCellEditable(Object node, int column)
	{
		return false;
	}

	/**
	 * Called when done editing a cell.
	 * @param value not used.
	 * @param node not used.
	 * @param column not used.
	 * @see org.jdesktop.swingx.treetable.DefaultTreeTableModel#setValueAt(java.lang.Object, java.lang.Object, int)
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
}
