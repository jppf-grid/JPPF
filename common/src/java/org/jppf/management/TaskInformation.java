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

package org.jppf.management;

import java.io.Serializable;

/**
 * Instances of this class encapsulate runtime information on tasks executed by a node.
 * @author Laurent Cohen
 */
public class TaskInformation implements Serializable
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The task id.
	 */
	private String id = null;
	/**
	 * The id of the job this task belongs to.
	 */
	private String jobId = null;
	/**
	 * The cpu time taken by the task.
	 */
	private long cpuTime = 0L;
	/**
	 * The wall clock time taken by the task.
	 */
	private long elapsedTime = 0L;
	/**
	 * Determines whether the task had an exception.
	 */
	private boolean error = false;
	/**
	 * Timestamp for the creation of this event.
	 */
	private long timestamp = System.currentTimeMillis();

	/**
	 * Initialize this event object with the specified task.
	 * @param id the task id.
	 * @param jobId the id of the job this task belongs to.
	 * @param cpuTime the cpu time taken by the task.
	 * @param elapsedTime the wall clock time taken by the task.
	 * @param error determines whether the task had an exception.
	 */
	public TaskInformation(String id, String jobId, long cpuTime, long elapsedTime, boolean error)
	{
		this.id = id;
		this.jobId = jobId;
		this.cpuTime = cpuTime;
		this.elapsedTime = elapsedTime;
		this.error = error;
	}

	/**
	 * Get the task id.
	 * @return the id as a string.
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Get the id of the job this task belongs to.
	 * @return the job id as a string.
	 */
	public String getJobId()
	{
		return jobId;
	}

	/**
	 * Get the cpu time taken by the task.
	 * @return the cpu time in milliseconds.
	 */
	public long getCpuTime()
	{
		return cpuTime;
	}

	/**
	 * Get the wall clock time taken by the task.
	 * @return the elapsed time in milliseconds.
	 */
	public long getElapsedTime()
	{
		return elapsedTime;
	}

	/**
	 * Determines whether the task had an exception.
	 * @return true if the task had an exception, false otherwise.
	 */
	public boolean hasError()
	{
		return error;
	}

	/**
	 * Get the timestamp for the creation of this event.
	 * @return the timestamp as a long value.
	 */
	public long getTimestamp()
	{
		return timestamp;
	}
}
