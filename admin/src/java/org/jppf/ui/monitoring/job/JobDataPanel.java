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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.monitoring.jobs.*;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.event.*;
import org.jppf.ui.monitoring.job.actions.*;
import org.jppf.ui.treetable.*;
import org.jppf.ui.utils.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
 * @author Martin Janda
 */
public class JobDataPanel extends AbstractTreeTableOption implements JobMonitoringListener {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JobDataPanel.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The object which monitors and maintains a representation of the jobs hierarchy.
   */
  private final JobMonitor jobMonitor;
 /**
  * Determines whether at least one driver was added.
  */
 private boolean firstDriverAdded = false;
 /**
  * Determines whether refreshes are currently suspended.
  */
 private AtomicBoolean suspended = new AtomicBoolean(false);

  /**
   * Initialize this panel with the specified information.
   */
  public JobDataPanel() {
    BASE = "org.jppf.ui.i18n.JobDataPage";
    if (debugEnabled) log.debug("initializing JobDataPanel");
    this.jobMonitor = StatsHandler.getInstance().getJobMonitor();
    createTreeTableModel();
    jobMonitor.addJobMonitoringListener(this);
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private void createTreeTableModel() {
    treeTableRoot = new DefaultMutableTreeNode(localize("job.tree.root.name"));
    model = new JobTreeTableModel(treeTableRoot);
    populateTreeTableModel();
  }

  /**
   * Create, initialize and layout the GUI components displayed in this 
   */
  @Override
  public void createUI() {
    treeTable = new JPPFTreeTable(model);
    treeTable.getTree().setLargeModel(true);
    treeTable.getTree().setRootVisible(false);
    treeTable.getTree().setShowsRootHandles(true);
    treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
    treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    treeTable.doLayout();
    treeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    treeTable.getTree().setCellRenderer(new JobRenderer());
    treeTable.setDefaultRenderer(Object.class, new JobTableCellRenderer(this));
    JScrollPane sp = new JScrollPane(treeTable);
    GuiUtils.adjustScrollbarsThickness(sp);
    setUIComponent(sp);
    treeTable.expandAll();
    StatsHandler.getInstance().getShowIPHandler().addShowIPListener(new ShowIPListener() {
      @Override
      public void stateChanged(final ShowIPEvent event) {
        treeTable.repaint();
      }
    });
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private synchronized void populateTreeTableModel() {
    if (debugEnabled) log.debug("populating the tree table");
    assert SwingUtilities.isEventDispatchThread() : "Not on event dispatch thread";
    for (JobDriver driver: jobMonitor.getJobDrivers()) {
      addDriver(driver);
      for (Job job: driver.getJobs()) {
        addJob(driver, job);
        for (JobDispatch dispatch: job.getJobDispatches()) addJobDispatch(job, dispatch);
      }
    }
  }

  /**
   * Remove all driver nodes from the tree table.
   */
  private void clearDrivers() {
    DefaultMutableTreeNode root = getTreeTableRoot();
    if (debugEnabled) log.debug("removing all drivers");
    int n = root.getChildCount();
    if (n <= 0) return;
    for (int i=n-1; i>=0; i--) getModel().removeNodeFromParent((DefaultMutableTreeNode) root.getChildAt(i));
  }

  /**
   * Refresh the entire tree table.
   */
  public void refresh() {
    clearDrivers();
    populateTreeTableModel();
  }

  /**
   * Called to notify that a driver was added.
   * @param driver a reference to the driver.
   */
  public void addDriver(final JobDriver driver) {
    final DefaultMutableTreeNode driverNode = JobsUtils.addDriver(getModel(), driver);
    if (driverNode == null) return;
    if (!firstDriverAdded) {
      firstDriverAdded = true;
      if (debugEnabled) log.debug("adding first driver: {}", driver.getDisplayName());
      Runnable r =  new Runnable() {
        @Override public synchronized void run() {
          try {
            JPPFTreeTable treeTable = null;
            while ((treeTable = getTreeTable()) == null) wait(10L);
            treeTable.expand(getTreeTableRoot());
            treeTable.expand(driverNode);
          } catch (Exception e) {
            log.debug(e.getMessage(), e);
          }
        }
      };
      new Thread(r, "Job tree expansion").start();
    } else {
      if (debugEnabled) log.debug("additional driver: {}", driver.getDisplayName());
      JPPFTreeTable treeTable = getTreeTable();
      if (treeTable != null) {
        treeTable.expand(getTreeTableRoot());
        treeTable.expand(driverNode);
      }
    }
  }

  /**
   * Called to notify that a driver was removed.
   * @param driver the name of the driver to remove.
   */
  public void removeDriver(final JobDriver driver) {
    JobsUtils.removeDriver(getModel(), driver);
  }

  /**
   * Called to notify that a job was submitted to a driver.
   * @param driver the driver the job was submitted to.
   * @param job information about the submitted job.
   */
  public void addJob(final JobDriver driver, final Job job) {
    final DefaultMutableTreeNode driverNode = JobsUtils.addJob(getModel(), driver, job);
    if ((getTreeTable() != null) && (driverNode != null)) getTreeTable().expand(driverNode);
  }

  /**
   * Called to notify that a job was removed from a driver.
   * @param driver the the driver the job was submitted to.
   * @param job the job.
   */
  public void removeJob(final JobDriver driver, final Job job) {
    JobsUtils.removeJob(getModel(), driver, job);
  }

  /**
   * Called to notify that a job was removed from a driver.
   * @param job information about the job.
   */
  public void updateJob(final Job job) {
    JobsUtils.updateJob(getModel(), job);
  }

  /**
   * Called to notify that a sub-job was dispatched to a node.
   * @param job information about the job.
   * @param dispatch information about the job dispatch.
   */
  public void addJobDispatch(final Job job, final JobDispatch dispatch) {
    final DefaultMutableTreeNode jobNode = JobsUtils.addJobDispatch(getModel(), job, dispatch);
    if ((getTreeTable() != null) && (jobNode != null)) getTreeTable().expand(jobNode);
  }

  /**
   * Called to notify that a sub-job was removed from a node.
   * @param job information about the job.
   * @param dispatch information about the node where the sub-job was dispatched.
   */
  public void removeJobDispatch(final Job job, final JobDispatch dispatch) {
    JobsUtils.removeJobDispatch(getModel(), job, dispatch);
    if (getTreeTable() != null) getTreeTable().repaint();
  }

  /**
   * Refreshes the tree table display.
   */
  public void refreshUI() {
    treeTable.repaint();
  }

  /**
   * Initialize all actions used in the toolbar and popup menus.
   */
  public void setupActions() {
    actionHandler = new JTreeTableActionHandler(treeTable);
    synchronized(actionHandler) {
      actionHandler.putAction("cancel.job", new CancelJobAction());
      actionHandler.putAction("suspend.job", new SuspendJobAction());
      actionHandler.putAction("suspend_requeue.job", new SuspendRequeueJobAction());
      actionHandler.putAction("resume.job", new ResumeJobAction());
      actionHandler.putAction("max.nodes.job", new UpdateMaxNodesAction());
      actionHandler.putAction("update.priority.job", new UpdatePriorityAction());
      actionHandler.putAction("job.select.jobs", new SelectJobsAction(this));
      actionHandler.putAction("job.show.hide", new ShowHideColumnsAction(this));
      actionHandler.updateActions();
    }
    treeTable.addMouseListener(new JobTreeTableMouseListener(actionHandler));
    Runnable r = new ActionsInitializer(this, "/job.toolbar");
    new Thread(r).start();
  }

  @Override
  public void driverAdded(final JobMonitoringEvent event) {
    addDriver(event.getJobDriver());
  }

  @Override
  public void driverRemoved(final JobMonitoringEvent event) {
    if (!suspended.get()) removeDriver(event.getJobDriver());
  }

  @Override
  public void jobAdded(final JobMonitoringEvent event) {
    if (!suspended.get()) addJob(event.getJobDriver(), event.getJob());
  }

  @Override
  public void jobRemoved(final JobMonitoringEvent event) {
    if (!suspended.get()) removeJob(event.getJobDriver(), event.getJob());
  }

  @Override
  public void jobUpdated(final JobMonitoringEvent event) {
    if (!suspended.get()) updateJob(event.getJob());
  }

  @Override
  public void jobDispatchAdded(final JobMonitoringEvent event) {
    if (!suspended.get()) addJobDispatch(event.getJob(), event.getJobDispatch());
  }

  @Override
  public void jobDispatchRemoved(final JobMonitoringEvent event) {
    if (!suspended.get()) removeJobDispatch(event.getJob(), event.getJobDispatch());
  }

  /**
   * Specify whether refreshes are currently suspended.
   * @param suspended {@code true} to suspend refreshes, {@code false} to resume them.
   * @since 5.1
   */
  public void setSuspended(final boolean suspended) {
    if (suspended == this.suspended.get()) return;
    if (!suspended) refresh();
    this.suspended.set(suspended);
  }
}
