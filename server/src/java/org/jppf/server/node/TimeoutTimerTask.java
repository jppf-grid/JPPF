/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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
	 * The future on which to call the cancel() method.
	 */
	private Future<?> future = null;
	/**
	 * The task to cancel.
	 */
	private JPPFTask task = null;

	/**
	 * Initialize this timer task with the specified future.
	 * @param future the future used to cancel the underlying JPPF task.
	 * @param task the task to cancel.
	 */
	public TimeoutTimerTask(Future<?> future, JPPFTask task)
	{
		this.future = future;
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
			//task.setException(new JPPFException("This task has timed out"));
		}
	}
}