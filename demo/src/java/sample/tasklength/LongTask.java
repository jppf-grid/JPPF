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
package sample.tasklength;

import sample.BaseDemoTask;

/**
 * Instances of this class are defined as tasks with a predefined execution length, specified at their creation. 
 * @author Laurent Cohen
 */
public class LongTask extends BaseDemoTask
{
	/**
	 * Determines how long this task will run.
	 */
	private long taskLength = 0L;
	/**
	 * Timestamp marking the time when the task execution starts.
	 */
	private long taskStart = 0L;

	/**
	 * Initialize this task with a predefined length of time, in milliseconds, during which it will run.
	 * @param taskLength determines how long this task will run.
	 */
	public LongTask(long taskLength)
	{
		this.taskLength = taskLength;
	}

	/**
	 * Perform the excution of this task.
	 * @see sample.BaseDemoTask#doWork()
	 */
	public void doWork()
	{
		/*
		fireNotification("Starting task with duration = "+ taskLength + " ms");
		taskStart = System.currentTimeMillis();
		double elapsed = 0L;
		while (elapsed < taskLength)
		{
			Random rand = new Random(System.currentTimeMillis());
			String s = "";
			for (int i=0; i<10; i++) s += "A"+rand.nextInt(10);
			//s.replace("8", "$");
			elapsed = System.currentTimeMillis() - taskStart;
		}
		fireNotification("Task execution ended after " + elapsed + " ms");
		*/
		try
		{
			Thread.sleep(taskLength);
		}
		catch(InterruptedException e)
		{
			setException(e);
			setResult(e.getMessage());
		}
		setResult("task has run for " + taskLength + " ms");
	}
}
