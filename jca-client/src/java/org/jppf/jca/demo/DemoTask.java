/*
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

package org.jppf.jca.demo;

import org.jppf.server.protocol.JPPFTask;

/**
 * Demonstration task to test the resource adaptor.
 * @author Laurent Cohen
 */
public class DemoTask extends JPPFTask
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
	 * Default constructor.
	 */
	public DemoTask()
	{
		incrementCount();
		counter = count;
	}

	/**
	 * Run this task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		String s = "***** Hello JPPF !!! [" + counter + "] *****";
		System.out.println(s);
		setResult(s);
	}

	/**
	 * Increment the invocation count.
	 */
	private static synchronized void incrementCount()
	{
		count++;
	}
}
