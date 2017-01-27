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

package org.jppf.ui.utils;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.monitoring.jobs.*;
import org.jppf.ui.treetable.*;
import org.slf4j.*;

/**
 * Utility methods for manipulating the jobs tree model.
 * @author Laurent Cohen
 */
public class JobsUtils {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JobsUtils.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Called to notify that a driver was added.
   * @param model the tree table model.
   * @param driver a reference to the driver.
   * @return the newly created {@link DefaultMutableTreeNode}, if any.
   */
  public static DefaultMutableTreeNode addDriver(final AbstractJPPFTreeTableModel model, final JobDriver driver) {
    DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    final int index = TreeTableUtils.insertIndex(treeTableRoot, driver);
    if (index < 0) return null;
    final DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(driver);
    if (debugEnabled) log.debug("adding driver: " + driver.getDisplayName() + " at index " + index);
    model.insertNodeInto(driverNode, treeTableRoot, index);
    return driverNode;
  }

  /**
   * Called to notify that a driver was removed.
   * @param model the tree table model.
   * @param driver the name of the driver to remove.
   */
  public static void removeDriver(final AbstractJPPFTreeTableModel model, final JobDriver driver) {
    DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    final DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, driver.getUuid());
    if (debugEnabled) log.debug("removing driver: " + driver.getDisplayName());
    if (driverNode == null) return;
    model.removeNodeFromParent(driverNode);
  }

  /**
   * Called to notify that a job was submitted to a driver.
   * @param model the tree table model.
   * @param driver the driver the job was submitted to.
   * @param job information about the submitted job.
   * @return the parent of the newly created job {@link DefaultMutableTreeNode}, if any.
   */
  public static DefaultMutableTreeNode addJob(final AbstractJPPFTreeTableModel model, final JobDriver driver, final Job job) {
    DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    final DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, driver.getUuid());
    if (driverNode == null) return null;
    final int index = TreeTableUtils.insertIndex(driverNode, job);
    if (index < 0) return null;
    final DefaultMutableTreeNode jobNode = new DefaultMutableTreeNode(job);
    if (debugEnabled) log.debug("adding job: " + job.getDisplayName() + " to driver " + driver.getDisplayName() + " at index " + index);
    model.insertNodeInto(jobNode, driverNode, index);
    return driverNode;
  }

  /**
   * Called to notify that a job was removed from a driver.
   * @param model the tree table model.
   * @param driver the the driver the job was submitted to.
   * @param job the job.
   */
  public static void removeJob(final AbstractJPPFTreeTableModel model, final JobDriver driver, final Job job) {
    DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, driver.getUuid());
    if (driverNode == null) return;
    final DefaultMutableTreeNode jobNode = TreeTableUtils.findComponent(driverNode, job.getUuid());
    if (jobNode == null) return;
    if (debugEnabled) log.debug("removing job: " + job.getDisplayName() + " from driver " + driver.getDisplayName());
    model.removeNodeFromParent(jobNode);
  }

  /**
   * Called to notify that a job was removed from a driver.
   * @param model the tree table model.
   * @param job information about the job.
   */
  public static void updateJob(final AbstractJPPFTreeTableModel model, final Job job) {
    DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, job.getJobDriver().getUuid());
    if (driverNode == null) return;
    final DefaultMutableTreeNode jobNode = TreeTableUtils.findComponent(driverNode, job.getUuid());
    if (jobNode == null) return;
    if (debugEnabled) log.debug("updating job: " + job.getDisplayName() + " from driver " + job.getJobDriver().getDisplayName());
    model.changeNode(jobNode);
  }

  /**
   * Called to notify that a sub-job was dispatched to a node.
   * @param model the tree table model.
   * @param job information about the job.
   * @param dispatch information about the job dispatch.
   * @return the parent of the newly created job dispatch {@link DefaultMutableTreeNode}, if any.
   */
  public static DefaultMutableTreeNode addJobDispatch(final AbstractJPPFTreeTableModel model, final Job job, final JobDispatch dispatch) {
    DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, job.getJobDriver().getUuid());
    if (driverNode == null) return null;
    final DefaultMutableTreeNode jobNode = TreeTableUtils.findComponent(driverNode, job.getUuid());
    if (jobNode == null) return null;
    final int index = TreeTableUtils.insertIndex(jobNode, dispatch);
    if (index < 0) return null;
    final DefaultMutableTreeNode subJobNode = new DefaultMutableTreeNode(dispatch);
    if (debugEnabled) log.debug("sub-job: {} dispatched to node {} (index {})", new Object[] { job.getDisplayName(), dispatch.getDisplayName(), index});
    model.insertNodeInto(subJobNode, jobNode, index);
    return jobNode;
  }

  /**
   * Called to notify that a sub-job was removed from a node.
   * @param model the tree table model.
   * @param job information about the job.
   * @param dispatch information about the node where the sub-job was dispatched.
   */
  public static void removeJobDispatch(final AbstractJPPFTreeTableModel model, final Job job, final JobDispatch dispatch) {
    if (dispatch == null) return;
    DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, job.getJobDriver().getUuid());
    if (driverNode == null) return;
    DefaultMutableTreeNode jobNode = TreeTableUtils.findComponent(driverNode, job.getUuid());
    if (jobNode == null) return;
    final DefaultMutableTreeNode subJobNode = TreeTableUtils.findComponent(jobNode, dispatch.getUuid());
    if (subJobNode == null) return;
    if (debugEnabled) log.debug("removing dispatch: " + job.getDisplayName() + " from node " + dispatch.getDisplayName());
    model.removeNodeFromParent(subJobNode);
  }
}
