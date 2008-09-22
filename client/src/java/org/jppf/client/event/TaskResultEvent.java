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
package org.jppf.client.event;

import java.util.*;

import org.jppf.client.JPPFAnnotatedTask;
import org.jppf.server.protocol.*;

/**
 * Event object used to notify interested listeners that a list of task results
 * have been received from the server.
 * @author Laurent Cohen
 */
public class TaskResultEvent extends EventObject
{
	/**
	 * The list of task objects, including those wrapped as <code>JPPFAnnotatedTask</code>.
	 */
	private List<Object> results = null;
	/**
	 * Index of the first task in the list, relative to the initial execution request.
	 */
	private int startIndex = -1;

	/**
	 * Initialize this event with a specified list of tasks and start index.
	 * @param taskList the list of tasks whose results have been received from the server.
	 * @param startIndex index of the first task in the list, relative to the initial execution
	 * request. Used to enable proper ordering of the results.
	 */
	public TaskResultEvent(List<JPPFTask> taskList, int startIndex)
	{
		super(taskList);
		this.startIndex = startIndex;
	}

	/**
	 * Get the list of tasks whose results have been received from the server.
	 * @return a list of <code>JPPFTask</code> instances.
	 */
	public List<JPPFTask> getTaskList()
	{
		return (List<JPPFTask>) getSource();
	}

	/**
	 * Get the list of task objects, including those wrapped as <code>JPPFAnnotatedTask</code>.
	 * @return a list of objects.
	 */
	public List<Object> getResults()
	{
		if (results == null)
		{
			results = new ArrayList<Object>();
			List<JPPFTask> tasks = (List<JPPFTask>) getSource();
			for (JPPFTask t: tasks)
			{
				results.add(t instanceof JPPFAnnotatedTask ? ((JPPFAnnotatedTask) t).getResult() : t);
			}
		}
		return results;
	}

	/**
	 * Get the index of the first task in the list, relative to the initial execution request.
	 * @return the index as an int value.
	 */
	public int getStartIndex()
	{
		return startIndex;
	}
}
