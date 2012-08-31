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

package org.jppf.example.tasknotifications.mbean;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.*;


/**
 * Implementation of the TaskNotificationsMBean interface.
 * @author Laurent Cohen
 */
public class TaskNotifications extends NotificationBroadcasterSupport implements TaskNotificationsMBean
{
  /**
   * Sequence number sent with each notification.
   */
  private static AtomicLong sequence = new AtomicLong(0L);

  /**
   * Initialize this MBean.
   */
  public TaskNotifications()
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendTaskNotification(final String message)
  {
    Notification notif = new Notification("task.notification", this,
        sequence.incrementAndGet(), System.currentTimeMillis(), message);
    sendNotification(notif);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendTaskNotification(final String message, final Object userData)
  {
    Notification notif = new Notification("task.notification", this,
        sequence.incrementAndGet(), System.currentTimeMillis(), message);
    // add the user data
    notif.setUserData(userData);
    sendNotification(notif);
  }
}
