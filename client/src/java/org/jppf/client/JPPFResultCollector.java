/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.jppf.client;

import java.util.*;

import org.apache.log4j.Logger;
import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;

/**
 * Implementation of the {@link org.jppf.client.event.TaskResultListener TaskResultListener} interface
 * that can be used &quot;as is&quot; to collect the results of an asynchronous tasks submission.
 * @see org.jppf.client.JPPFClient#submitNonBlocking(List, org.jppf.task.storage.DataProvider, TaskResultListener)
 * @author Laurent Cohen
 */
public class JPPFResultCollector implements TaskResultListener
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(JPPFResultCollector.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Count of results notr yet received.
	 */
	private int pendingCount = 0;
	/**
	 * Number of tasks whose results ot wait for.
	 */
	private int nbTasks = 0;
	/**
	 * A map containing the resulting tasks, ordered by ascending position in the
	 * submitted list of tasks.
	 */
	private Map<Integer, List<JPPFTask>> resultMap = new TreeMap<Integer, List<JPPFTask>>();
	/**
	 * The list of final results.
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
	 * Get the list of final results.
	 * @return a list of results as tasks, or null if not all tasks have been executed.
	 */
	public List<JPPFTask> getResults()
	{
		return results;
	}
}
