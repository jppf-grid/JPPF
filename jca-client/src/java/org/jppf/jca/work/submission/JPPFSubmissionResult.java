/*
 * Java Parallel Processing Framework.
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

package org.jppf.jca.work.submission;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.JPPFUuid;

/**
 * This class encapsulates the results of an asynchronous tasks submission.
 * @author Laurent Cohen
 */
public class JPPFSubmissionResult implements TaskResultListener
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFSubmissionResult.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * A map containing the resulting tasks, ordered by ascending position in the
	 * submitted list of tasks.
	 */
	//private Map<Integer, List<JPPFTask>> resultMap = new TreeMap<Integer, List<JPPFTask>>();
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
	 * The unique id of this submission.
	 */
	private String id = new JPPFUuid(JPPFUuid.HEXADECIMAL, 32).toString();
	/**
	 * List of listeners registered to receive this submission's status change notifications.
	 */
	private List<SubmissionStatusListener> listeners = new ArrayList<SubmissionStatusListener>();

	/**
	 * Initialize this collector. 
	 * @param taskCount the numbe rof tasks to execute.
	 */
	public JPPFSubmissionResult(int taskCount)
	{
		this.pendingCount = taskCount;
	}

	/**
	 * Called to notify that that results of a number of tasks have been received from the server.
	 * @param event the event that encapsulates the tasks that were received and related information.
	 * @see org.jppf.client.event.TaskResultListener#resultsReceived(org.jppf.client.event.TaskResultEvent)
	 */
	public void resultsReceived(TaskResultEvent event)
	{
		List<JPPFTask> tasks = event.getTaskList();
		pendingCount -= tasks.size();
		if (debugEnabled) log.debug("Received results for" + tasks.size() + " tasks ");
		for (JPPFTask task: tasks) resultMap.put(task.getPosition(), task);
	}

	/**
	 * Get the list of final results.
	 * @return a list of results as tasks, or null if not all tasks have been executed.
	 */
	public List<JPPFTask> getResults()
	{
		if (pendingCount > 0) return null;
		if (results == null)
		{
			results = new ArrayList<JPPFTask>();
			//for (JPPFTask task: resultMap.values()) results.add(task);
			for (Integer n: resultMap.keySet()) results.add(resultMap.get(n));
			resultMap.clear();
		}
		return results;
	}

	/**
	 * Get the status of this submission.
	 * @return a {@link SubmissionStatus} enumerated value.
	 */
	public SubmissionStatus getStatus()
	{
		return status;
	}

	/**
	 * Set the status of this submission.
	 * @param status a {@link SubmissionStatus} enumerated value.
	 */
	public void setStatus(SubmissionStatus status)
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
		if (debugEnabled) log.debug("submission [" + id + "] adding status listener " + listener);
		if (listener != null) listeners.add(listener);
	}

	/**
	 * Remove a listener from the list of status listeners.
	 * @param listener the listener to remove.
	 */
	public void removeSubmissionStatusListener(SubmissionStatusListener listener)
	{
		if (debugEnabled) log.debug("submission [" + id + "] removing status listener " + listener);
		if (listener != null) listeners.remove(listener);
	}

	/**
	 * Notify all listeners of a change of status for this submision.
	 */
	protected void fireStatusChangeEvent()
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
