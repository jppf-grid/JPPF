/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import org.jppf.client.monitoring.AbstractComponent;
import org.jppf.client.monitoring.jobs.*;
import org.jppf.client.monitoring.topology.TopologyManager;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.event.*;
import org.jppf.ui.monitoring.job.actions.*;
import org.jppf.ui.treetable.*;
import org.jppf.ui.utils.TreeTableUtils;
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
   * The topology manager.
   */
  private final TopologyManager topologyManager;
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
    if (debugEnabled) log.debug("initializing NodeDataPanel");
    this.topologyManager = StatsHandler.getInstance().getTopologyManager();
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
    setUIComponent(sp);
    treeTable.expandAll();
    StatsHandler.getInstance().addShowIPListener(new ShowIPListener() {
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
    final int index = insertIndex(treeTableRoot, driver);
    if (index < 0) return;
    final DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(driver);
    if (debugEnabled) log.debug("adding driver: " + driver.getDisplayName() + " at index " + index);
    getModel().insertNodeInto(driverNode, getTreeTableRoot(), index);
    if (!firstDriverAdded) {
      firstDriverAdded = true;
      if (debugEnabled) log.debug("adding first driver: " + driver.getDisplayName() + " at index " + index);
      Runnable r =  new Runnable() {
        @Override public synchronized void run() {
          try {
            JPPFTreeTable treeTable = null;
            while ((treeTable = getTreeTable()) == null) wait(10L);
            treeTable.expand(getTreeTableRoot());
            treeTable.expand(driverNode);
          } catch (Exception e) {
          }
        }
      };
      new Thread(r, "Job tree expansion").start();
    } else {
      if (debugEnabled) log.debug("additional driver: " + driver.getDisplayName() + " at index " + index);
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
    final DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, driver.getUuid());
    if (debugEnabled) log.debug("removing driver: " + driver.getDisplayName());
    if (driverNode == null) return;
    getModel().removeNodeFromParent(driverNode);
  }

  /**
   * Called to notify that a job was submitted to a driver.
   * @param driver the driver the job was submitted to.
   * @param job information about the submitted job.
   */
  public void addJob(final JobDriver driver, final Job job) {
    final DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, driver.getUuid());
    if (driverNode == null) return;
    final int index = insertIndex(driverNode, job);
    if (index < 0) return;
    final DefaultMutableTreeNode jobNode = new DefaultMutableTreeNode(job);
    if (debugEnabled) log.debug("adding job: " + job.getDisplayName() + " to driver " + driver.getDisplayName() + " at index " + index);
    getModel().insertNodeInto(jobNode, driverNode, index);
    if (getTreeTable() != null) getTreeTable().expand(driverNode);
  }

  /**
   * Called to notify that a job was removed from a driver.
   * @param driver the the driver the job was submitted to.
   * @param job the job.
   */
  public void removeJob(final JobDriver driver, final Job job) {
    DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, driver.getUuid());
    if (driverNode == null) return;
    final DefaultMutableTreeNode jobNode = TreeTableUtils.findComponent(driverNode, job.getUuid());
    //if (debugEnabled) log.debug("*** jobNode =  " + jobNode);
    if (jobNode == null) return;
    if (debugEnabled) log.debug("removing job: " + job.getDisplayName() + " from driver " + driver.getDisplayName());
    getModel().removeNodeFromParent(jobNode);
    //if (getTreeTable() != null) getTreeTable().repaint();
  }

  /**
   * Called to notify that a job was removed from a driver.
   * @param job information about the job.
   */
  public void updateJob(final Job job) {
    DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, job.getJobDriver().getUuid());
    if (driverNode == null) return;
    final DefaultMutableTreeNode jobNode = TreeTableUtils.findComponent(driverNode, job.getUuid());
    if (jobNode == null) return;
    if (debugEnabled) log.debug("updating job: " + job.getDisplayName() + " from driver " + job.getJobDriver().getDisplayName());
    getModel().changeNode(jobNode);
  }

  /**
   * Called to notify that a sub-job was dispatched to a node.
   * @param job information about the job.
   * @param dispatch information about the job dispatch.
   */
  public void addJobDispatch(final Job job, final JobDispatch dispatch) {
    DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, job.getJobDriver().getUuid());
    if (driverNode == null) return;
    final DefaultMutableTreeNode jobNode = TreeTableUtils.findComponent(driverNode, job.getUuid());
    if (jobNode == null) return;
    final int index = insertIndex(jobNode, dispatch);
    if (index < 0) return;
    final DefaultMutableTreeNode subJobNode = new DefaultMutableTreeNode(dispatch);
    if (debugEnabled) log.debug("sub-job: {} dispatched to node {} (index {})", new Object[] { job.getDisplayName(), dispatch.getDisplayName(), index});
    getModel().insertNodeInto(subJobNode, jobNode, index);
    if (getTreeTable() != null) getTreeTable().expand(jobNode);
  }

  /**
   * Called to notify that a sub-job was removed from a node.
   * @param job information about the job.
   * @param dispatch information about the node where the sub-job was dispatched.
   */
  public void removeJobDispatch(final Job job, final JobDispatch dispatch) {
    if (dispatch == null) return;
    DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, job.getJobDriver().getUuid());
    if (driverNode == null) return;
    DefaultMutableTreeNode jobNode = TreeTableUtils.findComponent(driverNode, job.getUuid());
    if (jobNode == null) return;
    final DefaultMutableTreeNode subJobNode = TreeTableUtils.findComponent(jobNode, dispatch.getUuid());
    if (subJobNode == null) return;
    if (debugEnabled) log.debug("removing dispatch: " + job.getDisplayName() + " from node " + dispatch.getDisplayName());
    getModel().removeNodeFromParent(subJobNode);
    if (getTreeTable() != null) getTreeTable().repaint();
  }

  /**
   * Find the position at which to insert a driver, using the sorted lexical order of driver display names.
   * @param root the parent tree node of the component to insert.
   * @param comp the driver to insert.
   * @return the index at which to insert the driver, or -1 if the driver is already in the tree.
   */
  int insertIndex(final DefaultMutableTreeNode root, final AbstractComponent comp) {
    if (TreeTableUtils.findComponent(root, comp.getUuid()) != null) return -1;
    return TreeTableUtils.insertIndex(root, comp);
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
