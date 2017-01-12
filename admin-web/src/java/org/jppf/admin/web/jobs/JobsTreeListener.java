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
    JobDriver driver = event.getJobDriver();
    selectionHandler.unselect(driver.getUuid());
    for (Job job: driver.getJobs()) selectionHandler.unselect(job.getUuid());
    JobsUtils.removeDriver(treeModel, driver);
  }

  @Override
  public void jobAdded(final JobMonitoringEvent event) {
    DefaultMutableTreeNode jobNode = JobsUtils.addJob(treeModel, event.getJobDriver(), event.getJob());
    if ((jobNode != null) && (getTableTree() != null)) {
      DefaultMutableTreeNode parent = (DefaultMutableTreeNode) jobNode.getParent();
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
    TopologyNode node = event.getJobDispatch().getNode();
    if ((node == null) || !isAccepted(node)) return;
    addDispatch(event.getJob(), event.getJobDispatch());
    /*
    DefaultMutableTreeNode dispatchNode = JobsUtils.addJobDispatch(treeModel, event.getJob(), event.getJobDispatch());
    if ((dispatchNode != null) && (getTableTree() != null)) {
      DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dispatchNode.getParent();
      if (parent.getChildCount() == 1) getTableTree().expand(parent);
    }
    */
  }

  @Override
  public void jobDispatchRemoved(final JobMonitoringEvent event) {
    JobsUtils.removeJobDispatch(treeModel, event.getJob(), event.getJobDispatch());
    selectionHandler.unselect(event.getJobDispatch().getUuid());
  }

  @Override
  public void onFilterChange(final TopologyFilterEvent event) {
    JobMonitor monitor = JPPFWebConsoleApplication.get().getJobMonitor();
    CollectionMap<Job, JobDispatch> toRemove = new ArrayListHashMap<>();
    CollectionMap<Job, JobDispatch> toAdd = new ArrayListHashMap<>();
    List<JobDriver> allDrivers = monitor.getJobDrivers();
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
    for (JobDriver driver: allDrivers) {
      DefaultMutableTreeNode driverDmtn = TreeTableUtils.findComponent(root, driver.getUuid());
      if (driverDmtn == null) continue;
      for (Job job: driver.getJobs()) {
        DefaultMutableTreeNode jobDmtn = TreeTableUtils.findComponent(driverDmtn, job.getUuid());
        if (jobDmtn == null) continue;
        for (JobDispatch dispatch: job.getJobDispatches()) {
          boolean accepted = isAccepted(dispatch.getNode());
          DefaultMutableTreeNode dispatchDmtn = TreeTableUtils.findComponent(jobDmtn, dispatch.getUuid());
          boolean present = dispatchDmtn!= null;
          if (accepted && !present) toAdd.putValue(job, dispatch);
          else if (!accepted && present) toRemove.putValue(job, dispatch);
        }
      }
    }
    for (Map.Entry<Job, Collection<JobDispatch>> entry: toRemove.entrySet()) {
      for (JobDispatch dispatch: entry.getValue()) {
        JobsUtils.removeJobDispatch(treeModel, entry.getKey(), dispatch);
        selectionHandler.unselect(dispatch.getUuid());
      }
    }
    for (Map.Entry<Job, Collection<JobDispatch>> entry: toAdd.entrySet()) {
      for (JobDispatch dispatch: entry.getValue()) addDispatch(entry.getKey(), dispatch);
    }
  }

  /**
   * Add a dispatch to the specified job.
   * @param job the job holding the dispatch.
   * @param dispatch the dispatch to add.
   */
  private void addDispatch(final Job job, final JobDispatch dispatch) {
    DefaultMutableTreeNode dispatchDmtn = JobsUtils.addJobDispatch(treeModel, job, dispatch);
    if ((dispatchDmtn != null) && (getTableTree() != null)) {
      DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dispatchDmtn.getParent();
      if (parent.getChildCount() == 1) getTableTree().expand(parent);
    }
  }

  /*
  public void onFilterChange(final TopologyFilterEvent event) {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
    for (int i=0; i<root.getChildCount(); i++) {
      DefaultMutableTreeNode driverDmtn = (DefaultMutableTreeNode) root.getChildAt(i);
      for (int j=0; j<driverDmtn.getChildCount(); j++) {
        DefaultMutableTreeNode jobDmtn = (DefaultMutableTreeNode) driverDmtn.getChildAt(j);
        Job job = (Job) jobDmtn.getUserObject();
        List<JobDispatch> toRemove = new ArrayList<>();
        for (int k=0; k<jobDmtn.getChildCount(); k++) {
          DefaultMutableTreeNode dispatchDmtn = (DefaultMutableTreeNode) jobDmtn.getChildAt(k);
          JobDispatch dispatch = (JobDispatch) dispatchDmtn.getUserObject();
          TopologyNode node = dispatch.getNode();
          if (!isAccepted(node)) toRemove.add(dispatch);
        }
        for (JobDispatch dispatch: toRemove) {
          JobsUtils.removeJobDispatch(treeModel, job, dispatch);
          selectionHandler.unselect(dispatch.getUuid());
        }
      }
    }
  }
  */
}
