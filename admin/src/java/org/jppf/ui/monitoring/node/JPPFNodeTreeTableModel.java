/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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
import org.jppf.ui.treetable.AbstractJPPFTreeTableModel;

/**
 * Tree table model for the tree table.
 */
public class JPPFNodeTreeTableModel extends AbstractJPPFTreeTableModel
{
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
		BASE = "org.jppf.ui.i18n.NodeDataPage";
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
			if (defNode.getUserObject() instanceof TopologyData)
			{
				TopologyData info = (TopologyData) defNode.getUserObject();
				if (TopologyDataType.DRIVER.equals(info.getType()) && (column > 0)) return res;
				JPPFNodeState state = info.getNodeState();
				if (state == null) return res;
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
}
