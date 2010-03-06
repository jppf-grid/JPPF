/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.client;

import java.io.Serializable;
import java.util.*;

import org.jppf.JPPFException;
import org.jppf.client.event.TaskResultListener;
import org.jppf.client.taskwrapper.JPPFAnnotatedTask;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.JPPFUuid;

/**
 * Instances of this class represent a JPPF submission and hold all the required elements:
 * tasks, execution policy, task listener, data provider, priority, blocking indicator.<br>
 * <p>This class also provides the API for handling JPPF-annotated tasks and POJO tasks.
 * <p>All jobs have an id. It can be specified by calling {@link #setId(java.lang.String) setId(String)}.
 * If left unspecified, JPPF will automatically assign a uuid as its value.
 * @author Laurent Cohen
 */
public class JPPFJob implements Serializable
{
	/**
	 * The list of tasks to execute.
	 */
	private List<JPPFTask> tasks = null;
	/**
	 * The container for data shared between tasks.
	 * The data provider should be considered read-only, i.e. no modification will be returned back to the client application.
	 */
	private DataProvider dataProvider = null;
	/**
	 * The listener that receives notifications of completed tasks.
	 */
	private transient TaskResultListener resultsListener = null;
	/**
	 * Determines whether the execution of this job is blocking on the client side.
	 */
	private boolean blocking = true;
	/**
	 * The universal unique id for this job.
	 */
	private String id = null;
	/**
	 * The list of tasks to execute.
	 */
	private List<JPPFTask> results = null;
	/**
	 * The service level agreement between the job and the server.
	 */
	private JPPFJobSLA jobSLA = new JPPFJobSLA();

	/**
	 * Default constructor, creates a blocking job with no data provider, default SLA values and a priority of 0.
	 * This constructor generates a pseudo-random id as a string of 32 hexadecimal characters.
	 */
	public JPPFJob()
	{
		id = new JPPFUuid(JPPFUuid.HEXADECIMAL, 32).toString();
	}

	/**
	 * Initialize a blocking job with the specified parameters.
	 * @param dataProvider the container for data shared between tasks.
	 */
	public JPPFJob(DataProvider dataProvider)
	{
		this(dataProvider, null, true, null);
	}

	/**
	 * Initialize a blocking job with the specified parameters.
	 * @param dataProvider the container for data shared between tasks.
	 * @param jobSLA sevice level agreement between job and server.
	 */
	public JPPFJob(DataProvider dataProvider, JPPFJobSLA jobSLA)
	{
		this(dataProvider, jobSLA, true, null);
	}

	/**
	 * Initialize a non-blocking job with the specified parameters.
	 * @param resultsListener the listener that receives notifications of completed tasks.
	 */
	public JPPFJob(TaskResultListener resultsListener)
	{
		this(null, null, false, resultsListener);
	}

	/**
	 * Initialize a non-blocking job with the specified parameters.
	 * @param dataProvider the container for data shared between tasks.
	 * @param resultsListener the listener that receives notifications of completed tasks.
	 */
	public JPPFJob(DataProvider dataProvider, TaskResultListener resultsListener)
	{
		this(dataProvider, null, false, resultsListener);
	}

	/**
	 * Initialize a non-blocking job with the specified parameters.
	 * @param dataProvider the container for data shared between tasks.
	 * @param jobSLA sevice level agreement between job and server.
	 * @param resultsListener the listener that receives notifications of completed tasks.
	 */
	public JPPFJob(DataProvider dataProvider, JPPFJobSLA jobSLA, TaskResultListener resultsListener)
	{
		this(dataProvider, jobSLA, false, resultsListener);
	}

	/**
	 * Initialize a job with the specified parameters.
	 * @param dataProvider the container for data shared between tasks.
	 * @param jobSLA sevice level agreement between job and server.
	 * @param blocking determines whether this job is blocking.
	 * @param resultsListener the listener that receives notifications of completed tasks.
	 */
	public JPPFJob(DataProvider dataProvider, JPPFJobSLA jobSLA, boolean blocking, TaskResultListener resultsListener)
	{
		this();
		this.dataProvider = dataProvider;
		if (jobSLA != null) this.jobSLA = jobSLA;
		this.resultsListener = resultsListener;
		this.blocking = blocking;
	}

	/**
	 * Get the universal unique id for this job.
	 * @return the uuid as a string. 
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Set the universal unique id for this job.
	 * @param id the id as a string. 
	 */
	public void setId(String id)
	{
		this.id = id;
	}

	/**
	 * Get the list of tasks to execute.
	 * @return a list of objects.
	 */
	public List<JPPFTask> getTasks()
	{
		return tasks;
	}

