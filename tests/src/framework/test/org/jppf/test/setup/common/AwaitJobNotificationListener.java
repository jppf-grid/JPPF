/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package test.org.jppf.test.setup.common;

import javax.management.*;

import org.jppf.client.JPPFClient;
import org.jppf.job.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.slf4j.*;

import test.org.jppf.test.setup.BaseSetup;

/**
 * This notification listener waits until a specified job lifecycle event is received.
 */
public class AwaitJobNotificationListener implements NotificationListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AwaitJobNotificationListener.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The expected event.
   */
  private final JobEventType expectedEvent;
  /**
   * Whether the epxcted event has been received.
   */
  private boolean eventReceived;
  /**
   * P'roxy to the job management MBean.
   */
  private final DriverJobManagementMBean jobManager;
  /**
   * Whether this listener was unregistered from the {@code await()} method.
   */
  private boolean listenerRemoved;

  /**
   * @param jobManager the mbean proxy to which to add a listener.
   * @param eventType the type of event to wait for.
   * @throws Exception if any error occurs.
   */
  public AwaitJobNotificationListener(final DriverJobManagementMBean jobManager, final JobEventType eventType) throws Exception {
    this.expectedEvent = eventType;
    this.eventReceived = false;
    this.listenerRemoved = false;
    this.jobManager = jobManager;
    this.jobManager.addNotificationListener(this, null, null);
  }

  /**
   * @param jmx represents the connection to the mbean proxy to which to add a listener.
   * @param eventType the type of event to wait for.
   * @throws Exception if any error occurs.
   */
  public AwaitJobNotificationListener(final JMXDriverConnectionWrapper jmx, final JobEventType eventType) throws Exception {
    this(jmx.getJobManager(), eventType);
  }

  /**
   * @param client the JPPF client.
   * @param eventType the type of event to wait for.
   * @throws Exception if any error occurs.
   */
  public AwaitJobNotificationListener(final JPPFClient client, final JobEventType eventType) throws Exception {
    this(BaseSetup.getJobManagementProxy(client), eventType);
  }

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    JobNotification jobNotif = (JobNotification) notification;
    JobInformation jobInfo = jobNotif.getJobInformation();
    if (debugEnabled) log.debug("job {} received event {}", jobInfo.getJobName(), jobNotif.getEventType());
    try {
      synchronized(this) {
        if (!eventReceived && (jobNotif.getEventType() == expectedEvent)) {
          if (debugEnabled) log.debug("job {} received expected event {}", jobInfo.getJobName(), expectedEvent);
          eventReceived = true;
          notifyAll();
        }
      }
    } catch (Exception ignore) {
      ignore.printStackTrace();
    }
  }

  /**
   * Wait for the specified event.
   * @throws Exception if any error occurs.
   */
  public synchronized void await() throws Exception {
    if (listenerRemoved) return;
    while (!eventReceived) wait(100L);
    if (debugEnabled) log.debug("finished waiting for expected event {}", expectedEvent);
    jobManager.removeNotificationListener(this);
    listenerRemoved = true;
    wait(100L);
  }

  /**
   * Determine whether this listener was unregistered from the {@code await()} method.
   * @return {@code true} if this listener was unregistered, {@code false} otherwise.
   */
  public synchronized boolean isListenerRemoved() {
    return listenerRemoved;
  }
}
