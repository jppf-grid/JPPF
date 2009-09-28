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

package test.priority;

import org.jppf.server.protocol.JPPFTask;

/**
 * This task simply waits a specified time.
 * @author Laurent Cohen
 */
public class WaitTask extends JPPFTask
{
	/**
	 * The time to wait.
	 */
	private long time = 0L;

	/**
	 * Initialize this time with the specified time to wait.
	 * @param time the time to wait, in milliseconds.
	 */
	public WaitTask(long time)
	{
		this.time = time;
	}

	/**
	 * Execute this task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			Thread.sleep(time);
			System.out.println("waited " + time + " ms");
		}
		catch(InterruptedException e)
		{
			setException(e);
		}
	}
}
