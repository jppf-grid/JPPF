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

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.job.JobInformation;
import org.jppf.management.*;
import org.jppf.ui.treetable.JPPFTreeTable;
import org.slf4j.*;

/**
 * This class manages updates to, and navigation within, the tree table
 * for the job data panel.
 * @author Laurent Cohen
 */
class JobDataPanelManager {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JobDataPanelManager.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The job data panel holding this manager.
   */
  private final JobDataPanel panel;
  /**
   *
   */
  private final ConnectionStatusListener listener = new ConnectionStatusListener();
  /**
   *
   */
  private boolean firstDriverAdded = false;

  /**
   * Initialize this job data panel manager.
   * @param panel the job data panel holding this manager.
   */
  public JobDataPanelManager(final JobDataPanel panel) {
    this.panel = panel;
  }

  /**
   * Called to notify that a driver was added.
   * @param driver a reference to the driver.
   */
  public void addDriver(final TopologyDriver driver) {
    JMXDriverConnectionWrapper wrapper = driver.getJmx();
    final int index = driverInsertIndex(driver.getUuid());
    if (index < 0) return;
    JobData data = new JobData(driver);
    try {
      driver.getConnection().addClientConnectionStatusListener(listener);
      data.changeNotificationListener(new JobNotificationListener(panel, driver));
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    final DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(data);
    if (debugEnabled) log.debug("adding driver: " + driver.getUuid() + " at index " + index);
    panel.getModel().insertNodeInto(driverNode, panel.getTreeTableRoot(), index);
    if (!firstDriverAdded) {
      firstDriverAdded = true;
      if (debugEnabled) log.debug("adding first driver: " + driver.getUuid() + " at index " + index);
      Runnable r =  new Runnable() {
        @Override public synchronized void run() {
          try {
            JPPFTreeTable treeTable = null;
            while ((treeTable = panel.getTreeTable()) == null) wait(10L);
            treeTable.expand(panel.getTreeTableRoot());
            treeTable.expand(driverNode);
          } catch (Exception e) {
          }
        }
      };
      new Thread(r, "Job tree expansion").start();
    } else {
      if (debugEnabled) log.debug("additional driver: " + driver.getUuid() + " at index " + index);
      JPPFTreeTable treeTable = panel.getTreeTable();
      if (treeTable != null) {
        treeTable.expand(panel.getTreeTableRoot());
        treeTable.expand(driverNode);
      }
    }
  }

  /**
   * Called to notify that a driver was removed.
   * @param driverUuid the name of the driver to remove.
   */
  public void removeDriver(final String driverUuid) {
    final DefaultMutableTreeNode driverNode = findDriver(driverUuid);
    if (debugEnabled) log.debug("removing driver: " + driverUuid);
    if (driverNode == null) return;
    tearDriverDown(driverNode);
  }

  /**
   * Remove all driver nodes from the tree table.
   */
  public void clearDriver() {
    DefaultMutableTreeNode root = panel.getTreeTableRoot();
    if (debugEnabled) log.debug("removing all drivers");
    int n = root.getChildCount();
    if (n <= 0) return;
    for (int i = n - 1; i >= 0; i--) tearDriverDown((DefaultMutableTreeNode) root.getChildAt(i));
  }

  /**
   * Remmove and cleanup the specified driver.
   * @param driverNode the driver to remove.
   */
  private void tearDriverDown(final DefaultMutableTreeNode driverNode) {
    if (driverNode == null) return;
    try {
      JobData data = (JobData) driverNode.getUserObject();
      data.close();
      data.getClientConnection().removeClientConnectionStatusListener(listener);
      data.changeNotificationListener(null);
    } catch (Exception e) {
      if (debugEnabled) log.debug("while removing: " + e.getMessage(), e);
    }
    panel.getModel().removeNodeFromParent(driverNode);
  }

  /**
   * Called to notify that a job was submitted to a driver.
   * @param driverUuid the name of the driver the job was submitted to.
   * @param jobInfo    information about the submitted job.
   */
  public void addJob(final String driverUuid, final JobInformation jobInfo) {
    final DefaultMutableTreeNode driverNode = findDriver(driverUuid);
    if (driverNode == null) return;
    JobData driverData = (JobData) driverNode.getUserObject();
    JobData data = new JobData(driverData.getDriver(), jobInfo);
    final int index = jobInsertIndex(driverNode, jobInfo);
    if (index < 0) return;
    final DefaultMutableTreeNode jobNode = new DefaultMutableTreeNode(data);
    if (debugEnabled) log.debug("adding job: " + jobInfo + " to driver " + driverUuid + " at index " + index);
    panel.getModel().insertNodeInto(jobNode, driverNode, index);
    if (panel.getTreeTable() != null) panel.getTreeTable().expand(driverNode);
  }

  /**
   * Called to notify that a job was removed from a driver.
   * @param driverUuid the name of the driver the job was submitted to.
   * @param jobUuid    the name of the job.
   */
  public void removeJob(final String driverUuid, final String jobUuid) {
    DefaultMutableTreeNode driverNode = findDriver(driverUuid);
    if (driverNode == null) return;
    final DefaultMutableTreeNode jobNode = findJob(driverNode, jobUuid);
    if (debugEnabled) log.debug("*** jobNode =  " + jobNode);
    if (jobNode == null) return;
    if (debugEnabled) log.debug("removing job: " + jobUuid + " from driver " + driverUuid);
    panel.getModel().removeNodeFromParent(jobNode);
    //if (panel.getTreeTable() != null) panel.getTreeTable().repaint();
  }

  /**
   * Called to notify that a job was removed from a driver.
   * @param driverUuid the name of the driver the job was submitted to.
   * @param jobInfo    information about the job.
   */
  public void updateJob(final String driverUuid, final JobInformation jobInfo) {
    DefaultMutableTreeNode driverNode = findDriver(driverUuid);
    if (driverNode == null) return;
    final DefaultMutableTreeNode jobNode = findJob(driverNode, jobInfo);
    JobData driverData = (JobData) driverNode.getUserObject();
    if (jobNode == null) return;
    if (debugEnabled) log.debug("updating job: " + jobInfo.getJobName() + " from driver " + driverUuid);
    JobData data = new JobData(driverData.getDriver(), jobInfo);
    jobNode.setUserObject(data);
    panel.getModel().changeNode(jobNode);
  }

  /**
   * Called to notify that a sub-job was dispatched to a node.
   * @param driverUuid the name of the driver the job was submitted to.
   * @param jobInfo    information about the sub-job.
   * @param nodeInfo   information about the node where the sub-job was dispatched.
   */
  public void addJobDispatch(final String driverUuid, final JobInformation jobInfo, final JPPFManagementInfo nodeInfo) {
    DefaultMutableTreeNode driverNode = findDriver(driverUuid);
    if (driverNode == null) return;
    final DefaultMutableTreeNode jobNode = findJob(driverNode, jobInfo);
    if (jobNode == null) return;
    JobData data = new JobData(jobInfo, nodeInfo);
    final int index = jobDispatchInsertIndex(jobNode, nodeInfo);
    if (index < 0) return;
    final DefaultMutableTreeNode subJobNode = new DefaultMutableTreeNode(data);
    if (debugEnabled) log.debug("sub-job: {} dispatched to node {}:{} (index {})", new Object[] {jobInfo.getJobName(), nodeInfo.getHost(), nodeInfo.getPort(), index});
    panel.getModel().insertNodeInto(subJobNode, jobNode, index);
    if (panel.getTreeTable() != null) panel.getTreeTable().expand(jobNode);
  }

  /**
   * Called to notify that a sub-job was removed from a node.
   * @param driverUuid the name of the driver the job was submitted to.
   * @param jobUuid    information about the job.
   * @param nodeUuid   information about the node where the sub-job was dispatched.
   */
  public void removeJobDispatch(final String driverUuid, final String jobUuid, final String nodeUuid) {
    DefaultMutableTreeNode driverNode = findDriver(driverUuid);
    if (driverNode == null) return;
    DefaultMutableTreeNode jobNode = findJob(driverNode, jobUuid);
    if (jobNode == null) return;
    final DefaultMutableTreeNode subJobNode = findJobDispatch(jobNode, nodeUuid);
    if (subJobNode == null) return;
    if (debugEnabled) log.debug("removing sub-job: " + jobUuid + " from node " + nodeUuid);
    panel.getModel().removeNodeFromParent(subJobNode);
    if (panel.getTreeTable() != null) panel.getTreeTable().repaint();
  }

  /**
   * Find the driver tree node with the specified driver name.
   * @param driverUuid name of the driver to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  DefaultMutableTreeNode findDriver(final String driverUuid) {
    for (int i = 0; i < panel.getTreeTableRoot().getChildCount(); i++) {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) panel.getTreeTableRoot().getChildAt(i);
      JobData data = (JobData) driverNode.getUserObject();
      String name = data.getClientConnection().getDriverUuid();
      if (name.equals(driverUuid)) return driverNode;
    }
    return null;
  }

  /**
   * Find the job with the specified id that was submitted to the specified driver.
   * @param driverNode the driver where the job was submitted.
   * @param jobInfo    information about the job to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the job could not be found.
   */
  DefaultMutableTreeNode findJob(final DefaultMutableTreeNode driverNode, final JobInformation jobInfo) {
    return findJob(driverNode, jobInfo.getJobUuid());
  }

  /**
   * Find the job with the specified id that was submitted to the specified driver.
   * @param driverNode the driver where the job was submitted.
   * @param jobUuid    the name of the job to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the job could not be found.
   */
  DefaultMutableTreeNode findJob(final DefaultMutableTreeNode driverNode, final String jobUuid) {
    for (int i = 0; i < driverNode.getChildCount(); i++) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
      JobData data = (JobData) node.getUserObject();
      if (data.getJobInformation().getJobUuid().equals(jobUuid)) return node;
    }
    return null;
  }

