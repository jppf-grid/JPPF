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

import javax.swing.tree.*;

import org.jppf.ui.treetable.*;

/**
 * Tree table model for the tree table.
 */
public class JobTreeTableModel extends AbstractJPPFTreeTableModel implements TreeTableModel
{
	/**
	 * Column number for the node's url.
	 */
	private static final int NODE_URL = 0;
	/**
	 * Column number for the node's thread pool size.
	 */
	private static final int INITIAL_TASK_COUNT = 1;
	/**
	 * Column number for the node's last event.
	 */
	private static final int TASK_COUNT = 2;
	/**
	 * Column number for the node's last event.
	 */
	private static final int PRIORITY = 3;

	/**
	 * Initialize this model with the specified tree root.
	 * @param node - the root of the tree.
	 */
	public JobTreeTableModel(TreeNode node)
	{
		super(node);
		BASE = "org.jppf.ui.i18n.JobDataPage";
	}

	/**
	 * Get the number of columns in the table.
	 * @return the number of columns as an int.
	 * @see org.jppf.ui.treetable.TreeTableModel#getColumnCount()
	 */
	public int getColumnCount()
	{
		return 4;
	}

	/**
	 * Returns which object is displayed in this column.
	 * @param node - the node for which to get a value.
	 * @param column - the column from which to set a value.
	 * @return the value from the specified node and column.
	 * @see org.jppf.ui.treetable.TreeTableModel#getValueAt(java.lang.Object, int)
	 */
	public Object getValueAt(Object node, int column)
	{
		Object res = "";
		if (node instanceof DefaultMutableTreeNode)
		{
			DefaultMutableTreeNode defNode = (DefaultMutableTreeNode) node;
			if (defNode.getUserObject() instanceof JobData)
			{
				JobData data = (JobData) defNode.getUserObject();
				switch (column)
				{
					case NODE_URL:
						//res = info.toString();
						break;
					case TASK_COUNT:
						if (data.getType().equals(JobDataType.SUB_JOB) || data.getType().equals(JobDataType.JOB)) res = "" + data.getJobInformation().getTaskCount();
						break;
					case INITIAL_TASK_COUNT:
						if (data.getType().equals(JobDataType.JOB)) res = "" + data.getJobInformation().getInitialTaskCount();
						break;
					case PRIORITY:
						if (data.getType().equals(JobDataType.JOB)) res = "" + data.getJobInformation().getPriority();
						break;
					default:
						res = "";
				}
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
				//res = localize("column.node.url");
				res = "Driver / Job / Node";
				break;
			case TASK_COUNT:
				res = "Current task count";
				break;
			case INITIAL_TASK_COUNT:
				res = "Initial task count";
				break;
			case PRIORITY:
				res = "Priority";
				break;
		}
		return res;
	}
}
