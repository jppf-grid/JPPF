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

package org.jppf.client;

import java.io.Serializable;
import java.util.*;

import org.jppf.JPPFException;
import org.jppf.client.event.*;
import org.jppf.client.taskwrapper.JPPFAnnotatedTask;
import org.jppf.node.protocol.*;
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
public class JPPFJob implements Serializable, JPPFDistributedJob
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The list of tasks to execute.
	 */
	private final List<JPPFTask> tasks = new ArrayList<JPPFTask>();
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
	 * The user-defined display name for this job.
	 */
	private String name = null;
	/**
	 * The universal unique id for this job.
	 */
	private String jobUuid = null;
	/**
	 * The service level agreement between the job and the server.
	 */
	private JobSLA jobSLA = new JPPFJobSLA();
	/**
	 * The user-defined metadata asoociated with this job.
	 */
	private JobMetadata jobMetadata = new JPPFJobMetadata();
	/**
	 * The number of tasks in this job.
	 */
	private int taskCount = 0;
	/**
	 * The object that holds the results of executed tasks.
	 */
	private final JobResults results = new JobResults();
	/**
	 * The list of listeners registered with this job.
	 */
	private final List<JobListener> listeners = new ArrayList<JobListener>();

	/**
	 * Default constructor, creates a blocking job with no data provider, default SLA values and a priority of 0.
	 * This constructor generates a pseudo-random id as a string of 32 hexadecimal characters.
	 */
	public JPPFJob()
	{
		this(new JPPFUuid(JPPFUuid.HEXADECIMAL, 32).toString());
	}

	/**
	 * Default constructor, creates a blocking job with no data provider, default SLA values and a priority of 0.
	 * This constructor generates a pseudo-random id as a string of 32 hexadecimal characters.
	 * @param jobUuid the uuid to assign to this job.
	 */
	public JPPFJob(String jobUuid)
	{
		this.jobUuid = (jobUuid == null) ? new JPPFUuid(JPPFUuid.HEXADECIMAL, 32).toString() : jobUuid;
		name = jobUuid;
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
		this(dataProvider, jobSLA, null, blocking, resultsListener);
	}

	/**
	 * Initialize a job with the specified parameters.
	 * @param dataProvider the container for data shared between tasks.
	 * @param jobSLA sevice level agreement between job and server.
	 * @param jobMetadata the user-defined job metadata.
	 * @param blocking determines whether this job is blocking.
	 * @param resultsListener the listener that receives notifications of completed tasks.
	 */
	public JPPFJob(DataProvider dataProvider, JPPFJobSLA jobSLA, JPPFJobMetadata jobMetadata, boolean blocking, TaskResultListener resultsListener)
	{
		this();
		this.dataProvider = dataProvider;
		if (jobSLA != null) this.jobSLA = jobSLA;
		if (jobMetadata != null) this.jobMetadata = jobMetadata;
		this.resultsListener = resultsListener;
		this.blocking = blocking;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getJobUuid()
	{
		return jobUuid;
	}

	/**
	 * {@inheritDoc}
	 * @deprecated use {@link #getName() getName()} instead.
	 */
	@Override
	public String getId()
	{
		return name;
	}

	/**
	 * Set the user-defined display name for this job.
	 * @param id the display name as a string. 
	 * @deprecated use {@link #setName(java.lang.String) setName(String)} instead.
	 */
	public void setId(String id)
	{
		this.name = id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName()
	{
		return name;
	}

	/**
	 * Set the user-defined display name for this job.
	 * @param name the display name as a string. 
	 */
	public void setName(String name)
	{
		this.name = name;
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
	 * Get the list of tasks that have not yet been executed.
	 * @return a list of <code>JPPFTask</code> objects.
	 */
	public synchronized List<JPPFTask> getPendingTasks()
	{
		List<JPPFTask> list = new LinkedList<JPPFTask>();
		for (JPPFTask t: tasks)
		{
			if (!results.hasResult(t.getPosition())) list.add(t);
		}
		return list;
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
		JPPFTask jppfTask = null;
		if (taskObject == null) throw new JPPFException("null tasks are not accepted");
		if (taskObject instanceof JPPFTask) jppfTask = (JPPFTask) taskObject;
		else jppfTask = new JPPFAnnotatedTask(taskObject, args);
		tasks.add(jppfTask);
		jppfTask.setPosition(taskCount++);
		return jppfTask;
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
		JPPFTask jppfTask = new JPPFAnnotatedTask(taskObject, method, args);
		tasks.add(jppfTask);
		jppfTask.setPosition(taskCount++);
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
	 * {@inheritDoc}
	 * @deprecated use {@link #getSLA() getSLA()} instead
	 */
	public JPPFJobSLA getJobSLA()
	{
		return (JPPFJobSLA) jobSLA;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JobSLA getSLA()
	{
		return jobSLA;
	}

	/**
	 * Get the service level agreement between the job and the server.
	 * @param jobSLA an instance of <code>JPPFJobSLA</code>.
	 */
	public void setSLA(JobSLA jobSLA)
	{
		this.jobSLA = jobSLA;
	}

	/**
	 * {@inheritDoc}
	 * @deprecated use {@link #getMetadata() getMetadata()} instead
	 */
	public JPPFJobMetadata getJobMetadata()
	{
		return (JPPFJobMetadata) jobMetadata;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JobMetadata getMetadata()
	{
		return jobMetadata;
	}

	/**
	 * Set this job's metadata.
	 * @param jobMetadata a {@link JPPFJobMetadata} instance.
	 */
	public void setMetadata(JobMetadata jobMetadata)
	{
		this.jobMetadata = jobMetadata;
	}

	/**
	 * COmpute the hascode of this job.
	 * @return th hascode as an int.
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jobUuid == null) ? 0 : jobUuid.hashCode());
		return result;
	}

	/**
	 * Determine whether this object is equal to another.
	 * @param obj the object to compare with.
	 * @return true if the two objects are equal, false otherwise.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof JPPFJob)) return false;
		JPPFJob other = (JPPFJob) obj;
		if (jobUuid == null) return other.jobUuid == null;
		return jobUuid.equals(other.jobUuid);
	}

	/**
	 * Get the object that holds the results of executed tasks.
	 * @return a {@link JobResults} instance.
	 */
	public JobResults getResults()
	{
		return results;
	}

	/**
	 * Add a listener to the list of job listeners.
	 * @param listener a {@link JobListener} instance.
	 */
	public void addJobListener(JobListener listener)
	{
		synchronized(listeners)
		{
			listeners.add(listener);
		}
	}

	/**
	 * Add a listener to the list of job listeners.
	 * @param listener a {@link JobListener} instance.
	 */
	public void removeJobListener(JobListener listener)
	{
		synchronized(listeners)
		{
			listeners.remove(listener);
		}
	}

	/**
	 * Notify all listeners of the specified event type.
	 * @param type the type of the event.
	 */
	public void fireJobEvent(JobEvent.Type type)
	{
		JobEvent event = new JobEvent(this);
		synchronized(listeners)
		{
			for (JobListener listener: listeners)
			{
				switch(type)
				{
					case JOB_START:
						listener.jobStarted(event);
						break;
					case JOB_END:
						listener.jobEnded(event);
						break;
				}
			}
		}
	}
}
