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

import java.io.Serializable;

/**
 * Instances of this class group tasks from the same client together, so they are sent to the same node,
 * avoiding unnecessary transport overhead.<br>
 * The goal is to provide a performance enhancement through an adaptive bundling of tasks originating from the same client.
 * The bundle size is computed dynamically, depending on the number of nodes connected to the server, and other factors.
 * @author Laurent Cohen
 */
public class JobInformation implements Serializable
{
	/**
	 * The unique identifier for this task bundle.
	 */
	private String jobId = null;
	/**
	 * The number of tasks in this bundle.
	 */
	private int taskCount = 0;
	/**
	 * The initial number of tasks in this job.
	 */
	private int initialTaskCount = 0;
	/**
	 * The priority of this task bundle.
	 */
	private int priority = 0;

	/**
	 * Initialize this object.
	 */
	public JobInformation()
	{
	}

	/**
	 * Initialize this object with the specified parameters.
	 * @param jobId - the id of this job.
	 * @param taskCount - tne number of tasks in thsi job.
	 * @param initialTaskCount - the initial number of tasks in the job submitted by the JPPF client.
	 * @param priority - the priority of this job.
	 */
	public JobInformation(String jobId, int taskCount, int initialTaskCount, int priority)
	{
		this.jobId = jobId;
		this.taskCount = taskCount;
		this.initialTaskCount = initialTaskCount;
		this.priority = priority;
	}

	/**
	 * Get the unique identifier for this task bundle.
	 * @return the uuid as a string.
	 */
	public String getJobId()
	{
		return jobId;
	}

	/**
	 * Set the unique identifier for this task bundle.
	 * @param uuid the uuid as a string.
	 */
	public void setJobId(String uuid)
	{
		this.jobId = uuid;
	}

	/**
	 * Get the current number of tasks in this bundle.
	 * @return the number of tasks as an int.
	 */
	public int getTaskCount()
	{
		return taskCount;
	}

	/**
	 * Set the number of tasks in this bundle.
	 * @param taskCount the number of tasks as an int.
	 */
	public void setTaskCount(int taskCount)
	{
		this.taskCount = taskCount;
	}

	/**
	 * Get the priority of this task bundle.
	 * @return the priority as an int.
	 */
	public int getPriority()
	{
		return priority;
	}

	/**
	 * Set the priority of this task bundle.
	 * @param priority the priority as an int. 
	 */
	public void setPriority(int priority)
	{
		this.priority = priority;
	}

	/**
	 * Get the initial task count of this bundle.
	 * @return the task count as an int.
	 */
	public int getInitialTaskCount()
	{
		return initialTaskCount;
	}

	/**
	 * Set the initial task count of this bundle.
	 * @param initialTaskCount - the task count as an int.
	 */
	public void setInitialTaskCount(int initialTaskCount)
	{
		this.initialTaskCount = initialTaskCount;
	}

}
