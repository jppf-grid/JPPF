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

package org.jppf.client;

import java.util.*;

import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;

/**
 * Implementation of the {@link org.jppf.client.event.TaskResultListener TaskResultListener} interface
 * that can be used &quot;as is&quot; to collect the results of an asynchronous job submission.
 * @see org.jppf.client.JPPFClient#submitNonBlocking(List, org.jppf.task.storage.DataProvider, TaskResultListener)
 * @author Laurent Cohen
 */
public class MyResultListener implements TaskResultListener
{
	/**
	 * Count of results not yet received.
	 */
	private int pendingCount = 0;
	/**
	 * An sorted map containing the resulting tasks, ordered by ascending position.
	 */
	private Map<Integer, JPPFTask> resultMap = new TreeMap<Integer, JPPFTask>();

	/**
	 * Initialize this collector with a specified number of tasks. 
	 * @param count the count of submitted tasks.
	 */
	public MyResultListener(int count)
	{
		this.pendingCount = count;
	}

	/**
	 * Called to notify that the results of a number of tasks have been received from the server.
	 * @param event a notification of completion for a set of submitted tasks.
	 * @see org.jppf.client.event.TaskResultListener#resultsReceived(org.jppf.client.event.TaskResultEvent)
	 */
	public void resultsReceived(TaskResultEvent event)
	{
		List<JPPFTask> tasks = event.getTaskList();
		for (JPPFTask task: tasks) resultMap.put(task.getPosition(), task);
		pendingCount -= tasks.size();
		notify();
	}

	/**
	 * Get the list of final results.
	 * @return a list of results as tasks, or null if not all tasks have been executed.
	 */
	public List<JPPFTask> getResults()
	{
		List<JPPFTask> results = new ArrayList<JPPFTask>();
		for (Integer n: resultMap.keySet()) results.add(resultMap.get(n));
		resultMap.clear();
		return results;
	}
}
