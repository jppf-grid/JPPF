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

package test.org.jppf.test.setup.common;

import javax.management.*;

import org.jppf.client.JPPFClient;
import org.jppf.job.*;
import org.jppf.server.job.management.DriverJobManagementMBean;

import test.org.jppf.test.setup.BaseSetup;

/**
 * This notification listener waits until a specified job lifecycle event is received.
 */
public class AwaitJobNotificationListener implements NotificationListener {
  /**
   * The expected event.
   */
  private JobEventType expectedEvent = JobEventType.JOB_DISPATCHED;
  /**
   * 
   */
  private final DriverJobManagementMBean jobManager;
  /**
   * Whether this listener was unregistered from the {@code await()} method.
   */
  private boolean listenerRemoved = false;

  /**
   * 
   * @param client the JPPF client.
   * @throws Exception if any error occurs.
   */
  public AwaitJobNotificationListener(final JPPFClient client) throws Exception {
    jobManager = BaseSetup.getJobManagementProxy(client);
    jobManager.addNotificationListener(this, null, null);
  }

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    JobNotification jobNotif = (JobNotification) notification;
    try {
      synchronized(this) {
        if (jobNotif.getEventType() == expectedEvent) notifyAll();
      }
    } catch (Exception ignore) {
      ignore.printStackTrace();
    }
  }

  /**
   * Await the specified event.
   * @param eventType the type of event to wait for.
   * @throws Exception if any error occurs.
   */
  public synchronized void await(final JobEventType eventType) throws Exception {
    this.expectedEvent = eventType;
    wait();
    jobManager.removeNotificationListener(this);
    listenerRemoved = true;
  }

  /**
   * Determine whether this listener was unregistered from the {@code await()} method.
   * @return {@code true} if this listener was unregistered, {@code false} otherwise.
   */
  public boolean isListenerRemoved() {
    return listenerRemoved;
  }
}