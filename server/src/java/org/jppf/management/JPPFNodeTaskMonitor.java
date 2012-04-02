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

package org.jppf.management;

import java.util.concurrent.*;

import javax.management.*;

import org.jppf.server.node.*;
import org.slf4j.*;

/**
 * MBean implementation for task-level monitoring on each node.
 * @author Laurent Cohen
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
  //private AtomicInteger taskCount = new AtomicInteger(0);
  private int taskCount = 0;
  /**
   * The current count of tasks executed.
   */
  //private AtomicInteger taskInErrorCount = new AtomicInteger(0);
  private int taskInErrorCount = 0;
  /**
   * The current count of tasks executed.
   */
  //private AtomicInteger taskSuccessfulCount = new AtomicInteger(0);
  private int taskSucessfullCount = 0;
  /**
   * The current count of tasks executed.
   */
  //private AtomicLong totalCpuTime = new AtomicLong(0L);
  private long totalCpuTime = 0L;
  /**
   * The current count of tasks executed.
   */
  //private AtomicLong totalElapsedTime = new AtomicLong(0L);
  private long totalElapsedTime = 0L;
  /**
   * The sequence number for notifications.
   */
  //private AtomicLong sequence = new AtomicLong(0L);
  private long sequence = 0L;
  /**
   * The count of notification listeners registered with this MBean.
   */
  private long listenerCount = 0L;
  /**
   * 
   */
  private ExecutorService executor = Executors.newSingleThreadExecutor();

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
   * @see org.jppf.server.node.TaskExecutionListener#taskExecuted(org.jppf.server.node.TaskExecutionEvent)
   */
  @Override
  public synchronized void taskExecuted(final TaskExecutionEvent event)
  {
    TaskInformation info = event.getTaskInformation();
    /*
		taskCount.incrementAndGet();
		if (info.hasError()) taskInErrorCount.incrementAndGet();
		else taskSucessfullCount.incrementAndGet();
		totalCpuTime.addAndGet(info.getCpuTime());
		totalElapsedTime.addAndGet(info.getElapsedTime());
		sendNotification(new TaskExecutionNotification(OBJECT_NAME, sequence.getAndIncrement(), info));
     */
    taskCount++;
    if (info.hasError()) taskInErrorCount++;
    else taskSucessfullCount++;
    totalCpuTime += info.getCpuTime();
    totalElapsedTime += info.getElapsedTime();
    //if (listenerCount > 0) sendNotification(new TaskExecutionNotification(OBJECT_NAME, ++sequence, info));
    if (listenerCount > 0) executor.submit(new NotificationSender(info));
  }

  /**
   * Get the total number of tasks executed by the node.
   * @return the number of tasks as an integer value.
   * @see org.jppf.management.JPPFNodeTaskMonitorMBean#getTotalTasksExecuted()
   */
  @Override
  public synchronized Integer getTotalTasksExecuted()
  {
    //return taskCount.get();
    return taskCount;
  }

  /**
   * The total cpu time used by the tasks in milliseconds.
   * @return the cpu time as long value.
   * @see org.jppf.management.JPPFNodeTaskMonitorMBean#getTotalTaskCpuTime()
   */
  @Override
  public synchronized Long getTotalTaskCpuTime()
  {
    //return totalCpuTime.get();
    return totalCpuTime;
  }

  /**
   * The total elapsed time used by the tasks in milliseconds.
   * @return the elapsed time as long value.
   * @see org.jppf.management.JPPFNodeTaskMonitorMBean#getTotalTaskElapsedTime()
   */
  @Override
  public synchronized Long getTotalTaskElapsedTime()
  {
    //return totalElapsedTime.get();
    return totalElapsedTime;
  }

  /**
   * The total number of tasks that ended in error.
   * @return the number as an integer value.
   * @see org.jppf.management.JPPFNodeTaskMonitorMBean#getTotalTasksInError()
   */
  @Override
  public synchronized Integer getTotalTasksInError()
  {
    //return taskInErrorCount.get();
    return taskInErrorCount;
  }

  /**
   * The total number of tasks that executed successfully.
   * @return the number as an integer value.
   * @see org.jppf.management.JPPFNodeTaskMonitorMBean#getTotalTasksSucessfull()
   */
  @Override
  public synchronized Integer getTotalTasksSucessfull()
  {
    //return taskSuccessfulCount.get();
    return taskSucessfullCount;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback)
  {
    synchronized(this)
    {
      super.addNotificationListener(listener, filter, handback);
      listenerCount++;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException
  {
    synchronized(this)
    {
      super.removeNotificationListener(listener);
      listenerCount--;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws ListenerNotFoundException
  {
    super.removeNotificationListener(listener, filter, handback);
    synchronized(this)
    {
      listenerCount--;
    }
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {
      sendNotification(new TaskExecutionNotification(OBJECT_NAME, ++sequence, info));
    }
  }
}
