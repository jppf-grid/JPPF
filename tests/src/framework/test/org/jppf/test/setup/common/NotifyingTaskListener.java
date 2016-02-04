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

import java.util.*;

import javax.management.*;

import org.jppf.management.TaskExecutionNotification;
import org.jppf.management.forwarding.JPPFNodeForwardingNotification;

/**
 * A JMX {@link NotificationListener} which simply accumulates the notifications it receives.
 * @author Laurent Cohen
 */
public class NotifyingTaskListener implements NotificationListener {
  /**
   * The task information received as notifications from the node.
   */
  public List<Notification> notifs = new Vector<>();
  /**
   * 
   */
  public Exception exception = null;
  /**
   * 
   */
  public int taskExecutionUserNotificationCount = 0;
  /**
   * 
   */
  public int taskExecutionJppfNotificationCount = 0;
  /**
   * List of received user objects.
   */
  public List<Object> userObjects = new Vector<>();

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    try {
      notifs.add(notification);
      if (notification instanceof JPPFNodeForwardingNotification) {
        Notification realNotif = ((JPPFNodeForwardingNotification) notification).getNotification();
        if (realNotif instanceof TaskExecutionNotification) {
          TaskExecutionNotification notif = (TaskExecutionNotification) realNotif;
          if (notif.isUserNotification()) taskExecutionUserNotificationCount++;
          else taskExecutionJppfNotificationCount++;
          Object o = notif.getUserData();
          if (o != null) userObjects.add(o);
        }
      }
    } catch (Exception e) {
      if (exception == null) exception = e;
    }
  }
}
