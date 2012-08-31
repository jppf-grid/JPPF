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

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.JPPFClientConnection;
import org.jppf.job.JobInformation;
import org.jppf.management.JPPFManagementInfo;

/**
 * Helper class for updates accumulation.
 * @author Martin Janda
 * @author Laurent Cohen
 */
public class AccumulatorHelper
{
  /**
   * Map that accumulates changes between TreeTable updates.
   */
  final Map<String, AccumulatorDriver> accumulatorMap = new HashMap<String, AccumulatorDriver>();
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
  Timer timer = null;

  /**
   * Initialize this hleper with the spsecified job panel.
   * @param jobPanel a {@link JobDataPanel} instance.
   */
  AccumulatorHelper(final JobDataPanel jobPanel)
  {
    this.jobPanel = jobPanel;
    panelManager = jobPanel.panelManager;
  }

  /**
   * Publish the accumulated changes.
   */
  protected void publish()
  {
    assert SwingUtilities.isEventDispatchThread() : "Not on event dispatch thread";
    Map<String, AccumulatorDriver> map = getMap();

    for (Map.Entry<String, AccumulatorDriver> driverEntry : map.entrySet())
    {
      String driverName = driverEntry.getKey();
      AccumulatorDriver driverAccumulator = driverEntry.getValue();
      JobAccumulator.Type driverType = driverAccumulator.getType();
      switch (driverType)
      {
        case ADD:
          panelManager.driverAdded(driverAccumulator.getValue());
          break;
        case REMOVE:
          panelManager.driverRemoved(driverName);
          continue;
        case UPDATE:
          DefaultMutableTreeNode driverNode = panelManager.findDriver(driverName);
          if (driverNode != null) jobPanel.getModel().changeNode(driverNode);
          break;
      }

      DefaultMutableTreeNode driverNode = panelManager.findDriver(driverName);
      for (Map.Entry<String, AccumulatorJob> jobEntry : driverAccumulator.getMap().entrySet())
      {
        String jobName = jobEntry.getKey();
        AccumulatorJob jobAccumulator = jobEntry.getValue();
        JobAccumulator.Type jobType = jobAccumulator.getType();
        switch (jobType)
        {
          case ADD:
            panelManager.jobAdded(driverName, jobAccumulator.getValue());
            break;
          case REMOVE:
            panelManager.jobRemoved(driverName, jobName);
            continue;
          case UPDATE:
            DefaultMutableTreeNode jobNode = panelManager.findJob(driverNode, jobName);
            if (jobNode != null) panelManager.jobUpdated(driverName, jobAccumulator.getValue());
            break;
        }

        for (Map.Entry<String, AccumulatorNode> nodeEntry : jobAccumulator.getMap().entrySet())
        {
          String nodeName = nodeEntry.getKey();
          AccumulatorNode nodeAccumulator = nodeEntry.getValue();
          JobAccumulator.Type nodeType = nodeAccumulator.getType();
          switch (nodeType)
          {
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
    if (jobPanel.getActionHandler() != null) jobPanel.getActionHandler().updateActions();
  }

  /**
   * Get the map of accumulated driver changes.
   * @return a map of <code>String</code> to <code>AccumulatorDriver</code> instances.
   */
  public synchronized Map<String, AccumulatorDriver> getMap()
  {
    Map<String, AccumulatorDriver> copy = new HashMap<String, AccumulatorDriver>(accumulatorMap);
    accumulatorMap.clear();
    timer = null;
    return copy;
  }

  /**
   * Clear the map of accumalator drivers and stop the timer.
   */
  synchronized void cleanup() {
    accumulatorMap.clear();
    if(timer != null) timer.stop();
    timer = null;
  }

  /**
   * Get the update accumulator for the specified driver.
   * @param driverName the name of the driver.
   * @return an {@link AccumulatorDriver} instance.
   */
  synchronized AccumulatorDriver getAccumulatedDriver(final String driverName)
  {
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
  synchronized AccumulatorJob getAccumulatorJob(final String driverName, final JobInformation jobInfo)
  {
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
  static class AccumulatorDriver extends JobAccumulatorBranch<JPPFClientConnection, String, AccumulatorJob>
  {
    /**
     * Initialize this driver accumulator for the specified type of updates and value.
     * @param type the type of updates.
     * @param value a {@link JPPFClientConnection} instance.
     */
    public AccumulatorDriver(final Type type, final JPPFClientConnection value)
    {
      super(type, value);
    }
  }

  /**
   * Definition of an update accumulator for a job.
   */
  static class AccumulatorJob extends JobAccumulatorBranch<JobInformation, String, AccumulatorNode>
  {
    /**
     * Initialize this job accumulator for the specified type of updates and value.
     * @param type the type of updates.
     * @param value a {@link JobInformation} instance.
     */
    public AccumulatorJob(final Type type, final JobInformation value)
    {
      super(type, value);
    }
  }

  /**
   * Definition of an update accumulator for a sub-job.
   */
  static class AccumulatorNode extends JobAccumulator<JPPFManagementInfo>
  {
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
    AccumulatorNode(final Type type, final JobInformation jobInfo, final JPPFManagementInfo value)
    {
      super(type, value);
      this.jobInfo = jobInfo;
    }

    /**
     * Get the information about the job.
     * @return a {@link JobInformation} instance.
     */
    public JobInformation getJobInfo()
    {
      return jobInfo;
    }
  }
}
