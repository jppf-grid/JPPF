/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.job.*;
import org.jppf.management.*;
import org.jppf.server.job.management.*;
import org.jppf.ui.actions.ActionsInitializer;
import org.jppf.ui.monitoring.data.StatsHandler;
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
public class JobDataPanel extends AbstractTreeTableOption implements ClientListener
{
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
   * Initialize this panel with the specified information.
   */
  public JobDataPanel()
  {
    BASE = "org.jppf.ui.i18n.JobDataPage";
    if (debugEnabled) log.debug("initializing NodeDataPanel");
    createTreeTableModel();
    panelManager = new JobDataPanelManager(this);
    accumulatorHelper = new AccumulatorHelper(this);
    populateTreeTableModel();
    StatsHandler.getInstance().getJppfClient(null).addClientListener(this);
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private void createTreeTableModel()
  {
    treeTableRoot = new DefaultMutableTreeNode(localize("job.tree.root.name"));
    model = new JobTreeTableModel(treeTableRoot);
  }

  /**
   * Refresh the tree from the latest data found in the server.
   * This method will clear the entire tree and repopulate it.
   */
  public synchronized void refresh()
  {
    SwingUtilities.invokeLater(new RefreshTask());
    //new RefreshTask().run();
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private synchronized void populateTreeTableModel()
  {
    if (debugEnabled) log.debug("populating the tree table");
    assert SwingUtilities.isEventDispatchThread() : "Not on event dispatch thread";

    List<JPPFClientConnection> list = StatsHandler.getInstance().getJppfClient(null).getAllConnections();
    if (debugEnabled) log.debug("connections = " + list);
    for (JPPFClientConnection c : list)
    {
      panelManager.driverAdded(c);
      if (debugEnabled) log.debug("added driver " + c);
      JPPFClientConnectionImpl connection = (JPPFClientConnectionImpl) c;
      if (connection.getJmxConnection() == null) continue;
      String driverName = ((JPPFClientConnectionImpl) connection).getUuid();
      DefaultMutableTreeNode driverNode = panelManager.findDriver(driverName);
      if (driverNode == null) continue;
      JobData driverData = (JobData) driverNode.getUserObject();
      DriverJobManagementMBean proxy = driverData.getProxy();
      if (proxy == null) continue;
      String[] jobIds;
      try
      {
        jobIds = proxy.getAllJobIds();
      }
      catch (Exception ex)
      {
        if (debugEnabled) log.debug("populating model: " + ex.getMessage(), ex);
        continue;
      }
      for (String id : jobIds)
      {
        JobInformation jobInfo = null;
        try
        {
          jobInfo = proxy.getJobInformation(id);
        }
        catch (Exception e)
        {
          if (debugEnabled) log.debug("populating model: " + e.getMessage(), e);
        }
        if (jobInfo == null) continue;
        panelManager.jobAdded(driverName, jobInfo);
        try
        {
          NodeJobInformation[] subJobInfo = proxy.getNodeInformation(id);
          for (NodeJobInformation nji : subJobInfo) panelManager.subJobAdded(driverName, nji.jobInfo, nji.nodeInfo);
        }
        catch (Exception e)
        {
          if (debugEnabled) log.debug("populating model: " + e.getMessage(), e);
        }
      }
    }
  }

  /**
   * Create, initialize and layout the GUI components displayed in this panel.
   */
  @Override
  public void createUI()
  {
    treeTable = new JPPFTreeTable(model);
    treeTable.getTree().setLargeModel(true);
    treeTable.getTree().setRootVisible(false);
    treeTable.getTree().setShowsRootHandles(true);
    treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
    treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    treeTable.doLayout();
    treeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    treeTable.getTree().setCellRenderer(new JobRenderer());
    treeTable.setDefaultRenderer(Object.class, new JobTableCellRenderer());
    JScrollPane sp = new JScrollPane(treeTable);
    setUIComponent(sp);
    treeTable.expandAll();
  }

  /**
   * Called to notify that a driver was added.
   * @param clientConnection a reference to the driver connection.
   */
  public void driverAdded(final JPPFClientConnection clientConnection)
  {
    if (clientConnection == null) throw new IllegalArgumentException("clientConnection is null");
    String driverName = ((JPPFClientConnectionImpl) clientConnection).getUuid();
    if (debugEnabled) log.debug("adding driver " + clientConnection + ", uuid=" + driverName);
    synchronized(accumulatorHelper)
    {
      AccumulatorDriver driver = accumulatorHelper.accumulatorMap.get(driverName);
      if (driver == null)
      {
        driver = new AccumulatorDriver(JobAccumulator.Type.ADD, clientConnection);
        accumulatorHelper.accumulatorMap.put(driverName, driver);
      }
      else
      {
        boolean remove = driver.mergeChange(JobAccumulator.Type.ADD);
        if (remove) accumulatorHelper.accumulatorMap.remove(driverName);
      }
    }
  }

  /**
   * Called to notify that a driver was removed.
   * @param clientConnection a reference to the driver connection to remove.
   */
  public void driverRemoved(final JPPFClientConnection clientConnection)
  {
    if (clientConnection == null) throw new IllegalArgumentException("clientConnection is null");
    String driverName = ((JPPFClientConnectionImpl) clientConnection).getUuid();
    if (debugEnabled) log.debug("removing driver " + clientConnection + ", uuid=" + driverName);
    synchronized(accumulatorHelper)
    {
      AccumulatorDriver driver = accumulatorHelper.accumulatorMap.get(driverName);
      if (driver == null) accumulatorHelper.accumulatorMap.put(driverName, new AccumulatorDriver(JobAccumulator.Type.REMOVE, clientConnection));
      else
      {
        boolean remove = driver.mergeChange(JobAccumulator.Type.REMOVE);
        if (remove) accumulatorHelper.accumulatorMap.remove(driverName);
      }
    }
  }

  /**
   * Called to notify that a driver was updated.
   * @param clientConnection a reference to the driver connection that changed.
   */
  public void driverUpdated(final JPPFClientConnection clientConnection)
  {
    if (clientConnection == null) throw new IllegalArgumentException("clientConnection is null");
    String driverName = ((JPPFClientConnectionImpl) clientConnection).getUuid();
    if (debugEnabled) log.debug("updating driver " + clientConnection + ", uuid=" + driverName);
    synchronized(accumulatorHelper)
    {
      AccumulatorDriver driver = accumulatorHelper.accumulatorMap.get(driverName);
      if (driver == null)
      {
        driver = new AccumulatorDriver(JobAccumulator.Type.UPDATE, clientConnection);
        accumulatorHelper.accumulatorMap.put(driverName, driver);
      }
      else
      {
        boolean remove = driver.mergeChange(JobAccumulator.Type.UPDATE, clientConnection);
        if (remove) accumulatorHelper.accumulatorMap.remove(driverName);
      }
    }
  }

  /**
   * Called to notify that a job was submitted to a driver.
   * @param driverName the name of the driver the job was submitted to.
   * @param jobInfo    information about the submitted job.
   */
  public void jobAdded(final String driverName, final JobInformation jobInfo)
  {
    if (jobInfo == null) throw new IllegalArgumentException("jobInfo is null");
    String jobName = jobInfo.getJobName();
    if (debugEnabled) log.debug("adding job " + jobInfo + " to driver " + driverName);
    synchronized(accumulatorHelper)
    {
      AccumulatorDriver driver = accumulatorHelper.getAccumulatedDriver(driverName);
      Map<String, AccumulatorJob> jobMap = driver.getMap();
      AccumulatorJob job = jobMap.get(jobName);
      if (job == null)
      {
        job = new AccumulatorJob(JobAccumulator.Type.ADD, jobInfo);
        jobMap.put(jobInfo.getJobName(), job);
      }
      else
      {
        boolean remove = job.mergeChange(JobAccumulator.Type.ADD);
        if (remove) jobMap.remove(jobName);
      }
    }
  }

  /**
   * Called to notify that a job was removed from a driver.
   * @param driverName the name of the driver the job was submitted to.
   * @param jobInfo    information about the job.
   */
  public void jobRemoved(final String driverName, final JobInformation jobInfo)
  {
    if (jobInfo == null) throw new IllegalArgumentException("jobInfo is null");
    synchronized(accumulatorHelper)
    {
      AccumulatorDriver driver = accumulatorHelper.getAccumulatedDriver(driverName);
      Map<String, AccumulatorJob> jobMap = driver.getMap();
      String jobName = jobInfo.getJobName();
      if (debugEnabled) log.debug("removing job " + jobInfo + " from driver " + driverName);
      AccumulatorJob job = jobMap.get(jobName);
      if (job == null)
      {
        job = new AccumulatorJob(JobAccumulator.Type.REMOVE, jobInfo);
        jobMap.put(jobName, job);
      }
      else
      {
        boolean remove = job.mergeChange(JobAccumulator.Type.REMOVE);
        if (remove) jobMap.remove(jobName);
      }
    }
  }

  /**
   * Called to notify that a job was removed from a driver.
   * @param driverName the name of the driver the job was submitted to.
   * @param jobInfo    information about the job.
   */
  public void jobUpdated(final String driverName, final JobInformation jobInfo)
  {
    if (jobInfo == null) throw new IllegalArgumentException("jobInfo is null");
    String jobName = jobInfo.getJobName();
    if (debugEnabled) log.debug("updating job " + jobInfo + " from driver " + driverName);
    synchronized(accumulatorHelper)
    {
      AccumulatorDriver driver = accumulatorHelper.getAccumulatedDriver(driverName);
      Map<String, AccumulatorJob> jobMap = driver.getMap();
      AccumulatorJob job = jobMap.get(jobName);
      if (job == null)
      {
        job = new AccumulatorJob(JobAccumulator.Type.UPDATE, jobInfo);
        jobMap.put(jobName, job);
      }
      else
      {
        boolean remove = job.mergeChange(JobAccumulator.Type.UPDATE, jobInfo);
        if (remove) jobMap.remove(jobName);
      }
    }
  }

  /**
   * Called to notify that a sub-job was dispatched to a node.
   * @param driverName the name of the driver the job was submitted to.
   * @param jobInfo    information about the sub-job.
   * @param nodeInfo   information about the node where the sub-job was dispatched.
   */
  public void subJobAdded(final String driverName, final JobInformation jobInfo, final JPPFManagementInfo nodeInfo)
  {
    String nodeName = nodeInfo.toString();
    if (debugEnabled) log.debug("driver " + driverName + ": adding sub-job " + jobInfo + " to node " + nodeName);
    synchronized(accumulatorHelper)
    {
      AccumulatorJob job = accumulatorHelper.getAccumulatorJob(driverName, jobInfo);
      Map<String, AccumulatorNode> nodeMap = job.getMap();
      AccumulatorNode node = nodeMap.get(nodeName);
      if (node == null)
      {
        node = new AccumulatorNode(JobAccumulator.Type.ADD, jobInfo, nodeInfo);
        nodeMap.put(nodeName, node);
      }
      else
      {
        boolean remove = node.mergeChange(JobAccumulator.Type.ADD);
        if (remove) nodeMap.remove(nodeName);
      }
    }
  }

  /**
   * Called to notify that a sub-job was removed from a node.
   * @param driverName the name of the driver the job was submitted to.
   * @param jobInfo    information about the job.
   * @param nodeInfo   information about the node where the sub-job was dispatched.
   */
  public void subJobRemoved(final String driverName, final JobInformation jobInfo, final JPPFManagementInfo nodeInfo)
  {
    String nodeName = nodeInfo.toString();
    if (debugEnabled) log.debug("driver " + driverName + ": removing sub-job " + jobInfo + " from node " + nodeName);
    synchronized(accumulatorHelper)
    {
      AccumulatorJob job = accumulatorHelper.getAccumulatorJob(driverName, jobInfo);
      Map<String, AccumulatorNode> nodeMap = job.getMap();
      AccumulatorNode node = nodeMap.get(nodeName);
      if (node == null)
      {
        node = new AccumulatorNode(JobAccumulator.Type.REMOVE, jobInfo, nodeInfo);
        nodeMap.put(nodeName, node);
      }
      else
      {
        boolean remove = node.mergeChange(JobAccumulator.Type.REMOVE);
        if (remove) nodeMap.remove(nodeName);
      }
    }
  }

  /**
   * Notify this listener that a new driver connection was created.
   * @param event the event to notify this listener of.
   * @see org.jppf.client.event.ClientListener#newConnection(org.jppf.client.event.ClientEvent)
   */
  @Override
  public void newConnection(final ClientEvent event)
  {
    driverAdded(event.getConnection());
  }

  @Override
  public void connectionFailed(final ClientEvent event)
  {
    driverRemoved(event.getConnection());
  }

  /**
   * Refreshes the tree table display.
   */
  public void refreshUI()
  {
    /*
    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        treeTable.invalidate();
        treeTable.doLayout();
        treeTable.updateUI();
        notifyChange();
      }
    });
     */
    treeTable.invalidate();
    treeTable.doLayout();
    treeTable.updateUI();
  }

  /**
   * Initialize all actions used in the panel.
   */
  public void setupActions()
  {
    actionHandler = new JobDataPanelActionManager(treeTable);
    synchronized(actionHandler)
    {
      actionHandler.putAction("cancel.job", new CancelJobAction());
      actionHandler.putAction("suspend.job", new SuspendJobAction());
      actionHandler.putAction("suspend_requeue.job", new SuspendRequeueJobAction());
      actionHandler.putAction("resume.job", new ResumeJobAction());
      actionHandler.putAction("max.nodes.job", new UpdateMaxNodesAction());
      actionHandler.putAction("update.priority.job", new UpdatePriorityAction());
      actionHandler.updateActions();
    }
    treeTable.addMouseListener(new JobTreeTableMouseListener(actionHandler));
    Runnable r = new ActionsInitializer(this, "/job.toolbar");
    new Thread(r).start();
  }

  /**
   * Put a new task in the executor's queue to handle th specified notification from the psecified driver.
   * @param driverName the name of the driver that sent the notification.
   * @param notif the notification to process.
   */
  void handleNotification(final String driverName, final JobNotification notif)
  {
    notificationsExecutor.submit(new JobNotificationTask(driverName, notif));
  }

  /**
   * Instances of this class process a single job notification sent from a driver.
   */
  private final class JobNotificationTask implements Runnable
  {
    /**
     * The name of the driver that sent the notification.
     */
    private final String driverName;
    /**
     * The notification to process.
     */
    private final JobNotification notif;

    /**
     * Initialize this notification processing task.
     * @param driverName the name of the driver that sent the notification.
     * @param notif the notification to process.
     */
    private JobNotificationTask(final String driverName, final JobNotification notif)
    {
      this.driverName = driverName;
      this.notif = notif;
    }

    @Override
    public void run()
    {
      switch(notif.getEventType())
      {
        case JOB_QUEUED:
          jobAdded(driverName, notif.getJobInformation());
          break;
        case JOB_ENDED:
          jobRemoved(driverName, notif.getJobInformation());
          break;
        case JOB_UPDATED:
          jobUpdated(driverName, notif.getJobInformation());
          break;
        case JOB_DISPATCHED:
          subJobAdded(driverName, notif.getJobInformation(), notif.getNodeInfo());
          break;
        case JOB_RETURNED:
          subJobRemoved(driverName, notif.getJobInformation(), notif.getNodeInfo());
          break;
      }
    }
  }

  /**
   * This task refreshes the entire job data panel.
   */
  public class RefreshTask implements Runnable
  {
    @Override
    public void run()
    {
      if (debugEnabled) log.debug("refresh requested");
      synchronized(accumulatorHelper)
      {
        accumulatorHelper.cleanup();
        panelManager.driverClear();
        populateTreeTableModel();
        accumulatorHelper.setup();
        refreshUI();
      }
    }
  }
}
