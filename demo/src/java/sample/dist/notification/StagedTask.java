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

import org.jppf.server.protocol.JPPFTask;

/**
 * Instances of this class simulate the execution of a number of stages of equal duration.
 * @author Laurent Cohen
 */
public class StagedTask extends JPPFTask
{
	/**
	 * The identifier of this task.
	 */
	private int taskId = 0;
	/**
	 * The number of stages to simulate.
	 */
	private int nbStages = 0;
	/**
	 * The duration of a stage.
	 */
	private long interval = 0L;

	/**
	 * Initialize this task with the specified task id, number of stages and stage duration.
	 * @param taskId the identifier of this task.
	 * @param nbStages the number of stages to simulate.
	 * @param interval the duration of a stage.
	 */
	public StagedTask(int taskId, int nbStages, long interval)
	{
		this.taskId = taskId;
		setId(""+taskId);
		this.nbStages = nbStages;
		this.interval = interval;
	}

	/**
	 * Execute this task's work.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		for (int i=1; i<= nbStages; i++)
		{
			// Notify that we are starting the next stage
			fireNotification(new TaskNotification(taskId, i, "Starting stage " + i));
			try
			{
				Thread.sleep(interval);
			}
			catch(InterruptedException ignored)
			{
			}
		}
		// Notify of the completion of this task.
		fireNotification(new TaskNotification(taskId, -1, "Task execution complete"));
	}

	/**
	 * Get the identifier of this task.
	 * @return the identifier as an int.
	 */
	public synchronized int getTaskId()
	{
		return taskId;
	}
}
