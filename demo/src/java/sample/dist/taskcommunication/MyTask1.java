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

package sample.dist.taskcommunication;

import java.util.Map;

/**
 * This task sends a message to a <code>MyTask2</code>, waits for its response, and use the response as its result.
 * @author Laurent Cohen
 */
public class MyTask1 extends AbstractMyTask
{
	/**
	 * Default constructor.
	 */
	public MyTask1()
	{
		super("task1");
	}

	/**
	 * Add an item to a distributed map for a <code>MyTask2</code> instance, then wait for data provided by this <code>MyTask2</code>.  
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		Map<String, String> taskMap = getMap();
		taskMap.put("toTask2", "This is for MyTask2");
		String s = null;
		while (s == null)
		{
			s = taskMap.get("toTask1");
			if (s == null) doWait(50);
		}
		setResult(s);
		taskMap.clear();
	}
}
