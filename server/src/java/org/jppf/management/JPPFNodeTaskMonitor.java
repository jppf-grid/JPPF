/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

import org.apache.commons.logging.*;
import org.jppf.server.node.*;

/**
 * MBean implementation for task-level monitoring on each node.
 * @author Laurent Cohen
 */
public class JPPFNodeTaskMonitor extends NotificationBroadcasterSupport implements JPPFNodeTaskMonitorMBean, TaskExecutionListener
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFNodeTaskMonitor.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The mbrean object name sent with the notifications.
	 */
	private ObjectName OBJECT_NAME;
	/**
	 * The current count of tasks executed.
	 */
	private AtomicInteger taskCount = new AtomicInteger(0);
	/**
	 * The current count of tasks executed.
	 */
	private AtomicInteger taskInErrorCount = new AtomicInteger(0);
	/**
	 * The current count of tasks executed.
	 */
	private AtomicInteger taskSucessfullCount = new AtomicInteger(0);
	/**
	 * The current count of tasks executed.
	 */
	private AtomicLong totalCpuTime = new AtomicLong(0L);
	/**
	 * The current count of tasks executed.
	 */
	private AtomicLong totalElapsedTime = new AtomicLong(0L);
	/**
	 * The sequence number for notifications.
	 */
	private AtomicLong sequence = new AtomicLong(0L);

	/**
	 * Default constructor.
	 * @param objectName a string representing the MBean object name.
	 */
	public JPPFNodeTaskMonitor(String objectName)
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
	public void taskExecuted(TaskExecutionEvent event)
	{
		TaskInformation info = event.getTaskInformation();
		taskCount.incrementAndGet();
		if (info.hasError()) taskInErrorCount.incrementAndGet();
		else taskSucessfullCount.incrementAndGet();
		totalCpuTime.addAndGet(info.getCpuTime());
		totalElapsedTime.addAndGet(info.getElapsedTime());
		sendNotification(new TaskExecutionNotification(OBJECT_NAME, sequence.getAndIncrement(), info));
	}

	/**
	 * Get the total number of tasks executed by the node.
	 * @return the number of tasks as an integer value.
	 * @see org.jppf.management.JPPFNodeTaskMonitorMBean#getTotalTasksExecuted()
	 */
	public Integer getTotalTasksExecuted()
	{
		return taskCount.get();
	}

	/**
	 * The total cpu time used by the tasks in milliseconds.
	 * @return the cpu time as long value.
	 * @see org.jppf.management.JPPFNodeTaskMonitorMBean#getTotalTaskCpuTime()
	 */
	public Long getTotalTaskCpuTime()
	{
		return totalCpuTime.get();
	}

	/**
	 * The total elapsed time used by the tasks in milliseconds.
	 * @return the elapsed time as long value.
	 * @see org.jppf.management.JPPFNodeTaskMonitorMBean#getTotalTaskElapsedTime()
	 */
	public Long getTotalTaskElapsedTime()
	{
		return totalElapsedTime.get();
	}

	/**
	 * The total number of tasks that ended in error.
	 * @return the number as an integer value.
	 * @see org.jppf.management.JPPFNodeTaskMonitorMBean#getTotalTasksInError()
	 */
	public Integer getTotalTasksInError()
	{
		return taskInErrorCount.get();
	}

	/**
	 * The total number of tasks that executed sucessfully.
	 * @return the number as an integer value.
	 * @see org.jppf.management.JPPFNodeTaskMonitorMBean#getTotalTasksSucessfull()
	 */
	public Integer getTotalTasksSucessfull()
	{
		return taskSucessfullCount.get();
	}
}
