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

import org.jppf.admin.web.tabletree.*;
import org.jppf.client.monitoring.jobs.*;
import org.jppf.ui.treetable.AbstractJPPFTreeTableModel;
import org.jppf.ui.utils.JobsUtils;

/**
 * Listens to job events so as to update the jobs view.
 * @author Laurent Cohen
 */
public class JobsTreeListener extends AbstractMonitoringListener implements JobMonitoringListener {
  /**
   * Initialize with the specified tree model and selection handler.
   * @param treeModel the tree table model.
   * @param selectionHandler handles the selection of rows in the tree table.
   */
  public JobsTreeListener(final AbstractJPPFTreeTableModel treeModel, final SelectionHandler selectionHandler) {
    super(treeModel, selectionHandler);
  }

  @Override
  public void driverAdded(final JobMonitoringEvent event) {
    JobsUtils.addDriver(treeModel, event.getJobDriver());
  }

  @Override
  public void driverRemoved(final JobMonitoringEvent event) {
    JobsUtils.removeDriver(treeModel, event.getJobDriver());
    selectionHandler.unselect(event.getJobDriver().getUuid());
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
    DefaultMutableTreeNode dispatchNode = JobsUtils.addJobDispatch(treeModel, event.getJob(), event.getJobDispatch());
    if ((dispatchNode != null) && (getTableTree() != null)) {
      DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dispatchNode.getParent();
      if (parent.getChildCount() == 1) getTableTree().expand(parent);
    }
  }

  @Override
  public void jobDispatchRemoved(final JobMonitoringEvent event) {
    JobsUtils.removeJobDispatch(treeModel, event.getJob(), event.getJobDispatch());
    selectionHandler.unselect(event.getJobDispatch().getUuid());
  }
}