  /**
   * Find the sub-job with the specified id that was dispatched to the specified JPPF node.
   * @param jobNode  the job whose sub-job we are looking for.
   * @param nodeUuid the name of the node to which the sub-job was dispatched.
   * @return a <code>DefaultMutableTreeNode</code> or null if the sub-job could not be found.
   */
  DefaultMutableTreeNode findJobDispatch(final DefaultMutableTreeNode jobNode, final String nodeUuid) {
    if (nodeUuid == null) return null;
    for (int i = 0; i < jobNode.getChildCount(); i++) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) jobNode.getChildAt(i);
      JobData data = (JobData) node.getUserObject();
      if ((data == null) || (data.getNodeInformation() == null)) return null;
      if (data.getNodeInformation().toString().equals(nodeUuid)) return node;
    }
    return null;
  }

  /**
   * Find the position at which to insert a driver,
   * using the sorted lexical order of driver names.
   * @param driverUuid the name of the driver to insert.
   * @return the index at which to insert the driver, or -1 if the driver is already in the tree.
   */
  int driverInsertIndex(final String driverUuid) {
    DefaultMutableTreeNode root = panel.getTreeTableRoot();
    int n = root.getChildCount();
    for (int i = 0; i < n; i++) {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) root.getChildAt(i);
      JobData data = (JobData) driverNode.getUserObject();
      String name = data.getClientConnection().getDriverUuid();
      if (name.equals(driverUuid)) return -1;
      else if (driverUuid.compareTo(name) < 0) return i;
    }
    return n;
  }

  /**
   * Find the position at which to insert a job, using the sorted lexical order of job names.
   * @param driverNode name the parent tree node of the job to insert.
   * @param jobInfo    information about the job to insert.
   * @return the index at which to insert the job, or -1 if the job is already in the tree.
   */
  int jobInsertIndex(final DefaultMutableTreeNode driverNode, final JobInformation jobInfo) {
    int n = driverNode.getChildCount();
    String jobUuid = jobInfo.getJobUuid();
    for (int i = 0; i < n; i++) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
      JobData jobData = (JobData) node.getUserObject();
      String name = jobData.getJobInformation().getJobUuid();
      if (jobUuid.equals(name)) return -1;
      else if (jobUuid.compareTo(name) < 0) return i;
    }
    return n;
  }

  /**
   * Find the position at which to insert a subjob, using the sorted lexical order of subjob names.
   * @param jobNode  the parent tree node of the subjob to insert.
   * @param nodeInfo information about the subjob to insert.
   * @return the index at which to insert the subjob, or -1 if the subjob is already in the tree.
   */
  int jobDispatchInsertIndex(final DefaultMutableTreeNode jobNode, final JPPFManagementInfo nodeInfo) {
    int n = jobNode.getChildCount();
    String subJobName = nodeInfo.toString();
    for (int i = 0; i < n; i++) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) jobNode.getChildAt(i);
      JobData jobData = (JobData) node.getUserObject();
      if ((jobData == null) || (jobData.getNodeInformation() == null)) continue;
      String name = jobData.getNodeInformation().toString();
      if (subJobName.equals(name))  return -1;
      else if (subJobName.compareTo(name) < 0) return i;
    }
    return n;
  }

  /**
   * Listens to JPPF client connection status changes for rendering purposes.
   */
  public class ConnectionStatusListener implements ClientConnectionStatusListener {
    @Override
    public void statusChanged(final ClientConnectionStatusEvent event) {
      if (event.getSource() instanceof JPPFClientConnection) {
        JPPFClientConnection c = (JPPFClientConnection) event.getSource();
        String uuid = c.getDriverUuid();
        TopologyDriver driver = panel.getTopologyManager().getDriver(uuid);
        JPPFClientConnectionStatus status = event.getClientConnectionStatusHandler().getStatus();
        if (status.isTerminatedStatus()) panel.driverRemoved(driver);
        else panel.updateDriver(driver);
      } else throw new IllegalStateException("Unsupported event source - expected JPPFClientConnection");
    }
  }
}
