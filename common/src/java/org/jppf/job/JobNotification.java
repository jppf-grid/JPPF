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

package org.jppf.job;

import javax.management.Notification;

import org.jppf.management.NodeManagementInfo;

/**
 * Instances of this class represent events emitted by a JPPFJobManager.
 * @author Laurent Cohen
 */
public class JobNotification extends Notification
{
	/**
	 * Information about a node.
	 */
	private NodeManagementInfo nodeInfo = null;
	/**
	 * Creation timestamp for this event.
	 */
	private long timestamp = -1L;
	/**
	 * The type of this job event.
	 */
	private JobEventType eventType = null;

	/**
	 * Initialize this event with the specified job and node information.
	 * @param eventType - the type of this job event.
	 * @param jobInfo - information about the job.
	 * @param nodeInfo - information about the node.
	 * @param timestamp - the creation timestamp for this event.
	 */
	public JobNotification(JobEventType eventType, JobInformation jobInfo, NodeManagementInfo nodeInfo, long timestamp)
	{
		super("jobEvent", jobInfo, timestamp);
		this.eventType = eventType;
		this.nodeInfo = nodeInfo;
		this.timestamp = timestamp;
	}

	/**
	 * Get the information about the job.
	 * @return a <code>JobInformation</code> instance.
	 */
	public JobInformation getJobInformation()
	{
		return (JobInformation) getSource();
	}

	/**
	 * Get the information about the node.
	 * @return a <code>NodeManagementInfo</code> instance.
	 */
	public NodeManagementInfo getNodeInfo()
	{
		return nodeInfo;
	}

	/**
	 * Get the creation timestamp for this event.
	 * @return the timestamp as a long value.
	 */
	public long getTimestamp()
	{
		return timestamp;
	}

	/**
	 * Get the type of this job event.
	 * @return a <code>JobManagerEventType</code> enum value.
	 */
	public JobEventType getEventType()
	{
		return eventType;
	}
}
