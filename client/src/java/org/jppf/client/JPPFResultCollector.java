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

package org.jppf.client;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.client.event.*;
import org.jppf.server.protocol.*;

/**
 * Implementation of the {@link org.jppf.client.event.TaskResultListener TaskResultListener} interface
 * that can be used &quot;as is&quot; to collect the results of an asynchronous tasks submission.
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
	private Map<Integer, List<JPPFTask>> resultMap = new TreeMap<Integer, List<JPPFTask>>();
	/**
	 * The list of final resulting taskss.
	 */
	private List<JPPFTask> results = null;
	/**
	 * The list of final result objects.
	 */
	private List<Object> resultObjects = null;

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
		int idx = event.getStartIndex();
		List<JPPFTask> tasks = event.getTaskList();
		if (debugEnabled) log.debug("Received results for tasks " + idx + " - " + (idx + tasks.size() - 1));
		resultMap.put(idx, tasks);
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
				e.printStackTrace();
			}
		}
		results = new ArrayList<JPPFTask>();
		for (Integer n: resultMap.keySet())
		{
			for (JPPFTask task: resultMap.get(n)) results.add(task);
		}
		resultMap.clear();
		return results;
	}

	/**
	 * Wait until all result objects of a request have been collected.
	 * @return the list of resulting objects, either tasks or JPPF-annotated objects.
	 */
	public synchronized List<Object> waitForResultObjects()
	{
		if (resultObjects == null)
		{
			waitForResults();
			resultObjects = new ArrayList<Object>();
			for (JPPFTask t: results)
			{
				resultObjects.add(
					t instanceof JPPFAnnotatedTask ? ((JPPFAnnotatedTask) t).getResult() : t);
			}
		}
		return resultObjects;
	}

	/**
	 * Get the list of final results.
	 * @return a list of results as tasks, or null if not all tasks have been executed.
	 */
	public List<JPPFTask> getResults()
	{
		return results;
	}

	/**
	 * Get the list of final result objects.
	 * @return a list of results as tasks, or null if not all tasks have been executed.
	 */
	public List<Object> getObjectResults()
	{
		return resultObjects;
	}
}
