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

package org.jppf.admin.web.jobs;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.admin.web.jobs.maxnodes.MaxNodesAction;
import org.jppf.admin.web.jobs.priority.PriorityAction;
import org.jppf.admin.web.tabletree.*;
import org.jppf.client.monitoring.jobs.*;
import org.jppf.ui.treetable.*;

/**
 *
 * @author Laurent Cohen
 */
public class JobsTreeData extends TableTreeData {
  /**
   * Listens to job events.
   */
  private JobMonitoringListener listener;

  /**
   *
   */
  public JobsTreeData() {
    super(TreeViewType.JOBS);
    getSelectionHandler().setFilter(new TreeNodeFilter() {
      @Override
      public boolean accepts(final DefaultMutableTreeNode node) {
        AbstractJobComponent comp = (AbstractJobComponent) node.getUserObject();
        return (comp instanceof Job);
      }
    });
    ActionHandler ah = getActionHandler();
    ah.addAction(JobsConstants.CANCEL_ACTION, new CancelJobLink.Action());
    UpdatableAction suspendAction = new SuspendJobLink.Action();
    ah.addAction(JobsConstants.SUSPEND_ACTION, suspendAction);
    ah.addAction(JobsConstants.SUSPEND_REQUEUE_ACTION, suspendAction);
    ah.addAction(JobsConstants.RESUME_ACTION, new ResumeJobLink.Action());
    ah.addAction(JobsConstants.UPDATE_MAX_NODES_ACTION, new MaxNodesAction());
    ah.addAction(JobsConstants.UPDATE_PRIORITY_ACTION, new PriorityAction());
  }

  /**
   * @return the job event listener.
   */
  public synchronized JobMonitoringListener getListener() {
    return listener;
  }

  /**
   * Set the job event listener.
   * @param listener the listener to set.
   */
  public synchronized void setListener(final JobMonitoringListener listener) {
    this.listener = listener;
  }
}
