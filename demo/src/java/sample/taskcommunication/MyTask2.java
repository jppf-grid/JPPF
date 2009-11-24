/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package sample.taskcommunication;

import java.util.Map;

/**
 * This task wait for a message from a <code>MyTask1</code>, sends a response, and use the message as its result.
 * @author Laurent Cohen
 */
public class MyTask2 extends AbstractMyTask
{
	/**
	 * Default constructor.
	 */
	public MyTask2()
	{
		super("task2");
	}

	/**
	 * Wait for data provided by a <code>MyTask1</code> instance, then add an item to a distributed map for this <code>Mytask1</code>.  
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		Map<String, String> taskMap = getMap();
		String s = null;
		while (s == null)
		{
			s = taskMap.get("toTask2");
			if (s == null) doWait(50);
		}
		setResult(s);
		String key = "toTask1";
		taskMap.put(key, "This is for MyTask1");
	}
}
