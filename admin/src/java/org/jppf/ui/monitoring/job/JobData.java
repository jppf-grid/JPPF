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

import javax.management.NotificationListener;

import org.jppf.client.JPPFClientConnection;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.job.JobInformation;
import org.jppf.management.*;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class hold the information related to each node in the job data tree table.
 * @author Laurent Cohen
 */
public class JobData implements AutoCloseable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JobData.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The type of this job data object.
   */
  private JobDataType type = null;
  /**
   * A driver to which jobs are submitted.
   */
  private final TopologyDriver driver;
  /**
   * Information on the job or sub-job in a JPPF driver or node.
   */
  private JobInformation jobInformation = null;
  /**
   * Information on the JPPF node where a job is dispatched.
   */
  private JPPFManagementInfo nodeInformation = null;
  /**
   * Receives notifications from the MBean.
   */
  private NotificationListener notificationListener = null;
  /**
   * 
   */
  private DriverJobManagementMBean jobManager = null;
  /**
   * 
   */
  private ProxySetting proxySetter;

  /**
   * Initialize this job data as a driver related object.
   * @param driver a reference to the driver.
   */
  public JobData(final TopologyDriver driver) {
    this.type = JobDataType.DRIVER;
    this.driver = driver;
    proxySetter = new ProxySetting();
    new Thread(proxySetter).start();
  }

  /**
   * Initialize this job data as a holding information about a job submitted to a driver.
   * @param driver a reference to the driver.
   * @param jobInformation information on the job in a JPPF driver.
   */
  public JobData(final TopologyDriver driver, final JobInformation jobInformation) {
    this.type = JobDataType.JOB;
    this.jobInformation = jobInformation;
    this.driver = driver;
  }

  /**
   * Initialize this job data as a holding information about a sub-job dispatched to a node.
   * @param jobInformation information on the job in a JPPF driver.
   * @param nodeInformation information on the JPPF node in which part of a job is executing.
   */
  public JobData(final JobInformation jobInformation, final JPPFManagementInfo nodeInformation) {
    this.type = JobDataType.SUB_JOB;
    this.jobInformation = jobInformation;
    this.nodeInformation = nodeInformation;
    this.driver = null;
  }

  /**
   * Get the type of this job data object.
   * @return a <code>JobDataType</code> enum value.
   */
  public JobDataType getType() {
    return type;
  }

  /**
   * Get the wrapper holding the connection to the JMX server on a driver.
   * @return a <code>JMXDriverConnectionWrapper</code> instance.
   */
  public JMXDriverConnectionWrapper getJmxWrapper() {
    return driver.getJmx();
  }

  /**
   * Get the information on the job or sub-job in a JPPF driver or node.
   * @return a <code>JobInformation</code> instance,
   */
  public JobInformation getJobInformation() {
    return jobInformation;
  }

  /**
   * Get the information on the JPPF node in which part of a job is executing.
   * @return a <code>NodeManagementInfo</code> instance.
   */
  public JPPFManagementInfo getNodeInformation() {
    return nodeInformation;
  }

  /**
   * Get a reference to the proxy to the job management mbean.
   * @return a {@link DriverJobManagementMBean} instance.
   */
  public synchronized DriverJobManagementMBean getProxy() {
    return jobManager;
  }

  /**
   * Get a reference to the proxy to the job management mbean.
   * @return a {@link DriverJobManagementMBean} instance.
   */
  private synchronized DriverJobManagementMBean initProxy() {
    if (jobManager == null) {
      JMXDriverConnectionWrapper jmx = getJmxWrapper();
      if ((jmx != null) && jmx.isConnected()) {
        try {
          jobManager = jmx.getJobManager();
          if (notificationListener != null) jobManager.addNotificationListener(notificationListener, null, null);
        } catch (Exception e) {
          /*
          String msg = "{},  error getting the job manager proxy: {}";
          if (debugEnabled) log.debug(msg, this, ExceptionUtils.getStackTrace(e));
          else log.warn(msg, this, ExceptionUtils.getMessage(e));
          */
        }
      }
    }
    return jobManager;
  }

  /**
   * Get a string representation of this object.
   * @return a string representing this object.
   */
  @Override
  public String toString() {
    String s = "";
    switch(type) {
      case DRIVER:
        s = driver.getDisplayName();
        break;
      case JOB:
        s = jobInformation.getJobName();
        break;
      case SUB_JOB:
        if (nodeInformation == null) s = "no information";
        else s = nodeInformation.getHost() + ':' + nodeInformation.getPort();
        break;
    }
    return s;
  }

  /**
   * Get the MBean notification listener.
   * @return a <code>NotificationListener</code> instance.
   */
  public NotificationListener getNotificationListener() {
    return notificationListener;
  }

  /**
   * Set the MBean notification listener.
   * @param listener a <code>NotificationListener</code> instance.
   * @throws Exception if any error occurs.
   */
  public synchronized void changeNotificationListener(final NotificationListener listener) throws Exception {
    DriverJobManagementMBean proxy = getProxy();
    if (proxy != null) {
      if (notificationListener != null) {
        try {
          proxy.removeNotificationListener(notificationListener);
        } catch (Exception e) {
          String s = ExceptionUtils.getMessage(e);
          if (debugEnabled) log.debug(s, e);
          //else log.warn(s);
        }
      }
    }
    notificationListener = listener;
    if ((notificationListener != null) && (proxy != null)) proxy.addNotificationListener(notificationListener, null, null);
  }

  /**
   * Get a reference to the driver connection.
   * @return a <code>JPPFClientConnection</code> instance.
   */
  public JPPFClientConnection getClientConnection() {
    return driver.getConnection();
  }

  /**
   * Get the driver to which jobs are submitted.
   * @return a {@link TopologyDriver} object.
   */
  public TopologyDriver getDriver() {
    return driver;
  }

  /**
   * 
   */
  private class ProxySetting extends ThreadSynchronization implements Runnable {
    @Override
    public void run() {
      while (!isStopped() && (initProxy() == null)) goToSleep(10L);
      if (debugEnabled) log.debug("proxy initialized for {}", JobData.this);
    }
  }

  @Override
  public void close(){
    if (proxySetter != null) proxySetter.setStopped(true);
  }
}
