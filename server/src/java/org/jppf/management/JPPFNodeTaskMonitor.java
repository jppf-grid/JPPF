/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.util.concurrent.*;

import javax.management.*;

import org.jppf.node.event.*;
import org.jppf.utils.JPPFThreadFactory;
import org.slf4j.*;

/**
 * MBean implementation for task-level monitoring on each node.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFNodeTaskMonitor extends NotificationBroadcasterSupport implements JPPFNodeTaskMonitorMBean, TaskExecutionListener
{
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
  private int taskCount = 0;
  /**
   * The current count of tasks executed.
   */
  private int taskInErrorCount = 0;
  /**
   * The current count of tasks executed.
   */
  private int taskSucessfullCount = 0;
  /**
   * The current count of tasks executed.
   */
  private long totalCpuTime = 0L;
  /**
   * The current count of tasks executed.
   */
  private long totalElapsedTime = 0L;
  /**
   * The sequence number for notifications.
   */
  private long sequence = 0L;
  /**
   * 
   */
  private ExecutorService executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("NodeTaskNonitor"));

  /**
   * Default constructor.
   * @param objectName a string representing the MBean object name.
   */
  public JPPFNodeTaskMonitor(final String objectName)
  {
    try
    {
      OBJECT_NAME = new ObjectName(objectName);
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Called to notify a listener that a task was executed.
   * @param event the event encapsulating the task-related data.
   */
  @Override
  public synchronized void taskExecuted(final TaskExecutionEvent event)
  {
    TaskInformation info = event.getTaskInformation();
    taskCount++;
    if (info.hasError()) taskInErrorCount++;
    else taskSucessfullCount++;
    totalCpuTime += info.getCpuTime();
    totalElapsedTime += info.getElapsedTime();
    executor.submit(new NotificationSender(info));
  }

  /**
   * Get the total number of tasks executed by the node.
   * @return the number of tasks as an integer value.
   */
  @Override
  public synchronized Integer getTotalTasksExecuted()
  {
    return taskCount;
  }

  /**
   * The total cpu time used by the tasks in milliseconds.
   * @return the cpu time as long value.
   */
  @Override
  public synchronized Long getTotalTaskCpuTime()
  {
    return totalCpuTime;
  }

  /**
   * The total elapsed time used by the tasks in milliseconds.
   * @return the elapsed time as long value.
   */
  @Override
  public synchronized Long getTotalTaskElapsedTime()
  {
    return totalElapsedTime;
  }

  /**
   * The total number of tasks that ended in error.
   * @return the number as an integer value.
   */
  @Override
  public synchronized Integer getTotalTasksInError()
  {
    return taskInErrorCount;
  }

  /**
   * The total number of tasks that executed successfully.
   * @return the number as an integer value.
   */
  @Override
  public synchronized Integer getTotalTasksSucessfull()
  {
    return taskSucessfullCount;
  }

  @Override
  public synchronized void reset()
  {
    this.taskCount = 0;
    this.taskInErrorCount = 0;
    this.taskSucessfullCount = 0;
    this.totalCpuTime = 0L;
    this.totalElapsedTime = 0L;
  }

  /**
   * 
   */
  private class NotificationSender implements Runnable
  {
    /**
     * 
     */
    private final TaskInformation info;

    /**
     * 
     * @param info the info to send.
     */
    public NotificationSender(final TaskInformation info)
    {
      this.info = info;
    }

    @Override
    public void run()
    {
      sendNotification(new TaskExecutionNotification(OBJECT_NAME, ++sequence, info));
    }
  }
}
