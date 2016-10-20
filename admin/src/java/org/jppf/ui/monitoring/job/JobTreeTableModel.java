/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.util.Locale;

import javax.swing.tree.*;

import org.jppf.client.monitoring.jobs.*;
import org.jppf.job.JobInformation;
import org.jppf.ui.treetable.AbstractJPPFTreeTableModel;

/**
 * Tree table model for the tree table.
 */
public class JobTreeTableModel extends AbstractJPPFTreeTableModel {
  /**
   * Column number for the node's url.
   */
  public static final int NODE_URL = 0;
  /**
   * Column number for the job's execution state.
   */
  public static final int JOB_STATE = 1;
  /**
   * Column number for the job's initial task count (at submission time).
   */
  public static final int INITIAL_TASK_COUNT = 2;
  /**
   * Column number for the dispatched job's task count.
   */
  public static final int TASK_COUNT = 3;
  /**
   * Column number for the job's priority.
   */
  public static final int PRIORITY = 4;
  /**
   * Column number for the maximum number of nodes a job can run on.
   */
  public static final int MAX_NODES = 5;

  /**
   * Initialize this model with the specified tree root.
   * @param node - the root of the tree.
   */
  public JobTreeTableModel(final TreeNode node) {
    super(node);
    BASE = "org.jppf.ui.i18n.JobDataPage";
  }

  /**
   * Initialize this model with the specified tree root.
   * @param node - the root of the tree.
   * @param locale the locale used to translate column headers and cell values.
   */
  public JobTreeTableModel(final TreeNode node, final Locale locale) {
    super(node, locale);
    BASE = "org.jppf.ui.i18n.JobDataPage";
  }

  @Override
  public int getColumnCount() {
    return 6;
  }

  @Override
  public Object getValueAt(final Object node, final int column) {
    Object res = "";
    if (node instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode defNode = (DefaultMutableTreeNode) node;
      if (defNode.getUserObject() instanceof AbstractJobComponent) {
        AbstractJobComponent data = (AbstractJobComponent) defNode.getUserObject();
        switch (column) {
          case NODE_URL:
            break;
          case JOB_STATE:
            if (data instanceof Job) {
              JobInformation jobInfo = ((Job) data).getJobInformation();
              String s = null;
              if (jobInfo.isPending()) s = "pending";
              else s = jobInfo.isSuspended() ? "suspended" : "executing";
              res = localize("job.state." + s);
            }
            break;
          case TASK_COUNT:
            if (data instanceof Job) res = Integer.toString(((Job) data).getJobInformation().getTaskCount());
            else if (data instanceof JobDispatch) res = Integer.toString(((JobDispatch) data).getJobInformation().getTaskCount());
            break;
          case INITIAL_TASK_COUNT:
            if (data instanceof Job) res = Integer.toString(((Job) data).getJobInformation().getInitialTaskCount());
            else if (data instanceof JobDispatch) res = Integer.toString(((JobDispatch) data).getJobInformation().getInitialTaskCount());
            break;
          case PRIORITY:
            if (data instanceof Job) res = Integer.toString(((Job) data).getJobInformation().getPriority());
            break;
          case MAX_NODES:
            if (data instanceof Job) {
              int n = ((Job) data).getJobInformation().getMaxNodes();
              // \u221E = infinity symbol
              res = (n == Integer.MAX_VALUE) ? "\u221E" : Integer.toString(n);
            }
            break;
          default:
            res = "";
        }
      }
    }
    return res;
  }

  @Override
  public String getBaseColumnName(final int column) {
    switch (column) {
      case NODE_URL:
        return "column.job.url";
      case TASK_COUNT:
        return "column.job.current.task.count";
      case JOB_STATE:
        return "column.job.state";
      case INITIAL_TASK_COUNT:
        return "column.job.initial.task.count";
      case PRIORITY:
        return "column.job.priority";
      case MAX_NODES:
        return "column.job.max.nodes";
    }
    return "";
  }
}
