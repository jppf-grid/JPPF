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

import java.util.*;

import javax.management.*;

import org.jppf.management.TaskExecutionNotification;
import org.jppf.management.forwarding.JPPFNodeForwardingNotification;
import org.slf4j.*;

import test.org.jppf.test.setup.BaseTest;

/**
 * A JMX {@link NotificationListener} which simply accumulates the notifications it receives.
 * @author Laurent Cohen
 */
public class NotifyingTaskListener implements NotificationListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NotifyingTaskListener.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The task information received as notifications from the node.
   */
  public List<Notification> notifs = new Vector<>();
  /**
   * An eventual exception that occurred in the {@link #handleNotification(Notification, Object)} method.
   */
  public Exception exception = null;
  /**
   * The count of user notifications sent via {@code Task.fireNotification()}.
   */
  public int taskExecutionUserNotificationCount = 0;
  /**
   * The count of JPPF notifications sent via {@code Task.fireNotification()}.
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
        } else {
          if ("NodeTest".equals(realNotif.getType())) {
            if (debugEnabled) log.debug("received test notification {}", realNotif);
            BaseTest.print(true, false, "client-side received notification:  type=%s, sequence=%d, userData=%s", realNotif.getType(), realNotif.getSequenceNumber(), realNotif.getUserData());
          }
        }
      }
    } catch (Exception e) {
      if (exception == null) exception = e;
    }
  }
}
