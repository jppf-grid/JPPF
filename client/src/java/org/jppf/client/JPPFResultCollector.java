/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
import org.jppf.client.persistence.JobPersistenceException;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;

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
	private static Logger log = LoggerFactory.getLogger(JPPFResultCollector.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The initial task count in the job.
	 */
	protected int count;
	/**
	 * Count of results notr yet received.
	 */
	protected int pendingCount = 0;
	/**
	 * A map containing the resulting tasks, ordered by ascending position in the
	 * submitted list of tasks.
	 */
	protected Map<Integer, JPPFTask> resultMap = new TreeMap<Integer, JPPFTask>();
	/**
	 * The list of final resulting tasks.
	 */
	protected List<JPPFTask> results = null;
	/**
	 * 
	 */
	protected JPPFJob job = null;

	/**
	 * Default constructor, provided as a convenience for subclasses.
	 */
	protected JPPFResultCollector()
	{
	}

	/**
	 * Initialize this collector with the specified job.
	 * @param job the job to execute.
	 */
	public JPPFResultCollector(final JPPFJob job)
	{
		this.job = job;
		count = job.getTasks().size() - job.getResults().size();
		pendingCount = count;
	}

	/**
	 * Initialize this collector with a specified number of tasks.
	 * @param count the count of submitted tasks.
	 */
	public JPPFResultCollector(final int count)
	{
		this.count = count;
		this.pendingCount = count;
		//if (debugEnabled) log.debug("count = " + count);
	}

	/**
	 * Called to notify that the results of a number of tasks have been received from the server.
	 * @param event a notification of completion for a set of submitted tasks.
	 * @see org.jppf.client.event.TaskResultListener#resultsReceived(org.jppf.client.event.TaskResultEvent)
	 */
	@Override
	public synchronized void resultsReceived(final TaskResultEvent event)
	{
		if (event.getThrowable() == null)
		{
			List<JPPFTask> tasks = event.getTaskList();
			for (JPPFTask task: tasks) resultMap.put(task.getPosition(), task);
			if (job != null) job.getResults().putResults(tasks);
			pendingCount -= tasks.size();
			if (debugEnabled) log.debug("Received results for " + tasks.size() + " tasks, pendingCount = " + pendingCount);
			notifyAll();
			if ((job != null) && (job.getPersistenceManager() != null))
			{
				try
				{
					job.getPersistenceManager().storeJob(job.getJobUuid(), job, tasks);
				}
				catch (JobPersistenceException e)
				{
					log.error(e.getMessage(), e);
				}
			}
		}
		else
		{
			Throwable t = event.getThrowable();
			if (debugEnabled) log.debug("received throwable '" + t.getClass().getName() + ": " + t.getMessage() + "', resetting this result collector");
			// reset this object's state to prepare for job resubmission
			if (job != null) count = job.getTasks().size() - job.getResults().size();
			pendingCount = count;
			resultMap = new TreeMap<Integer, JPPFTask>();
			results = null;
		}
	}

	/**
	 * Wait until all results of a request have been collected.
	 * @return the list of resulting tasks.
	 */
	public synchronized List<JPPFTask> waitForResults()
	{
		return waitForResults(Long.MAX_VALUE);
	}

	/**
	 * Wait until all results of a request have been collected, or the timeout has expired,
	 * whichever happens first.
	 * @param millis the maximum time to wait, zero meaning an indefinite wait.
	 * @return the list of resulting tasks.
	 */
	public synchronized List<JPPFTask> waitForResults(final long millis)
	{
		if (millis < 0) throw new IllegalArgumentException("wait time cannot be negative");
		if (log.isTraceEnabled()) log.trace("timeout = " + millis);
		long start = System.currentTimeMillis();
		long elapsed = 0;
		while ((elapsed < millis) && (pendingCount > 0))
		{
			try
			{
				if (elapsed >= millis) return null;
				wait(millis - elapsed);
			}
			catch(InterruptedException e)
			{
				log.error(e.getMessage(), e);
			}
			elapsed = System.currentTimeMillis() - start;
		}
		if (pendingCount <= 0) buildResults();
		if (log.isTraceEnabled()) log.trace("elapsed = " + elapsed);
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

	/**
	 * Build the results list based on a map of executed tasks.
	 */
	protected void buildResults()
	{
		if (job == null) results = new ArrayList<JPPFTask>(resultMap.values());
		else results = new ArrayList<JPPFTask>(job.getResults().getAll());
	}
}
