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

package org.jppf.node.event;

import java.util.EventObject;

import org.jppf.management.TaskInformation;
import org.jppf.node.protocol.Task;

/**
 * Instances of this class represent events that occur during the life span of an individual JPPF task.
 * @author Laurent Cohen
 */
public class TaskExecutionEvent extends EventObject
{
  /**
   * Object encapsulating information about the task.
   */
  private final TaskInformation taskInformation;
  /**
   * User-defined object to send as part of the notification.
   */
  private final Object userObject;
  /**
   * If <code>true</code> then also send this notification via the JMX MBean, otherwise only send to local listeners.
   */
  private final boolean sendViaJmx;
  /**
   * Whether this is a task completion or user-sent event.
   */
  private final boolean taskCompletion;

  /**
   * Initialize this event object with the specified task.
   * This constructor is used by the JPPF node when sending task completion notifications.
   * @param task the JPPF task from which the event originates.
   * @param jobId the id of the job this task belongs to.
   * @param jobName the name of the job this task belongs to.
   * @param cpuTime the cpu time taken by the task.
   * @param elapsedTime the wall clock time taken by the task.
   * @param error determines whether the task had an exception.
   */
  public TaskExecutionEvent(final Task<?> task, final String jobId, final String jobName, final long cpuTime, final long elapsedTime, final boolean error)
  {
    super(task);
    this.taskInformation = new TaskInformation(task.getId(), jobId, jobName, cpuTime, elapsedTime, error);
    this.userObject = null;
    this.sendViaJmx = true;
    taskCompletion = true;
  }

  /**
   * Initialize this event object with the specified task.
   * This constructor is used when sending user-ddefined notifications from the tasks.
   * @param task the JPPF task from which the event originates.
   * @param jobId the id of the job this task belongs to.
   * @param jobName the name of the job this task belongs to.
   * @param userObject a user-defined object to send as part of the notification.
   * @param sendViaJmx if <code>true</code> then also send this notification via the JMX MBean, otherwise only send to local listeners.
   * @since 4.0
   */
  public TaskExecutionEvent(final Task<?> task, final String jobId, final String jobName, final Object userObject, final boolean sendViaJmx)
  {
    super(task);
    this.taskInformation = new TaskInformation(task.getId(), jobId, jobName, -1, -1, false);
    this.userObject = userObject;
    this.sendViaJmx = sendViaJmx;
    taskCompletion = false;
  }

  /**
   * Get the JPPF task from which the event originates.
   * @return a <code>Task</code> instance.
   */
  public Task<?> getTask()
  {
    return (Task<?>) getSource();
  }

  /**
   * Get the object encapsulating information about the task.
   * @return a <code>TaskInformation</code> instance.
   */
  public TaskInformation getTaskInformation()
  {
    return taskInformation;
  }

  /**
   * Get the user-defined object to send as part of the notification.
   * @return the object specified in the constructor or <code>null</code>.
   * @since 4.0
   */
  public Object getUserObject()
  {
    return userObject;
  }

  /**
   * If <code>true</code> then also send this notification via the JMX MBean, otherwise only send to local listeners.
   * @return <code>true</code> if this notification should be sent via JMX, <code>false</code> otherwise.
   * @since 4.0
   */
  public boolean isSendViaJmx()
  {
    return sendViaJmx;
  }

  /**
   * Determine whether this is a task completion or user-sent event.
   * @return <code>true</code> if this is a task completion event, <code>false</code> otherwise.
   * @since 4.0
   * @exclude
   */
  public boolean isTaskCompletion()
  {
    return taskCompletion;
  }

  /**
   * Determine whether this is a user-sent event.
   * @return <code>true</code> if this is a task completion event, <code>false</code> otherwise.
   * @since 4.1
   */
  public boolean isUserNotification()
  {
    return !taskCompletion;
  }
}
