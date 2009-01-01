/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
package sample.taskdependency;

import org.jppf.server.protocol.JPPFTask;


/**
 * This task is intended for testing a task submitting another task.
 * @author Laurent Cohen
 */
public class DependencyTask extends WorkflowTask
{
	/**
	 * The name given to this task.
	 */
	private String name = null;

	/**
	 * Initialize this task.
	 * @param name the name given to this task.
	 */
	public DependencyTask(String name)
	{
		this.name = name;
	}

	/**
	 * Run the test.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			if (getNextTask() != null)
			{
				JPPFTask task = getNextTask();
				setNextTask(null);
				task.run();
				
			}
			else
			{
				System.out.println("I am task '"+name+"'");
				setNextTask(new DependencyTask("Hello 2"));
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
