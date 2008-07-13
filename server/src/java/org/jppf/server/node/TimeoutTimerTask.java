/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.server.node;

import java.util.TimerTask;
import java.util.concurrent.Future;

import org.jppf.server.protocol.JPPFTask;

/**
 * Instances of this class are scheduled by a timer to execute one time, check
 * whether the corresponding JPPF task timeout has been reached, and abort the
 * task if necessary.
 */
public class TimeoutTimerTask extends TimerTask
{
	/**
	 * The number identifying the task.
	 */
	private long number = 0L;
	/**
	 * The future on which to call the cancel() method.
	 */
	private Future<?> future = null;
	/**
	 * The task to cancel.
	 */
	private JPPFTask task = null;
	/**
	 * The execution manager that started this task.
	 */
	private NodeExecutionManager executionManager = null;

	/**
	 * Initialize this timer task with the specified future.
	 * @param executionManager the execution manager that started this task.
	 * @param number the number identifying the task.
	 * @param task the task to cancel.
	 */
	public TimeoutTimerTask(NodeExecutionManager executionManager, long number, JPPFTask task)
	{
		this.number = number;
		this.future = executionManager.getFutureFromNumber(number);
		this.task = task;
	}

	/**
	 * Execute this task.
	 * @see java.util.TimerTask#run()
	 */
	public void run()
	{
		if (!future.isDone())
		{
			future.cancel(true);
			task.onTimeout();
			executionManager.removeFuture(number);
		}
	}
}
