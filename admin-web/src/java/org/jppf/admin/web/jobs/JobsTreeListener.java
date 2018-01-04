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

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.admin.web.JPPFWebConsoleApplication;
import org.jppf.admin.web.filter.*;
import org.jppf.admin.web.tabletree.*;
import org.jppf.client.monitoring.jobs.*;
import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.treetable.AbstractJPPFTreeTableModel;
import org.jppf.ui.utils.*;
import org.jppf.utils.collections.*;

/**
 * Listens to job events so as to update the jobs view.
 * @author Laurent Cohen
 */
public class JobsTreeListener extends AbstractMonitoringListener implements JobMonitoringListener {
  /**
   * Initialize with the specified tree model and selection handler.
   * @param treeModel the tree table model.
   * @param selectionHandler handles the selection of rows in the tree table.
   * @param nodeFilter the node filter to use.
   */
  public JobsTreeListener(final AbstractJPPFTreeTableModel treeModel, final SelectionHandler selectionHandler, final TopologyFilter nodeFilter) {
    super(treeModel, selectionHandler, nodeFilter);
  }

  @Override
  public void driverAdded(final JobMonitoringEvent event) {
    JobsUtils.addDriver(treeModel, event.getJobDriver());
  }

  @Override
  public void driverRemoved(final JobMonitoringEvent event) {
    final JobDriver driver = event.getJobDriver();
    selectionHandler.unselect(driver.getUuid());
    for (final Job job: driver.getJobs()) selectionHandler.unselect(job.getUuid());
    JobsUtils.removeDriver(treeModel, driver);
  }

  @Override
  public void jobAdded(final JobMonitoringEvent event) {
    final DefaultMutableTreeNode jobNode = JobsUtils.addJob(treeModel, event.getJobDriver(), event.getJob());
    if ((jobNode != null) && (getTableTree() != null)) {
      final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) jobNode.getParent();
      if (parent.getChildCount() == 1) getTableTree().expand(parent);
    }
  }

  @Override
  public void jobRemoved(final JobMonitoringEvent event) {
    JobsUtils.removeJob(treeModel, event.getJobDriver(), event.getJob());
    selectionHandler.unselect(event.getJob().getUuid());
  }

  @Override
  public void jobUpdated(final JobMonitoringEvent event) {
    JobsUtils.updateJob(treeModel, event.getJob());
  }

  @Override
  public void jobDispatchAdded(final JobMonitoringEvent event) {
    final TopologyNode node = event.getJobDispatch().getNode();
    if ((node == null) || !isAccepted(nodeFilter, node)) return;
    addDispatch(event.getJob(), event.getJobDispatch());
  }

  @Override
  public void jobDispatchRemoved(final JobMonitoringEvent event) {
    JobsUtils.removeJobDispatch(treeModel, event.getJob(), event.getJobDispatch());
    selectionHandler.unselect(event.getJobDispatch().getUuid());
  }

  @Override
  public void onFilterChange(final TopologyFilterEvent event) {
    final JobMonitor monitor = JPPFWebConsoleApplication.get().getJobMonitor();
    final CollectionMap<Job, JobDispatch> toRemove = new ArrayListHashMap<>();
    final CollectionMap<Job, JobDispatch> toAdd = new ArrayListHashMap<>();
    final List<JobDriver> allDrivers = monitor.getJobDrivers();
    final DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
    for (final JobDriver driver: allDrivers) {
      final DefaultMutableTreeNode driverDmtn = TreeTableUtils.findComponent(root, driver.getUuid());
      if (driverDmtn == null) continue;
      for (final Job job: driver.getJobs()) {
        final DefaultMutableTreeNode jobDmtn = TreeTableUtils.findComponent(driverDmtn, job.getUuid());
        if (jobDmtn == null) continue;
        for (final JobDispatch dispatch: job.getJobDispatches()) {
          final boolean accepted = isAccepted(nodeFilter, dispatch.getNode());
          final DefaultMutableTreeNode dispatchDmtn = TreeTableUtils.findComponent(jobDmtn, dispatch.getUuid());
          final boolean present = dispatchDmtn!= null;
          if (accepted && !present) toAdd.putValue(job, dispatch);
          else if (!accepted && present) toRemove.putValue(job, dispatch);
        }
      }
    }
    for (final Map.Entry<Job, Collection<JobDispatch>> entry: toRemove.entrySet()) {
      for (final JobDispatch dispatch: entry.getValue()) {
        JobsUtils.removeJobDispatch(treeModel, entry.getKey(), dispatch);
        selectionHandler.unselect(dispatch.getUuid());
      }
    }
    for (final Map.Entry<Job, Collection<JobDispatch>> entry: toAdd.entrySet()) {
      for (final JobDispatch dispatch: entry.getValue()) addDispatch(entry.getKey(), dispatch);
    }
  }

  /**
   * Add a dispatch to the specified job.
   * @param job the job holding the dispatch.
   * @param dispatch the dispatch to add.
   */
  private void addDispatch(final Job job, final JobDispatch dispatch) {
    final DefaultMutableTreeNode dispatchDmtn = JobsUtils.addJobDispatch(treeModel, job, dispatch);
    if ((dispatchDmtn != null) && (getTableTree() != null)) {
      final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dispatchDmtn.getParent();
      if (parent.getChildCount() == 1) getTableTree().expand(parent);
    }
  }
}
