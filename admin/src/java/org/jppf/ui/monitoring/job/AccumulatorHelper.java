/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.JPPFClientConnection;
import org.jppf.job.JobInformation;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * Helper class for updates accumulation.
 * @author Martin Janda
 * @author Laurent Cohen
 */
public class AccumulatorHelper {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(AccumulatorHelper.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Map that accumulates changes between TreeTable updates.
   */
  Map<String, AccumulatorDriver> accumulatorMap = new HashMap<>();
  /**
   * The GUI panel.
   */
  private JobDataPanel jobPanel = null;
  /**
   * The object that manages updates to and navigation within the tree table.
   */
  private JobDataPanelManager panelManager = null;
  /**
   *  Timer that publishes accumulated changes on AWT thread to TreeTable
   */
  private Timer timer = null;
  /**
   * The timer task which performs the refresh
   */
  private TimerTask timerTask = new MyTimerTask();
  /**
   * The period of the refreshing thread.
   */
  final int period = JPPFConfiguration.getProperties().getInt("jppf.gui.publish.period", 1000 / 30);

  /**
   * Initialize this hleper with the spsecified job panel.
   * @param jobPanel a {@link JobDataPanel} instance.
   */
  AccumulatorHelper(final JobDataPanel jobPanel) {
    this.jobPanel = jobPanel;
    panelManager = jobPanel.panelManager;
    setup();
  }

  /**
   * Publish the accumulated changes.
   */
  protected void publish() {
    assert SwingUtilities.isEventDispatchThread() : "Not on event dispatch thread";
    Map<String, AccumulatorDriver> map = getMap();
    boolean changed = false;
    for (Map.Entry<String, AccumulatorDriver> driverEntry : map.entrySet()) {
      String driverName = driverEntry.getKey();
      AccumulatorDriver driverAccumulator = driverEntry.getValue();
      JobAccumulator.Type driverType = driverAccumulator.getType();
      switch (driverType) {
        case ADD:
          panelManager.driverAdded(driverAccumulator.getValue());
          changed = true;
          break;
        case REMOVE:
          panelManager.driverRemoved(driverName);
          changed = true;
          continue;
        case UPDATE:
          DefaultMutableTreeNode driverNode = panelManager.findDriver(driverName);
          if (driverNode != null) jobPanel.getModel().changeNode(driverNode);
          changed = true;
          break;
      }
      changed |= publishDriverJobs(driverName, driverAccumulator);
    }
    if (changed) {
      jobPanel.refreshUI();
      if (jobPanel.getActionHandler() != null) jobPanel.getActionHandler().updateActions();
    }
    //accumulatorMap.clear();
  }

  /**
   * Process the changes that occurred to the jobs executing on the specified driver.
   * @param driverName the name of the driver where jobs are executing.
   * @param driverAccumulator the accumulater for the driver's jobs.
   * @return changed <code>true</code> if any change occurred, <code>false</code> otherwise.
   */
  private boolean publishDriverJobs(final String driverName, final AccumulatorDriver driverAccumulator) {
    DefaultMutableTreeNode driverNode = panelManager.findDriver(driverName);
    if (driverNode == null) return false;
    boolean changed = false;
    for (Map.Entry<String, AccumulatorJob> jobEntry : driverAccumulator.getMap().entrySet()) {
      String jobName = jobEntry.getKey();
      AccumulatorJob jobAccumulator = jobEntry.getValue();
      JobAccumulator.Type jobType = jobAccumulator.getType();
      switch (jobType) {
        case ADD:
          panelManager.jobAdded(driverName, jobAccumulator.getValue());
          changed = true;
          break;
        case REMOVE:
          panelManager.jobRemoved(driverName, jobName);
          changed = true;
          continue;
        case UPDATE:
          DefaultMutableTreeNode jobNode = panelManager.findJob(driverNode, jobName);
          if (jobNode != null) panelManager.jobUpdated(driverName, jobAccumulator.getValue());
          changed = true;
          break;
      }
      changed |= publishJobNodes(driverName, jobName, jobAccumulator);
    }
    return changed;
  }

  /**
   * Publish the changes that ocurred to the subjobs of the specified job.
   * @param driverName the name of the driver where jobs are executing.
   * @param jobName the name of the job.
   * @param jobAccumulator the job whose subjobs have changes to publish.
   * @return changed <code>true</code> if any change occurred, <code>false</code> otherwise.
   */
  private boolean publishJobNodes(final String driverName, final String jobName, final AccumulatorJob jobAccumulator) {
    boolean changed = false;
    for (Map.Entry<String, AccumulatorNode> nodeEntry : jobAccumulator.getMap().entrySet()) {
      String nodeName = nodeEntry.getKey();
      AccumulatorNode nodeAccumulator = nodeEntry.getValue();
      JobAccumulator.Type nodeType = nodeAccumulator.getType();
      switch (nodeType) {
        case ADD:
          panelManager.subJobAdded(driverName, nodeAccumulator.getJobInfo(), nodeAccumulator.getValue());
          changed = true;
          break;
        case REMOVE:
          panelManager.subJobRemoved(driverName, jobName, nodeName);
          changed = true;
          continue;
      }
    }
    return changed;
  }

  /**
   * Get the map of accumulated driver changes.
   * @return a map of <code>String</code> to <code>AccumulatorDriver</code> instances.
   */
  private synchronized Map<String, AccumulatorDriver> getMap() {
    Map<String, AccumulatorDriver> copy = new HashMap<>(accumulatorMap);
    accumulatorMap.clear();
    return copy;
  }

  /**
   * Start the timer.
   */
  synchronized void setup() {
    if (debugEnabled) log.debug("setup invoked");
    if (timer == null) timer = new Timer("accumulator timer");
    timerTask = new MyTimerTask();
    timer.schedule(timerTask, period, period);
  }

  /**
   * Clear the map of accumalator drivers and stop the timer.
   */
  synchronized void cleanup() {
    if (debugEnabled) log.debug("cleanup invoked");
    if (timerTask != null) timerTask.cancel();
    if (timer != null) timer.purge();
    accumulatorMap.clear();
  }

  /**
   * Get the update accumulator for the specified driver.
   * @param driverName the name of the driver.
   * @return an {@link AccumulatorDriver} instance.
   */
  synchronized AccumulatorDriver getAccumulatedDriver(final String driverName) {
    AccumulatorDriver driver = accumulatorMap.get(driverName);
    if(driver == null) {
      driver = new AccumulatorDriver(JobAccumulator.Type.KEEP, null);
      accumulatorMap.put(driverName, driver);
    }
    return driver;
  }

  /**
   * Get the update accumulator for the specified job.
   * @param driverName the name of the driver to which the job is submitted.
   * @param jobInfo the job description
   * @return an {@link AccumulatorJob} instance.
   */
  synchronized AccumulatorJob getAccumulatorJob(final String driverName, final JobInformation jobInfo) {
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

  /**
   * Definition of an update accumulator for a driver.
   */
  static class AccumulatorDriver extends JobAccumulatorBranch<JPPFClientConnection, String, AccumulatorJob> {
    /**
     * Initialize this driver accumulator for the specified type of updates and value.
     * @param type the type of updates.
     * @param value a {@link JPPFClientConnection} instance.
     */
    public AccumulatorDriver(final Type type, final JPPFClientConnection value) {
      super(type, value);
    }
  }

  /**
   * Definition of an update accumulator for a job.
   */
  static class AccumulatorJob extends JobAccumulatorBranch<JobInformation, String, AccumulatorNode> {
    /**
     * Initialize this job accumulator for the specified type of updates and value.
     * @param type the type of updates.
     * @param value a {@link JobInformation} instance.
     */
    public AccumulatorJob(final Type type, final JobInformation value) {
      super(type, value);
    }
  }

  /**
   * Definition of an update accumulator for a sub-job.
   */
  static class AccumulatorNode extends JobAccumulator<JPPFManagementInfo> {
    /**
     * Information about the job.
     */
    private final JobInformation jobInfo;

    /**
     * Initialize this job accumulator for the specified type of updates and value.
     * @param type the type of updates.
     * @param jobInfo Information about the job.
     * @param value a {@link JPPFManagementInfo} instance.
     */
    AccumulatorNode(final Type type, final JobInformation jobInfo, final JPPFManagementInfo value) {
      super(type, value);
      this.jobInfo = jobInfo;
    }

    /**
     * Get the information about the job.
     * @return a {@link JobInformation} instance.
     */
    public JobInformation getJobInfo() {
      return jobInfo;
    }
  }

  /**
   * Periodic task that publishes changes to the GUI.
   */
  private class MyTimerTask extends TimerTask {
    @Override
    public void run() {
      try {
        SwingUtilities.invokeAndWait(new Runnable() {
          public void run() {
            publish();
          }
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  };
}
