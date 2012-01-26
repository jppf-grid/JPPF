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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.job.JobInformation;
import org.jppf.management.*;
import org.jppf.server.job.management.*;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.job.actions.*;
import org.jppf.ui.treetable.*;
import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
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
   * Map that accumulates changes between TreeTable updates.
   */
  private final Map<String, AccumulatorDriver> accumulatorMap = new HashMap<String, AccumulatorDriver>();
  /**
   *  Timer that publishes accumulated changes on AWT thread to TreeTable
   */
  private Timer timer = null;

  /**
   * The object that manages updates to and navigation within the tree table.
   */
  private JobDataPanelManager panelManager = null;

  /**
   * Initialize this panel with the specified information.
   */
  public JobDataPanel()
  {
    BASE = "org.jppf.ui.i18n.JobDataPage";
    if (debugEnabled) log.debug("initializing NodeDataPanel");
    createTreeTableModel();
    createUI();
    panelManager = new JobDataPanelManager(this);
    populateTreeTableModel();
    StatsHandler.getInstance().getJppfClient(null).addClientListener(this);
    treeTable.expandAll();
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
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private synchronized void populateTreeTableModel()
  {
    assert SwingUtilities.isEventDispatchThread() : "Not on event dispatch thread";

    List<JPPFClientConnection> list = StatsHandler.getInstance().getJppfClient(null).getAllConnections();
    for (JPPFClientConnection c: list)
    {
      panelManager.driverAdded(c);
      JPPFClientConnectionImpl connection = (JPPFClientConnectionImpl) c;
      JMXDriverConnectionWrapper wrapper = connection.getJmxConnection();
      if (wrapper == null) continue;
      String driverName = wrapper.getId();
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
      catch(Exception ex)
      {
        if (debugEnabled) log.debug("populating model: " + ex.getMessage(), ex);
        continue;
      }
      for (String id: jobIds)
      {
        JobInformation jobInfo = null;
        try
        {
          jobInfo = proxy.getJobInformation(id);
        }
        catch(Exception e)
        {
          if (debugEnabled) log.debug("populating model: " + e.getMessage(), e);
        }
        if (jobInfo == null) continue;
        panelManager.jobAdded(driverName, jobInfo);
        try
        {
          NodeJobInformation[] subJobInfo = proxy.getNodeInformation(id);
          for (NodeJobInformation nji: subJobInfo) panelManager.subJobAdded(driverName, nji.jobInfo, nji.nodeInfo);
        }
        catch(Exception e)
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
    //setupActions();
  }

  /**
   * Called to notify that a driver was added.
   * @param clientConnection a reference to the driver connection.
   */
  public synchronized void driverAdded(final JPPFClientConnection clientConnection)
  {
    if(clientConnection == null) throw new IllegalArgumentException("clientConnection is null");
    JMXDriverConnectionWrapper wrapper = ((JPPFClientConnectionImpl) clientConnection).getJmxConnection();
    String driverName = wrapper.getId();

    AccumulatorDriver driver = accumulatorMap.get(driverName);
    if(driver == null) {
      driver = new AccumulatorDriver(JobAccumulator.Type.ADD, clientConnection);
      accumulatorMap.put(driverName, driver);
    } else throw new IllegalStateException("driver already defined: " + driverName);

    notifyChange();
  }

  /**
   * Called to notify that a driver was removed.
   * @param clientConnection a reference to the driver connection to remove.
   */
  public synchronized void driverRemoved(final JPPFClientConnection clientConnection)
  {
    if(clientConnection == null) throw new IllegalArgumentException("clientConnection is null");
    JMXDriverConnectionWrapper wrapper = ((JPPFClientConnectionImpl) clientConnection).getJmxConnection();
    String driverName = wrapper.getId();
    AccumulatorDriver driver = accumulatorMap.get(driverName);
    if(driver == null) {
      accumulatorMap.put(driverName, new AccumulatorDriver(JobAccumulator.Type.REMOVE, clientConnection));
    }
    else
    {
      boolean remove = driver.mergeChange(JobAccumulator.Type.REMOVE);
      if(remove) accumulatorMap.remove(driverName);
    }

    notifyChange();
  }

  public synchronized void driverUpdated(final JPPFClientConnection clientConnection)
  {
    if(clientConnection == null) throw new IllegalArgumentException("clientConnection is null");
    JMXDriverConnectionWrapper wrapper = ((JPPFClientConnectionImpl) clientConnection).getJmxConnection();
    String driverName = wrapper.getId();

    AccumulatorDriver driver = accumulatorMap.get(driverName);
    if(driver == null) {
      driver = new AccumulatorDriver(JobAccumulator.Type.UPDATE, clientConnection);
      accumulatorMap.put(driverName, driver);
    } else {
      boolean remove = driver.mergeChange(JobAccumulator.Type.UPDATE, clientConnection);
      if(remove) accumulatorMap.remove(driverName);
    }

    notifyChange();
  }

  /**
   * Called to notify that a job was submitted to a driver.
   * @param driverName the name of the driver the job was submitted to.
   * @param jobInfo information about the submitted job.
   */
  public synchronized void jobAdded(final String driverName, final JobInformation jobInfo)
  {
    if(jobInfo == null) throw new IllegalArgumentException("jobInfo is null");

    AccumulatorDriver driver = getAccumulatedDriver(driverName);
    Map<String, AccumulatorJob> jobMap = driver.getMap();
    AccumulatorJob job = jobMap.get(jobInfo.getJobName());
    if(job == null) {
      job = new AccumulatorJob(JobAccumulator.Type.ADD, jobInfo);
      jobMap.put(jobInfo.getJobName(), job);
      notifyChange();
    } else
      throw new IllegalStateException("job already defined");
  }

  /**
   * Called to notify that a job was removed from a driver.
   * @param driverName the name of the driver the job was submitted to.
   * @param jobInfo information about the job.
   */
  public synchronized void jobRemoved(final String driverName, final JobInformation jobInfo)
  {
    if(jobInfo == null) throw new IllegalArgumentException("jobInfo is null");

    AccumulatorDriver driver = getAccumulatedDriver(driverName);

    Map<String, AccumulatorJob> jobMap = driver.getMap();
    String jobName = jobInfo.getJobName();
    AccumulatorJob job = jobMap.get(jobName);
    if(job == null) {
      job = new AccumulatorJob(JobAccumulator.Type.REMOVE, jobInfo);
      jobMap.put(jobName, job);
    } else {
      boolean remove = job.mergeChange(JobAccumulator.Type.REMOVE);
      if(remove) jobMap.remove(jobName);
    }

    notifyChange();
  }

  /**
   * Called to notify that a job was removed from a driver.
   * @param driverName the name of the driver the job was submitted to.
   * @param jobInfo information about the job.
   */
  public synchronized void jobUpdated(final String driverName, final JobInformation jobInfo)
  {
    if(jobInfo == null) throw new IllegalArgumentException("jobInfo is null");

    AccumulatorDriver driver = getAccumulatedDriver(driverName);

    Map<String, AccumulatorJob> jobMap = driver.getMap();
    String jobName = jobInfo.getJobName();
    AccumulatorJob job = jobMap.get(jobName);
    if(job == null) {
      job = new AccumulatorJob(JobAccumulator.Type.UPDATE, jobInfo);
      jobMap.put(jobName, job);
    } else {
      boolean remove = job.mergeChange(JobAccumulator.Type.UPDATE, jobInfo);
      if(remove) jobMap.remove(jobName);
    }

    notifyChange();
  }

  /**
   * Called to notify that a sub-job was dispatched to a node.
   * @param driverName the name of the driver the job was submitted to.
   * @param jobInfo information about the sub-job.
   * @param nodeInfo information about the node where the sub-job was dispatched.
   */
  public synchronized void subJobAdded(final String driverName, final JobInformation jobInfo, final JPPFManagementInfo nodeInfo)
  {
    AccumulatorJob job = getAccumulatorJob(driverName, jobInfo);

    Map<String, AccumulatorNode> nodeMap = job.getMap();
    String nodeName = nodeInfo.toString();
    AccumulatorNode node = nodeMap.get(nodeName);
    if(node == null) {
      node = new AccumulatorNode(JobAccumulator.Type.ADD, jobInfo, nodeInfo);
      nodeMap.put(nodeName, node);
      notifyChange();
    } else
      throw new IllegalStateException("node already defined");
  }

  /**
   * Called to notify that a sub-job was removed from a node.
   * @param driverName the name of the driver the job was submitted to.
   * @param jobInfo information about the job.
   * @param nodeInfo information about the node where the sub-job was dispatched.
   */
  public synchronized void subJobRemoved(final String driverName, final JobInformation jobInfo, final JPPFManagementInfo nodeInfo)
  {
    AccumulatorJob job = getAccumulatorJob(driverName, jobInfo);

    Map<String, AccumulatorNode> nodeMap = job.getMap();
    String nodeName = nodeInfo.toString();
    AccumulatorNode node = nodeMap.get(nodeName);
    if(node == null) {
      node = new AccumulatorNode(JobAccumulator.Type.REMOVE, jobInfo, nodeInfo);
      nodeMap.put(nodeName, node);
    } else {
      boolean remove = node.mergeChange(JobAccumulator.Type.REMOVE);
      if(remove) nodeMap.remove(nodeName);
    }

    notifyChange();
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

  /**
   * {@inheritDoc}
   */
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
  }

  /**
   * Initialize all actions used in the panel.
   */
  public void setupActions()
  {
    actionHandler = new JobDataPanelActionManager(treeTable);
    actionHandler.putAction("cancel.job", new CancelJobAction());
    actionHandler.putAction("suspend.job", new SuspendJobAction());
    actionHandler.putAction("suspend_requeue.job", new SuspendRequeueJobAction());
    actionHandler.putAction("resume.job", new ResumeJobAction());
    actionHandler.putAction("max.nodes.job", new UpdateMaxNodesAction());
    actionHandler.putAction("update.priority.job", new UpdatePriorityAction());
    actionHandler.updateActions();
    treeTable.addMouseListener(new JobTreeTableMouseListener(actionHandler));
    Runnable r = new ActionsInitializer(this, "/job.toolbar");
    new Thread(r).start();
  }

  protected void publish() {
    assert SwingUtilities.isEventDispatchThread() : "Not on event dispatch thread";
    Map<String, AccumulatorDriver> map = getMap();

    for (Map.Entry<String, AccumulatorDriver> driverEntry : map.entrySet())
    {
      String driverName = driverEntry.getKey();
      AccumulatorDriver driverAccumulator = driverEntry.getValue();
      JobAccumulator.Type driverType = driverAccumulator.getType();
      switch (driverType) {
        case ADD:
          panelManager.driverAdded(driverAccumulator.getValue());
          break;
        case REMOVE:
          panelManager.driverRemoved(driverName);
          continue;
        case UPDATE:
        {
          DefaultMutableTreeNode driverNode = panelManager.findDriver(driverName);
          if (driverNode != null) getModel().changeNode(driverNode);
        }
        break;
      }

      DefaultMutableTreeNode driverNode = panelManager.findDriver(driverName);
      for (Map.Entry<String, AccumulatorJob> jobEntry : driverAccumulator.getMap().entrySet())
      {
        String jobName = jobEntry.getKey();
        AccumulatorJob jobAccumulator = jobEntry.getValue();
        JobAccumulator.Type jobType = jobAccumulator.getType();
        switch (jobType) {
          case ADD:
            panelManager.jobAdded(driverName, jobAccumulator.getValue());
            break;
          case REMOVE:
            panelManager.jobRemoved(driverName, jobName);
            continue;
          case UPDATE:
          {
            DefaultMutableTreeNode jobNode = panelManager.findJob(driverNode, jobName);
            if(jobNode != null) panelManager.jobUpdated(driverName, jobAccumulator.getValue());
          }
          break;
        }

        for (Map.Entry<String, AccumulatorNode> nodeEntry : jobAccumulator.getMap().entrySet())
        {
          String nodeName = nodeEntry.getKey();
          AccumulatorNode nodeAccumulator = nodeEntry.getValue();
          JobAccumulator.Type nodeType = nodeAccumulator.getType();
          switch (nodeType) {
            case ADD:
              panelManager.subJobAdded(driverName, nodeAccumulator.getJobInfo(), nodeAccumulator.getValue());
              break;
            case REMOVE:
              panelManager.subJobRemoved(driverName, jobName, nodeName);
              continue;
          }
        }
      }
    }
    if (actionHandler != null) actionHandler.updateActions();
  }

  protected synchronized void notifyChange() {
    if(timer == null) {
      final int period = JPPFConfiguration.getProperties().getInt("jppf.gui.publish.period", 1000/30);
      timer = new Timer(period, new ActionListener()
      {
        @Override
        public void actionPerformed(final ActionEvent e)
        {
          synchronized (JobDataPanel.this) {
            //timer = null;
          }
          publish();
        }
      });
      //timer.setRepeats(false);
      timer.setRepeats(true);
      timer.start();
    }
  }

  public synchronized Map<String, AccumulatorDriver> getMap()
  {
    Map<String, AccumulatorDriver> copy = new HashMap<String, AccumulatorDriver>(accumulatorMap);
    accumulatorMap.clear();
    timer = null;
    return copy;
  }

  protected synchronized void cleanup() {
    accumulatorMap.clear();
    if(timer != null) timer.stop();
    timer = null;
  }

  /**
   * This task refreshes the entire job data panel.
   */
  public class RefreshTask implements Runnable
  {
    /**
     * Initialize this task.
     */
    public RefreshTask()
    {
    }

    /**
     * Perform the refresh.
     */
    @Override
    public void run()
    {
      cleanup();
      panelManager.driverClear();
      populateTreeTableModel();
      refreshUI();
    }
  }

  private AccumulatorDriver getAccumulatedDriver(final String driverName)
  {
    AccumulatorDriver driver = accumulatorMap.get(driverName);
    if(driver == null) {
      driver = new AccumulatorDriver(JobAccumulator.Type.KEEP, null);
      accumulatorMap.put(driverName, driver);
    }
    return driver;
  }

  private AccumulatorJob getAccumulatorJob(final String driverName, final JobInformation jobInfo) {
    AccumulatorDriver driver = getAccumulatedDriver(driverName);

    Map<String, AccumulatorJob> jobMap = driver.getMap();
    String jobName = jobInfo.getJobName();
    AccumulatorJob job = jobMap.get(jobName);
    if(job == null) {
      job = new AccumulatorJob(JobAccumulator.Type.KEEP, jobInfo);
      jobMap.put(jobName, job);
    }
    return job;
  }

  static class AccumulatorDriver extends JobAccumulatorBranch<JPPFClientConnection, String, AccumulatorJob>
  {

    public AccumulatorDriver(final Type type, final JPPFClientConnection value)
    {
      super(type, value);
    }
  }

  static class AccumulatorJob extends JobAccumulatorBranch<JobInformation, String, AccumulatorNode>
  {

    public AccumulatorJob(final Type type, final JobInformation value)
    {
      super(type, value);
    }
  }

  static class AccumulatorNode extends JobAccumulator<JPPFManagementInfo> {
    private final JobInformation jobInfo;

    AccumulatorNode(final Type type, final JobInformation jobInfo, final JPPFManagementInfo value)
    {
      super(type, value);
      this.jobInfo = jobInfo;
    }

    public JobInformation getJobInfo()
    {
      return jobInfo;
    }
  }
}
