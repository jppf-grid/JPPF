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

import java.util.Map;
import java.util.concurrent.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.client.monitoring.topology.*;
import org.jppf.job.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.server.job.management.*;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.event.*;
import org.jppf.ui.monitoring.job.AccumulatorHelper.AccumulatorDriver;
import org.jppf.ui.monitoring.job.AccumulatorHelper.AccumulatorJob;
import org.jppf.ui.monitoring.job.AccumulatorHelper.AccumulatorNode;
import org.jppf.ui.monitoring.job.actions.*;
import org.jppf.ui.treetable.*;
import org.jppf.utils.JPPFThreadFactory;
import org.slf4j.*;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
 * @author Martin Janda
 */
public class JobDataPanel extends AbstractTreeTableOption implements TopologyListener {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JobDataPanel.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The object that manages updates to and navigation within the tree table.
   */
  JobDataPanelManager panelManager = null;
  /**
   * Accumulator for driver and job state change notifications.
   */
  private final AccumulatorHelper accumulatorHelper;
  /**
   * Handles the notifications received from the drivers.
   */
  private final ExecutorService notificationsExecutor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("JobNotifications"));
  /**
   * The topology manager.
   */
  private final TopologyManager topologyManager;

  /**
   * Initialize this panel with the specified information.
   */
  public JobDataPanel() {
    BASE = "org.jppf.ui.i18n.JobDataPage";
    if (debugEnabled) log.debug("initializing NodeDataPanel");
    panelManager = new JobDataPanelManager(this);
    accumulatorHelper = new AccumulatorHelper(this);
    createTreeTableModel();
    //StatsHandler.getInstance().getClientHandler().getJppfClient(this);
    this.topologyManager = StatsHandler.getInstance().getTopologyManager();
    populateTreeTableModel();
    topologyManager.addTopologyListener(this);
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private void createTreeTableModel() {
    treeTableRoot = new DefaultMutableTreeNode(localize("job.tree.root.name"));
    model = new JobTreeTableModel(treeTableRoot);
    treeTable = new JPPFTreeTable(model);
    treeTable.expand(treeTableRoot);
    StatsHandler.getInstance().addShowIPListener(new ShowIPListener() {
      @Override
      public void stateChanged(final ShowIPEvent event) {
        treeTable.repaint();
      }
    });
  }

  /**
   * Refresh the tree from the latest data found in the server.
   * This method will clear the entire tree and repopulate it.
   */
  public synchronized void refresh() {
    SwingUtilities.invokeLater(new RefreshTask());
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private synchronized void populateTreeTableModel() {
    if (debugEnabled) log.debug("populating the tree table");
    assert SwingUtilities.isEventDispatchThread() : "Not on event dispatch thread";
    for (TopologyDriver driver: topologyManager.getDrivers()) {
      panelManager.addDriver(driver);
      if (debugEnabled) log.debug("added driver " + driver);
      if (driver.getJmx() == null) continue;
      String driverUuid = driver.getUuid();
      DefaultMutableTreeNode driverNode = panelManager.findDriver(driverUuid);
      if (driverNode == null) continue;
      JobData driverData = (JobData) driverNode.getUserObject();
      DriverJobManagementMBean proxy = driverData.getProxy();
      if (proxy == null) continue;
      String[] jobIds;
      try {
        jobIds = proxy.getAllJobIds();
      } catch (Exception ex) {
        if (debugEnabled) log.debug("populating model: " + ex.getMessage(), ex);
        continue;
      }
      for (String id : jobIds) {
        JobInformation jobInfo = null;
        try {
          jobInfo = proxy.getJobInformation(id);
        } catch (Exception e) {
          if (debugEnabled) log.debug("populating model: " + e.getMessage(), e);
        }
        if (jobInfo == null) continue;
        panelManager.addJob(driverUuid, jobInfo);
        try {
          NodeJobInformation[] subJobInfo = proxy.getNodeInformation(id);
          for (NodeJobInformation nji : subJobInfo) panelManager.addJobDispatch(driverUuid, nji.jobInfo, nji.nodeInfo);
        } catch (Exception e) {
          if (debugEnabled) log.debug("populating model: " + e.getMessage(), e);
        }
      }
    }
  }

  /**
   * Create, initialize and layout the GUI components displayed in this panel.
   */
  @Override
  public void createUI() {
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
    treeTable.setVisible(true);
    treeTable.expandAll();
  }

  /**
   * Called to notify that a driver was added.
   * @param driverData a reference to the driver connection.
   */
  public void driverAdded(final TopologyDriver driverData) {
    if (driverData == null) throw new IllegalArgumentException("clientConnection is null");
    String driverUuid = driverData.getUuid();
    if (debugEnabled) log.debug("adding driver " + driverData + ", uuid=" + driverUuid);
    synchronized(accumulatorHelper) {
      AccumulatorDriver driver = accumulatorHelper.accumulatorMap.get(driverUuid);
      if (driver == null) {
        driver = new AccumulatorDriver(JobAccumulator.Type.ADD, driverData);
        accumulatorHelper.accumulatorMap.put(driverUuid, driver);
      } else {
        boolean remove = driver.mergeChange(JobAccumulator.Type.ADD);
        if (remove) accumulatorHelper.accumulatorMap.remove(driverUuid);
      }
    }
  }

  /**
   * Called to notify that a driver was removed.
   * @param driverData a reference to the driver connection to remove.
   */
  public void driverRemoved(final TopologyDriver driverData) {
    //if (driverData == null) throw new IllegalArgumentException("clientConnection is null");
    if (driverData == null) return;
    String driverUuid = driverData.getUuid();
    if (debugEnabled) log.debug("removing driver " + driverData + ", uuid=" + driverUuid);
    synchronized(accumulatorHelper) {
      AccumulatorDriver driver = accumulatorHelper.accumulatorMap.get(driverUuid);
      if (driver == null) accumulatorHelper.accumulatorMap.put(driverUuid, new AccumulatorDriver(JobAccumulator.Type.REMOVE, driverData));
      else {
        boolean remove = driver.mergeChange(JobAccumulator.Type.REMOVE);
        if (remove) accumulatorHelper.accumulatorMap.remove(driverUuid);
      }
    }
  }

  /**
   * Called to notify that a driver was updated.
   * @param driverData a reference to the driver connection that changed.
   */
  public void updateDriver(final TopologyDriver driverData) {
    if (driverData == null) throw new IllegalArgumentException("clientConnection is null");
    String uuid = driverData.getUuid();
    if (debugEnabled) log.debug("updating driver " + driverData + ", uuid=" + uuid);
    synchronized(accumulatorHelper) {
      AccumulatorDriver driver = accumulatorHelper.accumulatorMap.get(uuid);
      if (driver == null) {
        driver = new AccumulatorDriver(JobAccumulator.Type.UPDATE, driverData);
        accumulatorHelper.accumulatorMap.put(uuid, driver);
      } else {
        boolean remove = driver.mergeChange(JobAccumulator.Type.UPDATE, driverData);
        if (remove) accumulatorHelper.accumulatorMap.remove(uuid);
      }
    }
  }

  /**
   * Called to notify that a job was submitted to a driver.
   * @param driverUuid the name of the driver the job was submitted to.
   * @param jobInfo    information about the submitted job.
   */
  public void addJob(final String driverUuid, final JobInformation jobInfo) {
    if (jobInfo == null) throw new IllegalArgumentException("jobInfo is null");
    String jobUuid = jobInfo.getJobUuid();
    if (debugEnabled) log.debug("adding job " + jobInfo + " to driver " + driverUuid);
    synchronized(accumulatorHelper) {
      AccumulatorDriver driver = accumulatorHelper.getAccumulatedDriver(driverUuid);
      Map<String, AccumulatorJob> jobMap = driver.getMap();
      AccumulatorJob job = jobMap.get(jobUuid);
      if (job == null) {
        job = new AccumulatorJob(JobAccumulator.Type.ADD, jobInfo);
        jobMap.put(jobUuid, job);
      } else {
        boolean remove = job.mergeChange(JobAccumulator.Type.ADD);
        if (remove) jobMap.remove(jobUuid);
      }
    }
  }

  /**
   * Called to notify that a job was removed from a driver.
   * @param driverUuid the name of the driver the job was submitted to.
   * @param jobInfo    information about the job.
   */
  public void removeJob(final String driverUuid, final JobInformation jobInfo) {
    if (jobInfo == null) throw new IllegalArgumentException("jobInfo is null");
    synchronized(accumulatorHelper) {
      AccumulatorDriver driver = accumulatorHelper.getAccumulatedDriver(driverUuid);
      Map<String, AccumulatorJob> jobMap = driver.getMap();
      String jobUuid = jobInfo.getJobUuid();
      if (debugEnabled) log.debug("removing job " + jobInfo + " from driver " + driverUuid);
      AccumulatorJob job = jobMap.get(jobUuid);
      if (job == null) {
        job = new AccumulatorJob(JobAccumulator.Type.REMOVE, jobInfo);
        jobMap.put(jobUuid, job);
      } else {
        boolean remove = job.mergeChange(JobAccumulator.Type.REMOVE);
        if (remove) jobMap.remove(jobUuid);
      }
    }
  }

  /**
   * Called to notify that a job was removed from a driver.
   * @param driverUuid the name of the driver the job was submitted to.
   * @param jobInfo    information about the job.
   */
  public void updateJob(final String driverUuid, final JobInformation jobInfo) {
    if (jobInfo == null) throw new IllegalArgumentException("jobInfo is null");
    String jobUuid = jobInfo.getJobUuid();
    if (debugEnabled) log.debug("updating job " + jobInfo + " from driver " + driverUuid);
    synchronized(accumulatorHelper) {
      AccumulatorDriver driver = accumulatorHelper.getAccumulatedDriver(driverUuid);
      Map<String, AccumulatorJob> jobMap = driver.getMap();
      AccumulatorJob job = jobMap.get(jobUuid);
      if (job == null) {
        job = new AccumulatorJob(JobAccumulator.Type.UPDATE, jobInfo);
        jobMap.put(jobUuid, job);
      } else {
        boolean remove = job.mergeChange(JobAccumulator.Type.UPDATE, jobInfo);
        if (remove) jobMap.remove(jobUuid);
      }
    }
  }

  /**
   * Called to notify that a sub-job was dispatched to a node.
   * @param driverUuid the name of the driver the job was submitted to.
   * @param jobInfo    information about the sub-job.
   * @param nodeInfo   information about the node where the sub-job was dispatched.
   */
  public void addDispatch(final String driverUuid, final JobInformation jobInfo, final JPPFManagementInfo nodeInfo) {
    String nodeName = nodeInfo.toString();
    if (debugEnabled) log.debug("driver " + driverUuid + ": adding sub-job " + jobInfo + " to node " + nodeInfo);
    synchronized(accumulatorHelper) {
      AccumulatorJob job = accumulatorHelper.getAccumulatorJob(driverUuid, jobInfo);
      Map<String, AccumulatorNode> nodeMap = job.getMap();
      AccumulatorNode node = nodeMap.get(nodeName);
      if (node == null) {
        node = new AccumulatorNode(JobAccumulator.Type.ADD, jobInfo, nodeInfo);
        nodeMap.put(nodeName, node);
      } else {
        boolean remove = node.mergeChange(JobAccumulator.Type.ADD);
        if (remove) nodeMap.remove(nodeName);
      }
    }
  }

  /**
   * Called to notify that a sub-job was removed from a node.
   * @param driverUuid the name of the driver the job was submitted to.
   * @param jobInfo    information about the job.
   * @param nodeInfo   information about the node where the sub-job was dispatched.
   */
  public void removeDispatch(final String driverUuid, final JobInformation jobInfo, final JPPFManagementInfo nodeInfo) {
    String nodeName = nodeInfo.toString();
    if (debugEnabled) log.debug("driver " + driverUuid + ": removing sub-job " + jobInfo + " from node " + nodeInfo);
    synchronized(accumulatorHelper) {
      AccumulatorJob job = accumulatorHelper.getAccumulatorJob(driverUuid, jobInfo);
      Map<String, AccumulatorNode> nodeMap = job.getMap();
      AccumulatorNode node = nodeMap.get(nodeName);
      if (node == null) {
        node = new AccumulatorNode(JobAccumulator.Type.REMOVE, jobInfo, nodeInfo);
        nodeMap.put(nodeName, node);
      } else {
        boolean remove = node.mergeChange(JobAccumulator.Type.REMOVE);
        if (remove) nodeMap.remove(nodeName);
      }
    }
  }

  /**
   * Refreshes the tree table display.
   */
  public void refreshUI() {
    treeTable.invalidate();
    treeTable.doLayout();
    treeTable.updateUI();
  }

  /**
   * Initialize all actions used in the panel.
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

  /**
   * Put a new task in the executor's queue to handle th specified notification from the psecified driver.
   * @param driver the name of the driver that sent the notification.
   * @param notif the notification to process.
   */
  void handleNotification(final TopologyDriver driver, final JobNotification notif) {
    notificationsExecutor.submit(new JobNotificationTask(driver, notif));
  }

  /**
   * Instances of this class process a single job notification sent from a driver.
   */
  private final class JobNotificationTask implements Runnable {
    /**
     * The driver that sent the notification.
     */
    private final TopologyDriver driver;
    /**
     * The notification to process.
     */
    private final JobNotification notif;

    /**
     * Initialize this notification processing task.
     * @param driver the driver that sent the notification.
     * @param notif the notification to process.
     */
    private JobNotificationTask(final TopologyDriver driver, final JobNotification notif) {
      this.driver = driver;
      this.notif = notif;
    }

    @Override
    public void run() {
      String uuid = driver.getUuid();
      JobInformation jobInfo = notif.getJobInformation();
      JPPFManagementInfo nodeInfo = notif.getNodeInfo();
      switch(notif.getEventType()) {
        case JOB_QUEUED:
          addJob(uuid, jobInfo);
          break;
        case JOB_ENDED:
          removeJob(uuid, jobInfo);
          break;
        case JOB_UPDATED:
          updateJob(uuid, jobInfo);
          break;
        case JOB_DISPATCHED:
          addDispatch(uuid, jobInfo, nodeInfo);
          break;
        case JOB_RETURNED:
          removeDispatch(uuid, jobInfo, nodeInfo);
          break;
      }
    }
  }

  /**
   * This task refreshes the entire job data panel.
   */
  public class RefreshTask implements Runnable {
    @Override
    public void run() {
      if (debugEnabled) log.debug("refresh requested");
      synchronized(accumulatorHelper) {
        accumulatorHelper.cleanup();
        panelManager.clearDriver();
        populateTreeTableModel();
        //accumulatorHelper.setup();
        accumulatorHelper.publish();
        refreshUI();
      }
    }
  }

  @Override
  public void driverAdded(final TopologyEvent event) {
    final TopologyDriver driver = event.getDriver();
    final JPPFClientConnection c = driver.getConnection();
    JPPFClientConnectionStatus status = c.getStatus();
    if ((status != null) && status.isWorkingStatus()) driverAdded(driver);
    else c.addClientConnectionStatusListener(new ClientConnectionStatusListener() {
      @Override
      public void statusChanged(final ClientConnectionStatusEvent cevt) {
        if (c.getStatus().isWorkingStatus()) driverAdded(driver);
      }
    });
  }

  @Override
  public void driverRemoved(final TopologyEvent event) {
    driverRemoved(event.getDriver());
  }

  @Override
  public void driverUpdated(final TopologyEvent event) {
  }

  @Override
  public void nodeAdded(final TopologyEvent event) {
  }

  @Override
  public void nodeRemoved(final TopologyEvent event) {
  }

  @Override
  public void nodeUpdated(final TopologyEvent event) {
  }

  /**
   * Get the topology manager.
   * @return a {@link TopologyManager} object.
   */
  TopologyManager getTopologyManager() {
    return topologyManager;
  }

  /**
   * Get the accumulator for driver and job state change notifications.
   * @return an instance of {@link AccumulatorHelper}.
   */
  public AccumulatorHelper getAccumulator() {
    return accumulatorHelper;
  }
}
