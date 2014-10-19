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

import javax.management.*;

import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.job.*;
import org.slf4j.*;

/**
 * Implementation of a notification listener for processing of job events.
 */
public class JobNotificationListener implements NotificationListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JobNotificationListener.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The driver that sends the notifications.
   */
  private final TopologyDriver driver;
  /**
   * The panel to which the notifications are delegated.
   */
  private final JobDataPanel jobDataPanel;

  /**
   * Initialize this listener with the specified driver name.
   * @param driver the driver that sends the notifications.
   * @param jobDataPanel the panel to which the notifications are delegated.
   */
  public JobNotificationListener(final JobDataPanel jobDataPanel, final TopologyDriver driver) {
    this.driver = driver;
    this.jobDataPanel = jobDataPanel;
  }

  /**
   * Handle notifications of job events.
   * @param notification encapsulates the job event ot handle.
   * @param handback not used.
   */
  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    if (!(notification instanceof JobNotification)) return;
    JobNotification notif = (JobNotification) notification;
    if (log.isDebugEnabled() && (notif.getEventType() == JobEventType.JOB_ENDED)) log.debug("job removed notif: {}", notif);
    if (traceEnabled) log.trace("driver " + driver + " received notification: " + notif);
    jobDataPanel.handleNotification(driver, notif);
  }
}
