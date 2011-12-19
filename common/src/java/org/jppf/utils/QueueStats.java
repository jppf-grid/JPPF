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

package org.jppf.utils;

import java.io.Serializable;

/**
 * Instances of this class represent statistics for the content of a queue.
 * @author Laurent Cohen
 */
public class QueueStats implements Serializable
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Total number of objects that have been queued.
	 */
	private int totalQueued = 0;
	/**
	 * The current size of the queue.
	 */
	private int queueSize = 0;
	/**
	 * The maximum size of the queue.
	 */
	private int maxQueueSize = 0;
	/**
	 * Time statistics for the queued objects.
	 */
	private TimeSnapshot times = new TimeSnapshot("queue");
	/**
	 * Title for this queue snapshot, used in the {@link #toString()} method.
	 */
	public String title = "";

	/**
	 * Initialize this time snapshot with a specified title.
	 * @param title the title for this snapshot.
	 */
	public QueueStats(String title)
	{
		this.title = title;
		times = new TimeSnapshot(title);
	}

	/**
	 * Get the total number of objects that have been queued.
	 * @return the number of objects as an int.
	 */
	public int getTotalQueued()
	{
		return totalQueued;
	}

	/**
	 * Set the total number of objects that have been queued.
	 * @param totalQueued the number of objects as an int.
	 */
	public void setTotalQueued(int totalQueued)
	{
		this.totalQueued = totalQueued;
	}

	/**
	 * Get the current size of the queue.
	 * @return the queue size as an int. 
	 */
	public int getQueueSize()
	{
		return queueSize;
	}

	/**
	 * Set the current size of the queue.
	 * @param queueSize the queue size as an int.
	 */
	public void setQueueSize(int queueSize)
	{
		this.queueSize = queueSize;
	}

	/**
	 * Get the maximum size of the queue.
	 * @return the maximum queue size as an int. 
	 */
	public int getMaxQueueSize()
	{
		return maxQueueSize;
	}

	/**
	 * Set the maximum size of the queue.
	 * @param maxQueueSize the maximum queue size as an int.
	 */
	public void setMaxQueueSize(int maxQueueSize)
	{
		this.maxQueueSize = maxQueueSize;
	}

	/**
	 * Get the time snapshot.
	 * @return a {@link TimeSnapshot} instance.
	 */
	public TimeSnapshot getTimes()
	{
		return times;
	}

	/**
	 * Set the time snapshot.
	 * @param times a {@link TimeSnapshot} instance.
	 */
	public void setTimes(TimeSnapshot times)
	{
		this.times = times;
	}

	/**
	 * Get the title.
	 * @return the title string.
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(title).append(" queue");
		sb.append("queueSize : ").append(queueSize).append("\n");
		sb.append("maxQueueSize : ").append(maxQueueSize).append("\n");
		sb.append("totalQueued : ").append(totalQueued).append("\n");
		sb.append(times.toString());
		return sb.toString();
	}

	/**
	 * Male a copy of this queue stats object.
	 * @return a {@link QueueStats} instance.
	 */
	public QueueStats makeCopy()
	{
		QueueStats qs = new QueueStats(title);
		qs.setQueueSize(queueSize);
		qs.setMaxQueueSize(maxQueueSize);
		qs.setTotalQueued(totalQueued);
		qs.setTimes(times.makeCopy());
		return qs;
	}
}
