/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.server.job;

import java.util.EventObject;

import org.jppf.management.NodeManagementInfo;

/**
 * Instances of this class represent events emitted by a JPPFJobManager.
 * @author Laurent Cohen
 */
public class JobManagerEvent extends EventObject
{
	/**
	 * Information about a node.
	 */
	private NodeManagementInfo nodeInfo = null;

	/**
	 * Initialize this event with the specified job and node information.
	 * @param jobInfo - information about the job.
	 * @param nodeInfo - information about the node.
	 */
	public JobManagerEvent(JobInformation jobInfo, NodeManagementInfo nodeInfo)
	{
		super(jobInfo);
		this.nodeInfo = nodeInfo;
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
}
