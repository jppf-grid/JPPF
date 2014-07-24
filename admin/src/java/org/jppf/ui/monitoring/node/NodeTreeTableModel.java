/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import java.text.NumberFormat;

import javax.swing.tree.*;

import org.jppf.management.*;
import org.jppf.ui.treetable.AbstractJPPFTreeTableModel;

/**
 * Tree table model for the tree table.
 */
public class NodeTreeTableModel extends AbstractJPPFTreeTableModel
{
  /**
   * Column number for the node's url.
   */
  static final int NODE_URL = 0;
  /**
   * Column number for the node's thread pool size.
   */
  static final int NODE_THREADS = 1;
  /**
   * Column number for the node's last event.
   */
  static final int NODE_STATUS = 2;
  /**
   * Column number for the node's last event.
   */
  static final int EXECUTION_STATUS = 3;
  /**
   * Column number for the node's number of tasks executed.
   */
  static final int NB_TASKS = 4;
  /**
   * Column number for the node's number of provisioned slaves.
   */
  static final int NB_SLAVES = 5;
  /**
   * 
   */
  static NumberFormat nf = createNumberFormat();

  /**
   * Initialize this model with the specified tree.
   * @param node the root of the tree.
   */
  public NodeTreeTableModel(final TreeNode node)
  {
    super(node);
    BASE = "org.jppf.ui.i18n.NodeDataPage";
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
   * @param node the node for which to get a value.
   * @param column the column from which to set a value.
   * @return the value from the specified node and column.
   * @see org.jppf.ui.treetable.TreeTableModel#getValueAt(java.lang.Object, int)
   */
  @Override
  public Object getValueAt(final Object node, final int column) {
    Object res = "";
    if (node instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode defNode = (DefaultMutableTreeNode) node;
      if (defNode.getUserObject() instanceof TopologyData) {
        TopologyData info = (TopologyData) defNode.getUserObject();
        JPPFManagementInfo mgtInfo = info.getNodeInformation();
        boolean isNode = (mgtInfo != null) && mgtInfo.isNode();
        if (info.isDriver() && (column > 0)) return res;
        JPPFNodeState state = info.getNodeState();
        if (state == null) return res;
        switch (column) {
          case NODE_URL:
            res = info.toString() + (isNode ? "" : "(peer driver)");
            break;
          case NODE_THREADS:
            if (isNode) {
              int n = state.getThreadPoolSize();
              int p = state.getThreadPriority();
              res = "" + (n <= 0 ? "?" : n) + " / " + (p <= 0 ? "?" : p);
            }
            break;
          case NODE_STATUS:
            if (isNode) res = state.getConnectionStatus();
            break;
          case EXECUTION_STATUS:
            if (isNode) res = state.getExecutionStatus();
            break;
          case NB_TASKS:
            if (isNode) res = nf.format(state.getNbTasksExecuted());
            break;
          case NB_SLAVES:
            if (isNode) {
              if ((mgtInfo != null) && mgtInfo.isMasterNode()) {
                int n = info.getNbSlaveNodes();
                res = n >= 0 ? nf.format(n) : "";
              } else res = "";
            }
            break;
        }
      } else {
        if (column == 0)res = defNode.getUserObject().toString();
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
      case NB_SLAVES:
        res = localize("column.nb.slaves");
        break;
    }
    return res;
  }

  /**
   * Get a number formatter for the number of tasks for each node.
   * @return a <code>NumberFormat</code> instance.
   */
  private static NumberFormat createNumberFormat()
  {
    NumberFormat nf = NumberFormat.getIntegerInstance();
    nf.setGroupingUsed(true);
    return nf;
  }
}
