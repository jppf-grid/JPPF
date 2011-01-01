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

package org.jppf.jca.demo;

import java.text.*;

import org.jppf.server.protocol.JPPFTask;

/**
 * Demonstration task to test the resource adaptor.
 * @author Laurent Cohen
 */
public class DurationTask extends JPPFTask
{
	/**
	 * Counts the number of times this task was run.
	 */
	private static int count = 0;
	/**
	 * A counter to be displayed.
	 */
	private int counter = 0;
	/**
	 * Duration of this task in seconds.
	 */
	private long duration = 1;

	/**
	 * Initialize this task withe specified duration.
	 * @param duration duration of this task in milliseconds.
	 */
	public DurationTask(long duration)
	{
		incrementCount();
		counter = count;
		this.duration = duration;
	}

	/**
	 * Run this task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		DecimalFormat nf = new DecimalFormat("0.###");
		String res = nf.format(duration/1000f);
		try
		{
			Thread.sleep(duration);
			String s = "JPPF task [" + getId() + "] successfully completed after " + res + " seconds";
			System.out.println(s);
			setResult(s);
		}
		catch (InterruptedException e)
		{
			setException(e);
			setResult("Exception for task [" + getId() + "] with specified duration of " + res + " seconds: " + e.getMessage());
		}
	}

	/**
	 * Increment the invocation count.
	 */
	private static synchronized void incrementCount()
	{
		count++;
	}
}
