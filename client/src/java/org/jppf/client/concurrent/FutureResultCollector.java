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

package org.jppf.client.concurrent;

import java.util.*;

import org.jppf.client.JPPFResultCollector;
import org.jppf.client.event.TaskResultEvent;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
class FutureResultCollector extends JPPFResultCollector
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(FutureResultCollector.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * A lst of the listeners to this results collector.
	 */
	private List<FutureResultCollectorListener> listeners = new ArrayList<FutureResultCollectorListener>();
	/**
	 * The uuid of the corresponding job.
	 */
	private String jobUuid = null;

	/**
	 * Initialize this collector with a specified number of tasks. 
	 * @param count the count of submitted tasks.
	 * @param jobUuid the uuid of the corresponding job.
	 */
	FutureResultCollector(int count, String jobUuid)
	{
		super(count);
		this.jobUuid = jobUuid;
	}

	/**
	 * Set the pending tasks count for this result collector.
	 * @param count the task count ot set.
	 */
	synchronized void setTaskCount(int count)
	{
		pendingCount = count;
	}

	/**
	 * Get the task at the specified position.
	 * @param position the position of the task in the job it is a part of.
	 * @return the task whose results were received, or null if the results were not received.
	 */
	synchronized JPPFTask getTask(int position)
	{
		return resultMap.get(position);
	}

	/**
	 * Wait for the execution results of the specified task to be received.
	 * @param position the position of the task in the job it is a part of.
	 * @return the task whose results were received.
	 */
	synchronized JPPFTask waitForTask(int position)
	{
		return waitForTask(position, Long.MAX_VALUE);
	}

	/**
	 * Wait for the execution results of the specified task to be received.
	 * @param position the position of the task in the job it is a part of.
	 * @param millis maximum number of miliseconds to wait.
	 * @return the task whose results were received, or null if the tiemout expired before it was received.
	 */
	synchronized JPPFTask waitForTask(int position, long millis)
	{
		long start = System.currentTimeMillis();
		long elapsed = 0;
		boolean taskReceived = isTaskReceived(position);
		while ((elapsed < millis) && !taskReceived)
		{
			try
			{
				wait(millis - elapsed);
			}
			catch(InterruptedException e)
			{
				log.error(e.getMessage(), e);
			}
			elapsed = System.currentTimeMillis() - start;
			taskReceived = isTaskReceived(position);
			if ((elapsed >= millis) && !taskReceived) return null;
		}
		return resultMap.get(position);
	}

	/**
	 * Determine whether the results of the specified task have been received.
	 * @param position the position of the task in the job it is a part of.
	 * @return true if the results of the task have been received, false otherwise.
	 */
	boolean isTaskReceived(int position)
	{
		return resultMap.get(position) != null;
	}

	/**
	 * Called to notify that the results of a number of tasks have been received from the server.
	 * @param event a notification of completion for a set of submitted tasks.
	 * @see org.jppf.client.JPPFResultCollector#resultsReceived(org.jppf.client.event.TaskResultEvent)
	 */
	@Override
    public synchronized void resultsReceived(TaskResultEvent event)
	{
		super.resultsReceived(event);
		if (pendingCount <= 0) fireEvent();
	}

	/**
	 * Register a listener with this results collector.
	 * @param listener the listener to register.
	 */
	synchronized void addListener(FutureResultCollectorListener listener)
	{
		listeners.add(listener);
	}

	/**
	 * Remove a listener form the list of listners registered this results collector.
	 * @param listener the listener to remove.
	 */
	synchronized void removeListener(FutureResultCollectorListener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Notify all listeners that all results have been received by this collector.
	 */
	synchronized void fireEvent()
	{
		FutureResultCollectorEvent event = new FutureResultCollectorEvent(this);
		for (FutureResultCollectorListener listener: listeners) listener.resultsComplete(event);
	}

	/**
	 * Get the uuid of the corresponding job.
	 * @return the uuid as a string.
	 */
	String getJobUuid()
	{
		return jobUuid;
	}
}
