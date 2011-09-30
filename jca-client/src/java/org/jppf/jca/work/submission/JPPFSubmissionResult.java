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

package org.jppf.jca.work.submission;

import java.util.*;

import org.jppf.client.SubmissionStatus;
import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class encapsulates the results of an asynchronous tasks submission.
 * @author Laurent Cohen
 */
public class JPPFSubmissionResult extends ThreadSynchronization implements TaskResultListener
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFSubmissionResult.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
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
	 * The status of this submission.
	 */
	private SubmissionStatus status = SubmissionStatus.SUBMITTED;
	/**
	 * Number of tasks not yet completed.
	 */
	private int pendingCount = -1;
	/**
	 * Total number of tasks to execute.
	 */
	private int taskCount = -1;
	/**
	 * The unique id of this submission.
	 */
	private String id = null;
	/**
	 * List of listeners registered to receive this submission's status change notifications.
	 */
	private List<SubmissionStatusListener> listeners = new ArrayList<SubmissionStatusListener>();

	/**
	 * Initialize this collector. 
	 * @param taskCount the numbe rof tasks to execute.
	 * @param id the id assigned to this submission.
	 */
	JPPFSubmissionResult(int taskCount, String id)
	{
		this.taskCount = taskCount;
		this.pendingCount = taskCount;
		this.id = (id == null) ? new JPPFUuid(JPPFUuid.HEXADECIMAL, 32).toString() : id;
	}

	/**
	 * Called to notify that that results of a number of tasks have been received from the server.
	 * @param event the event that encapsulates the tasks that were received and related information.
	 * @see org.jppf.client.event.TaskResultListener#resultsReceived(org.jppf.client.event.TaskResultEvent)
	 */
	public synchronized void resultsReceived(TaskResultEvent event)
	{
		List<JPPFTask> tasks = event.getTaskList();
		int n = tasks.size();
		pendingCount -= n;
		if (debugEnabled) log.debug("Received results for " + tasks.size() + " tasks, " + pendingCount + " pending tasks remain" +  (n > 0 ? ", first position=" + tasks.get(0).getPosition() : ""));
		for (JPPFTask task: tasks) resultMap.put(task.getPosition(), task);
		wakeUp();
	}

	/**
	 * Wait until all results have been received, or the specified timeout expires, whichever happens first.
	 * @param timeout the maximum time to wait for the result, 0 or less means no expiration.
	 */
	public synchronized void waitForResults(long timeout)
	{
		long start = System.currentTimeMillis();
		long elapsed = 0;
		boolean expires = timeout > 0;
		while ((pendingCount > 0) && (((elapsed < timeout) && expires) || !expires))
		{
			if (timeout > 0) goToSleep(timeout - elapsed);
			else goToSleep();
			elapsed = System.currentTimeMillis() - start;
		}
	}

	/**
	 * Get the list of final results.
	 * @return a list of results as tasks, or null if not all tasks have been executed.
	 */
	public synchronized List<JPPFTask> getResults()
	{
		if (pendingCount > 0) return null;
		if (results == null)
		{
			results = new ArrayList<JPPFTask>();
			//for (JPPFTask task: resultMap.values()) results.add(task);
			for (final Map.Entry<Integer, JPPFTask> entry : resultMap.entrySet()) results.add(entry.getValue());
			resultMap.clear();
		}
		return results;
	}

	/**
	 * Get the status of this submission.
	 * @return a {@link SubmissionStatus} enumerated value.
	 */
	public synchronized SubmissionStatus getStatus()
	{
		return status;
	}

	/**
	 * Set the status of this submission.
	 * @param status a {@link SubmissionStatus} enumerated value.
	 */
	public synchronized void setStatus(SubmissionStatus status)
	{
		if (debugEnabled) log.debug("submission [" + id + "] status changing from '" + this.status + "' to '" + status + "'");
		this.status = status;
		fireStatusChangeEvent();
	}

	/**
	 * Get the unique id of this submission.
	 * @return the id as a string.
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Add a listener to the list of status listeners.
	 * @param listener the listener to add.
	 */
	public void addSubmissionStatusListener(SubmissionStatusListener listener)
	{
		synchronized(listeners)
		{
			if (debugEnabled) log.debug("submission [" + id + "] adding status listener " + listener);
			if (listener != null) listeners.add(listener);
		}
	}

	/**
	 * Remove a listener from the list of status listeners.
	 * @param listener the listener to remove.
	 */
	public void removeSubmissionStatusListener(SubmissionStatusListener listener)
	{
		synchronized(listeners)
		{
			if (debugEnabled) log.debug("submission [" + id + "] removing status listener " + listener);
			if (listener != null) listeners.remove(listener);
		}
	}

	/**
	 * Notify all listeners of a change of status for this submision.
	 */
	protected void fireStatusChangeEvent()
	{
		synchronized(listeners)
		{
			if (listeners.isEmpty()) return;
			if (debugEnabled) log.debug("submission [" + id + "] firng status changed event for '" + status + "'");
			SubmissionStatusEvent event = new SubmissionStatusEvent(id, status);
			for (SubmissionStatusListener listener: listeners)
			{
				listener.submissionStatusChanged(event);
			}
		}
	}

	/**
	 * Reset this submission result for new submission of the same tasks. 
	 */
	synchronized void reset()
	{
		resultMap.clear();
		results = null;
		pendingCount = taskCount;
	}
}
