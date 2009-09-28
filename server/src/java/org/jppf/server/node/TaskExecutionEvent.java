/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.server.node;

import java.util.EventObject;

import org.jppf.management.TaskInformation;
import org.jppf.server.protocol.JPPFTask;

/**
 * Instances of this class represent events that occur during the life span of an individual JPPF task.
 * @author Laurent Cohen
 */
public class TaskExecutionEvent extends EventObject
{
	/**
	 * Object encapsulating information about the task.
	 */
	private TaskInformation taskInformation = null;

	/**
	 * Initialize this event object with the specified task.
	 * @param task - the JPPF task from which the event originates.
	 * @param cpuTime - the cpu time taken by the task.
	 * @param elapsedTime - the wall clock time taken by the task.
	 * @param error - determines whether the task had an exception.
	 */
	public TaskExecutionEvent(JPPFTask task, long cpuTime, long elapsedTime, boolean error)
	{
		super(task);
		this.taskInformation = new TaskInformation(task.getId(), cpuTime, elapsedTime, error);
	}

	/**
	 * Get the JPPF task from which the event originates.
	 * @return a <code>JPPFTask</code> instance.
	 */
	public JPPFTask getTask()
	{
		return (JPPFTask) getSource();
	}

	/**
	 * Get the object encapsulating information about the task.
	 * @return a <code>TaskInformation</code> instance.
	 */
	public TaskInformation getTaskInformation()
	{
		return taskInformation;
	}
}
