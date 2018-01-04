/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import org.jppf.admin.web.*;
import org.jppf.admin.web.filter.TopologyFilter;
import org.jppf.admin.web.jobs.maxnodes.MaxNodesAction;
import org.jppf.admin.web.jobs.priority.PriorityAction;
import org.jppf.admin.web.tabletree.*;
import org.jppf.admin.web.utils.UpdatableAction;
import org.jppf.client.monitoring.jobs.*;
import org.jppf.client.monitoring.topology.TopologyNode;
import org.jppf.ui.monitoring.job.JobTreeTableModel;
import org.jppf.ui.treetable.*;
import org.jppf.ui.utils.JobsUtils;

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
        final AbstractJobComponent comp = (AbstractJobComponent) node.getUserObject();
        return (comp instanceof Job);
      }
    });
    listener = new JobsTreeListener(model, getSelectionHandler(), JPPFWebSession.get().getNodeFilter());
    JPPFWebConsoleApplication.get().getJobMonitor().addJobMonitoringListener(listener);
    final ActionHandler ah = getActionHandler();
    ah.addAction(JobsConstants.CANCEL_ACTION, new CancelJobLink.Action());
    final UpdatableAction suspendAction = new SuspendJobLink.Action();
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

  @Override
  public void cleanup() {
    super.cleanup();
    if (listener != null) JPPFWebConsoleApplication.get().getJobMonitor().removeJobMonitoringListener(listener);
  }

  @Override
  protected void createTreeTableModel() {
    final JPPFWebSession session = JPPFWebSession.get();
    final TopologyFilter filter = session.getNodeFilter();
    model = new JobTreeTableModel(new DefaultMutableTreeNode("tree.root.name"), session.getLocale());
    for (final JobDriver driver: JPPFWebConsoleApplication.get().getJobMonitor().getJobDrivers()) {
      JobsUtils.addDriver(model, driver);
      for (final Job job: driver.getJobs()) {
        JobsUtils.addJob(model, driver, job);
        for (final JobDispatch dispatch: job.getJobDispatches()) {
          final TopologyNode node = dispatch.getNode();
          if (AbstractMonitoringListener.isAccepted(filter, node)) JobsUtils.addJobDispatch(model, job, dispatch);
        }
      }
    }
  }
}
