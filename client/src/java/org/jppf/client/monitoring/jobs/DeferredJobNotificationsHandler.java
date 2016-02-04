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

package org.jppf.client.monitoring.jobs;

import static org.jppf.client.monitoring.jobs.DeferredJobNotificationsHandler.JobNotificationType.*;

import java.util.*;

import org.jppf.client.monitoring.AbstractRefreshHandler;
import org.jppf.client.monitoring.topology.*;
import org.jppf.job.*;
import org.jppf.management.JPPFManagementInfo;
import org.slf4j.*;

/**
 * This class publishes JMX notifications at regular intervals.
 * If, in the same interval, multiple notifications are received for the same job or job dispatch, then they are aggregated into a single event, or no event at all.
 * <p>Examples:<br>
 * - a {@code JOB_QUEUED} followed by one or more {@code JOB_UPDATED} will be aggregated into a single {@code jobAdded()} event,
 * with the job information from the last {@code JOB_UPDATED} notification<br>
 * - a {@code JOB_DISPATCHED} followed by a {@code JOB_RETURNED} will be discarded and will trigger no event 
 * @author Laurent Cohen
 * @since 5.1
 * @exclude
 */
class DeferredJobNotificationsHandler extends AbstractJobNotificationsHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DeferredJobNotificationsHandler.class);
  /**
   * This map represents the roots of the job notifications tree.
   */
  final Map<String, DriverNotif> driverMap = new HashMap<>();
  /**
   * Performs a timer-based periodic publication of the notifications.
   */
  final AbstractRefreshHandler refreshHandler;

  /**
   * Initialize with the specified job monitor.
   * @param monitor an instance of {@link JobMonitor}.
   * @param name the name given to this refresher and its timer thread.
   * @param period interval in milliseconds between refreshes.
   */
  DeferredJobNotificationsHandler(final JobMonitor monitor, final String name, final long period) {
    super(monitor);
    refreshHandler = new AbstractRefreshHandler(name, period) {
      @Override
      protected void performRefresh() {
        publish();
      }
    };
    refreshHandler.startRefreshTimer();
  }

  @Override
  void handleNotificationAsync(final JobNotification notif) {
    JobInformation jobInfo = notif.getJobInformation();
    JobDriver driver = monitor.getJobDriver(notif.getDriverUuid());
    DriverNotif driverNotif = getDriverNotif(driver);
    JPPFManagementInfo nodeInfo = notif.getNodeInfo();
    switch (notif.getEventType()) {
      case JOB_QUEUED:
        handleJobNotif(jobInfo, driverNotif, ADD);
        break;
      case JOB_ENDED:
        handleJobNotif(jobInfo, driverNotif, REMOVE);
        break;
      case JOB_UPDATED:
        handleJobNotif(jobInfo, driverNotif, UPDATE);
        break;
      case JOB_DISPATCHED:
        handleJobDispatchNotif(jobInfo, nodeInfo, driverNotif, ADD);
        break;
      case JOB_RETURNED:
        handleJobDispatchNotif(jobInfo, nodeInfo, driverNotif, REMOVE);
        break;
    }
  }

  /**
   * Publish the aggregated notifications as job monitor events. 
   */
  protected void publish() {
    try {
      Map<String, DriverNotif> tmpMap = new HashMap<>();
      synchronized (driverMap) {
        for (DriverNotif driverNotif : driverMap.values()) {
          DriverNotif tmpDriverNotif = new DriverNotif(driverNotif.driver);
          tmpDriverNotif.jobs = driverNotif.jobs;
          driverNotif.jobs = new HashMap<>();
          tmpMap.put(tmpDriverNotif.driver.getUuid(), tmpDriverNotif);
        }
      }
      if (tmpMap.isEmpty()) return;
      for (DriverNotif driverNotif : tmpMap.values()) {
        if (driverNotif.jobs.isEmpty()) continue;
        JobDriver driver = driverNotif.driver;
        Job job = null;
        for (JobNotif jn : driverNotif.jobs.values()) {
          switch (jn.type) {
            case ADD:
              job = new Job(jn.jobInformation);
              monitor.jobAdded(driver, job);
              break;

            case REMOVE:
              job = driver.getJob(jn.jobInformation.getJobUuid());
              if (job != null) monitor.jobRemoved(driver, job);
              break;

            case UPDATE:
              job = driver.getJob(jn.jobInformation.getJobUuid());
              if (monitor.isJobUpdated(job.getJobInformation(), jn.jobInformation)) {
                job.setJobInformation(jn.jobInformation);
                monitor.jobUpdated(driver, job);
              }
              break;
          }
          if (jn.dispatches.isEmpty()) continue;
          job = driver.getJob(jn.jobInformation.getJobUuid());
          if (job != null) {
            for (JobDispatchNotif jdn : jn.dispatches.values()) {
              TopologyNode node = monitor.getTopologyManager().getNode(jdn.nodeInformation.getUuid());
              switch (jdn.type) {
                case ADD:
                  if (node != null) monitor.dispatchAdded(driver, job, new JobDispatch(jdn.jobInformation, node));
                  break;

                case REMOVE:
                  if (node != null) {
                    JobDispatch dispatch = job.getJobDispatch(node.getUuid());
                    if (dispatch != null) monitor.dispatchRemoved(driver, job, dispatch);
                  }
                  break;
              }
            }
          }
          jn.dispatches.clear();
        }
        driverNotif.jobs.clear();
      }
      tmpMap.clear();
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Get the driver notif for the specified driver.
   * @param driver the driver.
   * @return a {@link DriverNotif} instance.
   */
  DriverNotif getDriverNotif(final JobDriver driver) {
    synchronized (driverMap) {
      DriverNotif notif = driverMap.get(driver.getUuid());
      if (notif == null) {
        notif = new DriverNotif(driver);
        driverMap.put(driver.getUuid(), notif);
      }
      return notif;
    }
  }

  /**
   * Handle a job-specific notification.
   * @param jobInfo information on the job.
   * @param driverNotif the driver to notify.
   * @param type the type of notification.
   */
  private void handleJobNotif(final JobInformation jobInfo, final DriverNotif driverNotif, final JobNotificationType type) {
    if (log.isTraceEnabled()) log.trace(String.format("type=%s, jobInfo=%s, driverNotif=%s", type, jobInfo, driverNotif));
    JobNotif jn = new JobNotif(jobInfo, type);
    synchronized (driverMap) {
      JobNotif oldJN = driverNotif.jobs.get(jobInfo.getJobUuid());
      if (oldJN == null) driverNotif.jobs.put(jobInfo.getJobUuid(), jn);
      else oldJN.merge(jn);
    }
  }

  /**
   * Handle a job dispatch-specific notification.
   * @param jobInfo information on the job.
   * @param nodeInfo information on the node the job is dispatched to.
   * @param driverNotif the driver to notify.
   * @param type the type of notification.
   */
  private void handleJobDispatchNotif(final JobInformation jobInfo, final JPPFManagementInfo nodeInfo, final DriverNotif driverNotif, final JobNotificationType type) {
    if (log.isTraceEnabled()) log.trace(String.format("type=%s, jobInfo=%s, nodeInfo=%s, driverNotif=%s", type, jobInfo, nodeInfo, driverNotif));
    synchronized (driverMap) {
      JobNotif jn = driverNotif.jobs.get(jobInfo.getJobUuid());
      if (jn == null) {
        if (type == REMOVE) {
          jn = new JobNotif(jobInfo, JobNotificationType.NONE);
          driverNotif.jobs.put(jobInfo.getJobUuid(), jn);
        } else return;
      }
      JobDispatchNotif jdn = new JobDispatchNotif(jobInfo, nodeInfo, type);
      JobDispatchNotif oldJDN = jn.dispatches.get(nodeInfo.getUuid());
      if (oldJDN == null) jn.dispatches.put(nodeInfo.getUuid(), jdn);
      else oldJDN.merge(jdn);
    }
  }

  /**
   * Types of action for aggregated notifications.
   */
  static enum JobNotificationType {
    /**
     * A job or dispatch was added.
     */
    ADD,
    /**
     * A job or dispatch was removed.
     */
    REMOVE,
    /**
     * A job or dispatch was updated.
     */
    UPDATE,
    /**
     * No action to take (no event is emitted).
     */
    NONE
  }

  /**
   * Represents an aggregated job / dispatch notification.
   * @param <N> the type of notification.
   */
  static abstract class AbstractJobNotification<N extends AbstractJobNotification> {
    /**
     * The job or dispatch information.
     */
    JobInformation jobInformation;
    /**
     * The type of notification.
     */
    JobNotificationType type;

    /**
     * Initialize this notification with the specified parameters.
     * @param jobInformation the job or dispatch information.
     * @param type the type of notification.
     */
    AbstractJobNotification(final JobInformation jobInformation, final JobNotificationType type) {
      this.jobInformation = jobInformation;
      this.type = type;
    }

    /**
     * Merge with a subsequent notification.
     * @param notif the other notif to merge with.
     */
    abstract void merge(final N notif);
  }

  /**
   * Represents a driver in the notifications hierarchy.
   */
  static class DriverNotif {
    /**
     * Mapping of job uuids to corresponding aggregated job notifications.
     */
    Map<String, JobNotif> jobs = new HashMap<>();
    /**
     * The corresponding driver.
     */
    JobDriver driver;

    /**
     * Initialize with the specified driver.
     * @param driver a {@link JobDriver} instance.
     */
    DriverNotif(final JobDriver driver) {
      this.driver = driver;
    }
  }

  /**
   * Aggregated job notification.
   */
  static class JobNotif extends AbstractJobNotification<JobNotif> {
    /**
     * Mapping of node uuids to corresponding aggregated dispatch notifications.
     */
    final Map<String, JobDispatchNotif> dispatches = new HashMap<>();

    /**
     * Initialize this notification with the specified parameters.
     * @param jobInformation the job or dispatch information.
     * @param type the type of notification.
     */
    JobNotif(final JobInformation jobInformation, final JobNotificationType type) {
      super(jobInformation, type);
    }

    @Override
    void merge(final JobNotif notif) {
      if ((type == ADD) && (notif.type == UPDATE)) type = ADD;
      else if ((type == ADD) && (notif.type == REMOVE)) type = NONE;
      else if ((type == REMOVE) && (notif.type == ADD)) type = NONE;
      else type = notif.type;
      jobInformation = notif.jobInformation;
    }
  }

  /**
   * Aggregated job dispatch notification.
   */
  static class JobDispatchNotif extends AbstractJobNotification<JobDispatchNotif> {
    /**
     * Information on the ndoe the job is dispatched to.
     */
    JPPFManagementInfo nodeInformation;

    /**
     * Initialize this notification with the specified parameters.
     * @param jobInformation the job or dispatch information.
     * @param nodeInformation information on the node the job is dispatched to.
     * @param type the type of notification.
     */
    JobDispatchNotif(final JobInformation jobInformation, final JPPFManagementInfo nodeInformation, final JobNotificationType type) {
      super(jobInformation, type);
      this.nodeInformation = nodeInformation;
    }

    @Override
    void merge(final JobDispatchNotif notif) {
      if ((type == ADD) && (notif.type == REMOVE)) type = NONE;
      else if ((type == REMOVE) && (notif.type == ADD)) type = NONE;
      else type = notif.type;
      jobInformation = notif.jobInformation;
      nodeInformation = notif.nodeInformation;
    }
  }
}