	/**
	 * Add a task to this job. This method is for adding a task that is either an instance of {@link org.jppf.server.protocol.JPPFTask JPPFTask},
	 * annotated with {@link org.jppf.server.protocol.JPPFRunnable JPPFRunnable}, or an instance of {@link java.lang.Runnable Runnable} or {@link java.util.concurrent.Callable Callable}.
	 * @param taskObject the task to add to this job.
	 * @param args arguments to use with a JPPF-annotated class.
	 * @return an instance of <code>JPPFTask</code> that is either the same as the input if the input is a subclass of <code>JPPFTask</code>,
	 * or a wrapper around the input object in the other cases.
	 * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
	 */
	public JPPFTask addTask(Object taskObject, Object...args) throws JPPFException
	{
		JPPFTask tmp = null;
		if (taskObject == null) throw new JPPFException("null tasks are not accepted");
		if (taskObject instanceof JPPFTask) tmp = (JPPFTask) taskObject;
		else tmp = new JPPFAnnotatedTask(taskObject, args);
		if (tasks == null) tasks = new ArrayList<JPPFTask>();
		tasks.add(tmp);
		return tmp;
	}

	/**
	 * Add a POJO task to this job. The POJO task is identified as a method name associated with either an object for a non-static method,
	 * or a class for a static method or for a constructor.
	 * @param taskObject the task to add to this job.
	 * @param method the name of the method to execute.
	 * @param args arguments to use with a JPPF-annotated class.
	 * @return an instance of <code>JPPFTask</code> that is a wrapper around the input task object.
	 * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
	 */
	public JPPFTask addTask(String method, Object taskObject, Object...args) throws JPPFException
	{
		if (taskObject == null) throw new JPPFException("null tasks are not accepted");
		if (tasks == null) tasks = new ArrayList<JPPFTask>();
		JPPFTask jppfTask = new JPPFAnnotatedTask(taskObject, method, args);
		tasks.add(jppfTask);
		return jppfTask;
	}

	/**
	 * Get the container for data shared between tasks.
	 * @return a <code>DataProvider</code> instance.
	 */
	public DataProvider getDataProvider()
	{
		return dataProvider;
	}

	/**
	 * Set the container for data shared between tasks.
	 * @param dataProvider a <code>DataProvider</code> instance.
	 */
	public void setDataProvider(DataProvider dataProvider)
	{
		this.dataProvider = dataProvider;
	}

	/**
	 * Get the listener that receives notifications of completed tasks.
	 * @return a <code>TaskCompletionListener</code> instance.
	 */
	public TaskResultListener getResultListener()
	{
		return resultsListener;
	}

	/**
	 * Set the listener that receives notifications of completed tasks.
	 * @param resultsListener a <code>TaskCompletionListener</code> instance.
	 */
	public void setResultListener(TaskResultListener resultsListener)
	{
		this.resultsListener = resultsListener;
		blocking = false;
	}

	/**
	 * Determine whether the execution of this job is blocking on the client side.
	 * @return true if the execution is blocking, false otherwise.
	 */
	public boolean isBlocking()
	{
		return blocking;
	}

	/**
	 * Specify whether the execution of this job is blocking on the client side.
	 * @param blocking true if the execution is blocking, false otherwise.
	 */
	public void setBlocking(boolean blocking)
	{
		this.blocking = blocking;
	}

	/**
	 * Get the tasks execution policy.
	 * @return an <code>ExecutionPolicy</code> instance.
	 * @deprecated use {@link org.jppf.server.protocol.JPPFJobSLA#getExecutionPolicy() JPPFJobSLA.getExecutionPolicy()} instead.
	 */
	public ExecutionPolicy getExecutionPolicy()
	{
		return jobSLA.getExecutionPolicy();
	}

	/**
	 * Set the tasks execution policy.
	 * @param executionPolicy an <code>ExecutionPolicy</code> instance.
	 * @deprecated use {@link org.jppf.server.protocol.JPPFJobSLA#setExecutionPolicy(org.jppf.node.policy.ExecutionPolicy) JPPFJobSLA.setExecutionPolicy(ExecutionPolicy)} instead.
	 */
	public void setExecutionPolicy(ExecutionPolicy executionPolicy)
	{
		jobSLA.setExecutionPolicy(executionPolicy);
	}

	/**
	 * Get the priority of this job.
	 * @return the priority as an int.
	 * @deprecated use {@link org.jppf.server.protocol.JPPFJobSLA#getPriority() JPPFJobSLA.getPriority()} instead.
	 */
	public int getPriority()
	{
		return jobSLA.getPriority();
	}

	/**
	 * Set the priority of this job.
	 * @param priority the priority as an int.
	 * @deprecated use {@link org.jppf.server.protocol.JPPFJobSLA#setPriority(int) JPPFJobSLA.setPriority(int)} instead.
	 */
	public void setPriority(int priority)
	{
		jobSLA.setPriority(priority);
	}

	/**
	 * Get the service level agreement between the job and the server.
	 * @return an instance of <code>JPPFJobSLA</code>.
	 */
	public JPPFJobSLA getJobSLA()
	{
		return jobSLA;
	}

	/**
	 * Get the service level agreement between the job and the server.
	 * @param jobSLA an instance of <code>JPPFJobSLA</code>.
	 */
	public void setJobSLA(JPPFJobSLA jobSLA)
	{
		this.jobSLA = jobSLA;
	}
}
