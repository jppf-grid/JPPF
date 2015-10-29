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

package org.jppf.management;

import javax.management.*;

/**
 * Notification sent to provide information about a task that was executed in a node.
 * @author Laurent Cohen
 */
public class TaskExecutionNotification extends Notification {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Information about the task that triggered this notification.
   */
  private final TaskInformation taskInformation;
  /**
   * Determines whether this is a user-defined notification sent from a task.
   */
  private final boolean userNotification;

  /**
   * Initialize this notification with the specified parameters.
   * @param source the emitter MBean's object name.
   * @param sequenceNumber the notification sequence number.
   * @param taskInformation information about the task that triggered this notification.
   * @param userObject an user-defined object sent along with this notification.
   * @param userNotification determines whether this is a user-defined notification sent from a task.
   */
  public TaskExecutionNotification(final ObjectName source, final long sequenceNumber, final TaskInformation taskInformation, final Object userObject, final boolean userNotification) {
    super("task.monitor", source, sequenceNumber, taskInformation.getTimestamp());
    this.taskInformation = taskInformation;
    this.userNotification = userNotification;
    setUserData(userObject);
  }

  /**
   * Get the object encapsulating information about the task.
   * @return a <code>TaskInformation</code> instance.
   */
  public TaskInformation getTaskInformation() {
    return taskInformation;
  }

  /**
   * Determine whether this is a user-defined notification sent from a task.
   * @return {@code true} if this is a user notification, {@code false} otherwise.
   * @since 4.1
   */
  public boolean isUserNotification() {
    return userNotification;
  }
}
