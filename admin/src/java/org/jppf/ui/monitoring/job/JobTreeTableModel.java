/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.ui.monitoring.job;

import javax.swing.tree.*;

import org.jppf.job.JobInformation;
import org.jppf.ui.treetable.*;

/**
 * Tree table model for the tree table.
 */
public class JobTreeTableModel extends AbstractJPPFTreeTableModel implements TreeTableModel
{
  /**
   * Column number for the node's url.
   */
  static final int NODE_URL = 0;
  /**
   * Column number for the job's execution state.
   */
  static final int JOB_STATE = 1;
  /**
   * Column number for the job's initial task count (at submission time).
   */
  static final int INITIAL_TASK_COUNT = 2;
  /**
   * Column number for the dispatched job's task count.
   */
  static final int TASK_COUNT = 3;
  /**
   * Column number for the job's priority.
   */
  static final int PRIORITY = 4;
  /**
   * Column number for the maximum number of nodes a job can run on.
   */
  static final int MAX_NODES = 5;

  /**
   * Initialize this model with the specified tree root.
   * @param node - the root of the tree.
   */
  public JobTreeTableModel(final TreeNode node)
  {
    super(node);
    BASE = "org.jppf.ui.i18n.JobDataPage";
  }

  /**
   * Get the number of columns in the table.
   * @return the number of columns as an int.
   * @see org.jppf.ui.treetable.TreeTableModel#getColumnCount()
   */
  @Override
  public int getColumnCount()
  {
    return 6;
  }

  /**
   * Returns which object is displayed in this column.
   * @param node - the node for which to get a value.
   * @param column - the column from which to set a value.
   * @return the value from the specified node and column.
   * @see org.jppf.ui.treetable.TreeTableModel#getValueAt(java.lang.Object, int)
   */
  @Override
  public Object getValueAt(final Object node, final int column)
  {
    Object res = "";
    if (node instanceof DefaultMutableTreeNode)
    {
      DefaultMutableTreeNode defNode = (DefaultMutableTreeNode) node;
      if (defNode.getUserObject() instanceof JobData)
      {
        JobData data = (JobData) defNode.getUserObject();
        JobInformation jobInfo = data.getJobInformation();
        switch (column)
        {
          case NODE_URL:
            break;
          case JOB_STATE:
            if (data.getType().equals(JobDataType.JOB))
            {
              String s = null;
              if (jobInfo.isPending()) s = "pending";
              else s = jobInfo.isSuspended() ? "suspended" : "executing";
              res = localize("job.state." + s);
            }
            break;
          case TASK_COUNT:
            if (data.getType().equals(JobDataType.SUB_JOB) || data.getType().equals(JobDataType.JOB)) res = Integer.toString(jobInfo.getTaskCount());
            break;
          case INITIAL_TASK_COUNT:
            if (data.getType().equals(JobDataType.JOB)) res = Integer.toString(jobInfo.getInitialTaskCount());
            break;
          case PRIORITY:
            if (data.getType().equals(JobDataType.JOB)) res = Integer.toString(jobInfo.getPriority());
            break;
          case MAX_NODES:
            if (data.getType().equals(JobDataType.JOB))
            {
              int n = jobInfo.getMaxNodes();
              // \u221E = infinity symbol
              if (n == Integer.MAX_VALUE) res = "\u221E";
              else res = Integer.toString(n);
            }
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
  @Override
  public String getColumnName(final int column)
  {
    String res = "";
    switch (column)
    {
      case NODE_URL:
        //res = localize("column.node.url");
        res = "Driver / Job / Node";
        break;
      case TASK_COUNT:
        res = localize("job.current.task.count");
        break;
      case JOB_STATE:
        res = localize("job.state");
        break;
      case INITIAL_TASK_COUNT:
        res = localize("job.initial.task.count");
        break;
      case PRIORITY:
        res = localize("job.priority");
        break;
      case MAX_NODES:
        res = localize("job.max.nodes");
        break;
    }
    return res;
  }
}
