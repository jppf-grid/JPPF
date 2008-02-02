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

package sample.taskdependency;

import java.util.*;

import org.jppf.server.protocol.JPPFTask;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class WorkflowTask extends JPPFTask
{
	/**
	 * Subtasks this task has to execute.
	 */
	private List<JPPFTask> subtasks = new ArrayList<JPPFTask>();
	/**
	 * The next task to run.
	 */
	private JPPFTask nextTask = null;

	/**
	 * Get the next task to run.
	 * @return a <code>JPPFTask</code> instance.
	 */
	public JPPFTask getNextTask()
	{
		return nextTask;
	}

	/**
	 * Set the next task to run.
	 * @param nextTask a <code>JPPFTask</code> instance.
	 */
	public void setNextTask(JPPFTask nextTask)
	{
		this.nextTask = nextTask;
	}
}
