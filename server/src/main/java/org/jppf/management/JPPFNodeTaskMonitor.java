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

package org.jppf.management;

import java.util.concurrent.atomic.*;

import javax.management.*;

import org.jppf.node.Node;
import org.jppf.node.event.*;
import org.jppf.utils.concurrent.QueueHandler;
import org.slf4j.*;

/**
 * MBean implementation for task-level monitoring on each node.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFNodeTaskMonitor extends NotificationBroadcasterSupport implements JPPFNodeTaskMonitorMBean, TaskExecutionListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFNodeTaskMonitor.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The mbean object name sent with the notifications.
   */
  private ObjectName OBJECT_NAME;
  /**
   * The current count of tasks executed.
   */
  private int taskCount;
  /**
   * The current count of tasks executed.
   */
  private int taskInErrorCount;
  /**
   * The current count of tasks executed.
   */
  private int taskSuccessfulCount;
  /**
   * The current count of tasks executed.
   */
  private long totalCpuTime;
  /**
   * The current count of tasks executed.
   */
  private long totalElapsedTime;
  /**
   * The sequence number for notifications.
   */
  private final AtomicLong sequence = new AtomicLong(0L);
  /**
   * The jppf node which hosts this mbean.
   */
  final Node node;
  /**
   * 
   */
  final QueueHandler<NotificationSender> notificationHandler;

  /**
   * Default constructor.
   * @param node the jppf node which hosts this mbean.
   * @param objectName a string representing the MBean object name.
   */
  public JPPFNodeTaskMonitor(final Node node, final String objectName) {
    this.node = node;
    try {
      OBJECT_NAME = ObjectNameCache.getObjectName(objectName);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    this.notificationHandler = QueueHandler.<NotificationSender>builder()
      .named("NodeTaskMonitor")
      .handlingElementsAs(sender -> sender.run())
      .usingSingleDequuerThread()
      .build();
  }

  @Override
  public void taskExecuted(final TaskExecutionEvent event) {
    final TaskInformation info = event.getTaskInformation();
    synchronized(this) {
      taskCount++;
      if (info.hasError()) taskInErrorCount++;
      else taskSuccessfulCount++;
      totalCpuTime += info.getCpuTime();
      totalElapsedTime += info.getElapsedTime();
    }
    notificationHandler.offer(new NotificationSender(info, null, false));
  }

  @Override
  public void taskNotification(final TaskExecutionEvent event) {
    if (event.isSendViaJmx()) notificationHandler.offer(new NotificationSender(event.getTaskInformation(), event.getUserObject(), true));
  }

  /**
   * Get the total number of tasks executed by the node.
   * @return the number of tasks as an integer value.
   */
  @Override
  public synchronized Integer getTotalTasksExecuted() {
    return taskCount;
  }

  /**
   * The total cpu time used by the tasks in milliseconds.
   * @return the cpu time as long value.
   */
  @Override
  public synchronized Long getTotalTaskCpuTime() {
    return totalCpuTime;
  }

  /**
   * The total elapsed time used by the tasks in milliseconds.
   * @return the elapsed time as long value.
   */
  @Override
  public synchronized Long getTotalTaskElapsedTime() {
    return totalElapsedTime;
  }

  /**
   * The total number of tasks that ended in error.
   * @return the number as an integer value.
   */
  @Override
  public synchronized Integer getTotalTasksInError() {
    return taskInErrorCount;
  }

  /**
   * The total number of tasks that executed successfully.
   * @return the number as an integer value.
   */
  @Override
  public synchronized Integer getTotalTasksSucessfull() {
    return taskSuccessfulCount;
  }

  @Override
  public synchronized void reset() {
    this.taskCount = 0;
    this.taskInErrorCount = 0;
    this.taskSuccessfulCount = 0;
    this.totalCpuTime = 0L;
    this.totalElapsedTime = 0L;
  }

  @Override
  public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) {
    super.addNotificationListener(listener, filter, handback);
    if (debugEnabled) log.debug("registered notification listener = {}, filter = {}, handback = {}", listener, filter, handback);
  }

  @Override
  public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
    super.removeNotificationListener(listener);
    if (debugEnabled) log.debug("un registered notification listener = {}", listener);
  }

  @Override
  public void removeNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws ListenerNotFoundException {
    super.removeNotificationListener(listener, filter, handback);
    if (debugEnabled) log.debug("un registered notification listener = {}, filter = {}, handback = {}", listener, filter, handback);
  }

  /**
   * 
   */
  private class NotificationSender implements Runnable {
    /**
     * Information about the task for which this notification is created.
     */
    private final TaskInformation info;
    /**
     * User-defined object sent along with the notification.
     */
    private final Object userObject;
    /**
     * Determines whether this is a user-defined notification sent from a task.
     */
    private final boolean userNotification;

    /**
     * 
     * @param info the info to send.
     * @param userObject a user-defined object sent along with the notification.
     * @param userNotification determines whether this is a user-defined notification sent from a task.
     */
    public NotificationSender(final TaskInformation info, final Object userObject, final boolean userNotification) {
      this.info = info;
      this.userObject = userObject;
      this.userNotification = userNotification;
    }

    @Override
    public void run() {
      if (debugEnabled) log.debug("sending task notification with userObject={}, info={}", userObject, info);
      sendNotification(new TaskExecutionNotification(OBJECT_NAME, sequence.incrementAndGet(), info, userObject, userNotification));
    }
  }
}
