/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import org.jppf.client.monitoring.topology.TopologyNode;
import org.jppf.job.*;
import org.jppf.management.JPPFManagementInfo;
import org.slf4j.*;

/**
 * This class publishes each JMX notification as a job monitor event.
 * @author Laurent Cohen
 * @since 5.1
 * @exclude
 */
class ImmediateJobNotificationsHandler extends AbstractJobNotificationsHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ImmediateJobNotificationsHandler.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();

  /**
   * Initialize with the specified job monitor.
   * @param monitor an instance of {@link JobMonitor}.
   */
  ImmediateJobNotificationsHandler(final JobMonitor monitor) {
    super(monitor);
  }

  @Override
  void handleNotificationAsync(final JobNotification notif) {
    try {
      if (traceEnabled) log.trace("handling {} notification: {}", notif.getEventType(), notif);
      final JobInformation jobInfo = notif.getJobInformation();
      final JobDriver driver = monitor.getJobDriver(notif.getDriverUuid());
      if (driver == null) return;
      final Job job = driver.getJob(jobInfo.getJobUuid());
      final JPPFManagementInfo nodeInfo = notif.getNodeInfo();
      final TopologyNode node = (nodeInfo == null) ? null : (TopologyNode) driver.getTopologyDriver().getChild(nodeInfo.getUuid());
      switch (notif.getEventType()) {
        case JOB_QUEUED:
          if (debugEnabled) log.debug("job queued: {}", jobInfo);
          monitor.jobAdded(driver, new Job(jobInfo));
          break;
  
        case JOB_ENDED:
          if (debugEnabled) log.debug("job ended: {}", jobInfo);
          monitor.jobRemoved(driver, job);
          break;
  
        case JOB_UPDATED:
          if ((job != null) && monitor.isJobUpdated(job.getJobInformation(), jobInfo)) {
            job.setJobInformation(jobInfo);
            monitor.jobUpdated(driver, job);
          }
          break;
  
        case JOB_DISPATCHED:
          if (node != null) monitor.dispatchAdded(driver, job, new JobDispatch(jobInfo, node));
          break;
  
        case JOB_RETURNED:
          if ((node != null) && (job != null)) monitor.dispatchRemoved(driver, job, job.getJobDispatch(node.getUuid()));
          break;
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
