/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.samples.jaligner;

import java.util.*;

import javax.swing.SwingUtilities;

import org.jppf.client.JPPFResultCollector;
import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;

/**
 * Result collector that updates the progess bar's value during the computation.
 */
public class AlignmentResultCollector implements TaskResultListener
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFResultCollector.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Totak number of expected results.
	 */
	private int initialCount = 0;
	/**
	 * Count of results not yet received.
	 */
	private int pendingCount = 0;
	/**
	 * A map containing the resulting tasks, ordered by ascending position in the
	 * submitted list of tasks.
	 */
	private Map<Integer, JPPFTask> resultMap = new TreeMap<Integer, JPPFTask>();
	/**
	 * The list of final results.
	 */
	private List<JPPFTask> results = null;

	/**
	 * Initialize this collector with a specified number of tasks. 
	 * @param count the count of submitted tasks.
	 */
	public AlignmentResultCollector(int count)
	{
		this.pendingCount = count;
		this.initialCount = count;
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
		final int n = (100 * (initialCount-pendingCount)) / initialCount;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				SequenceAlignmentRunner.updateProgress(n);
			}
		});
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
