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

import org.apache.commons.logging.*;
import org.jppf.client.event.*;
import org.jppf.server.protocol.*;

/**
 * Implementation of the {@link org.jppf.client.event.TaskResultListener TaskResultListener} interface
 * that can be used &quot;as is&quot; to collect the results of an asynchronous job submission.
 * @see org.jppf.client.JPPFClient#submitNonBlocking(List, org.jppf.task.storage.DataProvider, TaskResultListener)
 * @author Laurent Cohen
 */
public class JPPFResultCollector implements TaskResultListener
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFResultCollector.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Count of results notr yet received.
	 */
	private int pendingCount = 0;
	/**
	 * A map containing the resulting tasks, ordered by ascending position in the
	 * submitted list of tasks.
	 */
	private Map<Integer, JPPFTask> resultMap = new TreeMap<Integer, JPPFTask>();
	/**
	 * The list of final resulting tasks.
	 */
	private List<JPPFTask> results = null;

	/**
	 * Initialize this collector with a specified number of tasks. 
	 * @param count the count of submitted tasks.
	 */
	public JPPFResultCollector(int count)
	{
		this.pendingCount = count;
	}

	/**
	 * Called to notify that the results of a number of tasks have been received from the server.
	 * @param event a notification of completion for a set of submitted tasks.
	 * @see org.jppf.client.event.TaskResultListener#resultsReceived(org.jppf.client.event.TaskResultEvent)
	 */
	public synchronized void resultsReceived(TaskResultEvent event)
	{
		List<JPPFTask> tasks = event.getTaskList();
		if (debugEnabled) log.debug("Received results for " + tasks.size() + " tasks");
		for (JPPFTask task: tasks) resultMap.put(task.getPosition(), task);
		pendingCount -= tasks.size();
		notify();
	}

	/**
	 * Wait until all results of a request have been collected.
	 * @return the list of resulting tasks.
	 */
	public synchronized List<JPPFTask> waitForResults()
	{
		while (pendingCount > 0)
		{
			try
			{
				wait();
			}
			catch(InterruptedException e)
			{
				log.error(e.getMessage(), e);
			}
		}
		results = new ArrayList<JPPFTask>();
		for (Integer n: resultMap.keySet()) results.add(resultMap.get(n));
		resultMap.clear();
		return results;
	}

	/**
	 * Get the list of final results.
	 * @return a list of results as tasks, or null if not all tasks have been executed.
	 */
	public List<JPPFTask> getResults()
	{
		return results;
	}
}
