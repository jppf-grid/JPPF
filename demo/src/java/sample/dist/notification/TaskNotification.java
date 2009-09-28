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

package sample.dist.notification;

import java.io.Serializable;

/**
 * This class represents a sample notification sent by a task during its execution cycle.
 * It holds a task identifier along with a stage number and a corresponding notfication message.
 * @author Laurent Cohen
 */
public class TaskNotification implements Serializable
{
	/**
	 * The id of the task sending this notification.
	 */
	private int taskId = 0;
	/**
	 * The index of the stage the task is executing at this notification's creation.
	 */
	private int stage = 0;
	/**
	 * The message corresponding to the execution stage.
	 */
	private String message = null;

	/**
	 * Initialize this notification with the specified task id, stage number and message.
	 * @param taskId the id of the task sending this notification.
	 * @param stage the index of the stage the task is executing at this notification's creation.
	 * @param message the message corresponding to the execution stage.
	 */
	public TaskNotification(int taskId, int stage, String message)
	{
		super();
		this.taskId = taskId;
		this.stage = stage;
		this.message = message;
	}

	/**
	 * Get the id of the task sending this notification.
	 * @return  the task id as an int.
	 */
	public synchronized int getTaskId()
	{
		return taskId;
	}

	/**
	 * Get the index of the stage the task is executing at this notification's creation.
	 * @return the stage number as an int.
	 */
	public synchronized int getStage()
	{
		return stage;
	}

	/**
	 * Get the message corresponding to the execution stage.
	 * @return the message as a string.
	 */
	public synchronized String getMessage()
	{
		return message;
	}

	/**
	 * Get a string representation of this notification.
	 * @return a string representation of this notification's internal state.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return "[task: " + taskId + ", stage: " + stage + "] " + message;
	}
}
