/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
package org.jppf.client.loadbalancer;

import org.jppf.JPPFException;
import org.jppf.server.protocol.JPPFTask;

/**
 * JPPF task wrapper used to catch unhandled exceptions.
 */
public class TaskWrapper implements Runnable
{
	/**
	 * The JPPF task to run.
	 */
	private JPPFTask task = null;

	/**
	 * Initialize this task wrapper with the specified JPPF task.
	 * @param task the JPPF task to execute.
	 */
	public TaskWrapper(JPPFTask task)
	{
		this.task = task;
	}

	/**
	 * Run the task and handle uncaught exceptions.
	 * @see java.lang.Runnable#run()
	 */
	@Override
    public void run()
	{
		try
		{
			task.run();
		}
		catch(Throwable t)
		{
			if (t instanceof Exception) task.setException((Exception) t);
			else task.setException(new JPPFException(t));
		}
	}
}
