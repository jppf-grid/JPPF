/*
 * JPPF.
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
package org.jppf.utils;

import org.apache.commons.logging.*;

/**
 * Task that is submitted for each received notification.
 */
public abstract class SynchronizedTask implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(SynchronizedTask.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * An object to synchronize against.
	 */
	Object sync = null;

	/**
	 * Initialize this task with an object to synchronize against.
	 * @param sync - an object to synchronize against.
	 */
	public SynchronizedTask(Object sync)
	{
		this.sync = sync;
	}

	/**
	 * Wrap the task execution within a synchronized block within a try/catch block.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			synchronized(sync)
			{
				perform();
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Execute the task.
	 * @throws Exception if any error occurs.
	 */
	public abstract void perform() throws Exception;
}
