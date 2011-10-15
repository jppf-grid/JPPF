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

package org.jppf.node.protocol;

import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.scheduling.JPPFSchedule;

/**
 * This interface represents the Service Level Agreement Between a JPPF job and a server.
 * It determines the state, conditions and order in which a job will be executed. 
 * @author Laurent Cohen
 */
public interface JobSLA
{

	/**
	 * Get the tasks execution policy.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	ExecutionPolicy getExecutionPolicy();

	/**
	 * Set the tasks execution policy.
	 * @param executionPolicy an <code>ExecutionPolicy</code> instance.
	 */
	void setExecutionPolicy(ExecutionPolicy executionPolicy);

	/**
	 * Get the priority of this job.
	 * @return the priority as an int.
	 */
	int getPriority();

	/**
	 * Set the priority of this job.
	 * @param priority the priority as an int.
	 */
	void setPriority(int priority);

	/**
	 * Get the maximum number of nodes this job can run on.
	 * @return the number of nodes as an int value.
	 */
	int getMaxNodes();

	/**
	 * Set the maximum number of nodes this job can run on.
	 * @param maxNodes the number of nodes as an int value. A value <= 0 means no limit on the number of nodes.
	 */
	void setMaxNodes(int maxNodes);

	/**
	 * Determine whether this job is initially suspended.
	 * @return true if the job is suspended, false otherwise.
	 */
	boolean isSuspended();

	/**
	 * Specify whether this job is initially suspended.
	 * @param suspended true if the job is suspended, false otherwise.
	 */
	void setSuspended(boolean suspended);

	/**
	 * Get the job schedule.
	 * @return a <code>JPPFSchedule</code> instance.
	 */
	JPPFSchedule getJobSchedule();

	/**
	 * Set the job schedule.
	 * @param jobSchedule a <code>JPPFSchedule</code> instance. 
	 */
	void setJobSchedule(JPPFSchedule jobSchedule);

	/**
	 * Get the job expiration schedule configuration.
	 * @return a {@link JPPFSchedule} instance.
	 */
	JPPFSchedule getJobExpirationSchedule();

	/**
	 * Set the job expiration schedule configuration.
	 * @param jobExpirationSchedule a {@link JPPFSchedule} instance.
	 */
	void setJobExpirationSchedule(JPPFSchedule jobExpirationSchedule);

	/**
	 * Determine whether the job is a broadcast job.
	 * @return true for a broadcast job, false otherwise.
	 */
	boolean isBroadcastJob();

	/**
	 * Specify whether the job is a broadcast job.
	 * @param broadcastJob true for a broadcast job, false otherwise.
	 */
	void setBroadcastJob(boolean broadcastJob);

	/**
	 * Determine whether the job should be canceled by the driver if the client gets disconnected.
	 * @return <code>true</code> if the job should be canceled (this is the default), <code>false</code> otherwise.
	 */
	boolean isCancelUponClientDisconnect();

	/**
	 * Specifiy whether the job should be canceled by the driver if the client gets disconnected.
	 * @param cancelUponClientDisconnect <code>true</code> if the job should be canceled, <code>false</code> otherwise.
	 */
	void setCancelUponClientDisconnect(boolean cancelUponClientDisconnect);
}