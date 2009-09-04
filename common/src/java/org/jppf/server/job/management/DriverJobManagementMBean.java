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

package org.jppf.server.job.management;

import javax.management.NotificationEmitter;

import org.jppf.job.JobInformation;


/**
 * A sample MBean interface.
 * @author Laurent Cohen
 */
public interface DriverJobManagementMBean extends NotificationEmitter
{
	/**
	 * Cancel the job with the specified id.
	 * @param jobId - the id of the job to cancel.
	 * @throws Exception if any error occurs.
	 */
	void cancelJob(String jobId) throws Exception;
	/**
	 * Suspend the job with the specified id.
	 * @param jobId - the id of the job to suspend.
	 * @throws Exception if any error occurs.
	 */
	void suspendJob(String jobId) throws Exception;
	/**
	 * Resume the job with the specified id.
	 * @param jobId - the id of the job to resume.
	 * @throws Exception if any error occurs.
	 */
	void resumeJob(String jobId) throws Exception;
	/**
	 * Update the maximum number of nodes a node can run on.
	 * @param jobId - the id of the job to update.
	 * @param maxNodes - the new maximum number of nodes for the job.
	 * @throws Exception if any error occurs.
	 */
	void updateMaxNodes(String jobId, Integer maxNodes) throws Exception;
	/**
	 * Get the set of ids for all the jobs currently queued or executing.
	 * @return an array of ids as strings.
	 * @throws Exception if any error occurs.
	 */
	String[] getAllJobIds() throws Exception;
	/**
	 * Get an object describing the job with the specified id. 
	 * @param jobId - the id of the job to get information about.
	 * @return an instance of <code>JobInformation</code>.
	 * @throws Exception if any error occurs.
	 */
	JobInformation getJobInformation(String jobId) throws Exception;
	/**
	 * Get a list of objects describing the nodes to which the whole or part of a job was dispatched.
	 * @param jobId - the id of the job for which to find node information.
	 * @return an array of <code>NodeManagementInfo</code>, <code>JobInformation</code> instances.
	 * @throws Exception if any error occurs.
	 */
	NodeJobInformation[] getNodeInformation(String jobId) throws Exception;
}
