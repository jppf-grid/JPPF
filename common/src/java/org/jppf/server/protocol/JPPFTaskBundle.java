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
package org.jppf.server.protocol;

import java.io.Serializable;
import java.util.*;

import org.jppf.utils.*;

/**
 * Instances of this class group tasks from the same client together, so they are sent to the same node,
 * avoiding unnecessary transport overhead.<br>
 * The goal is to provide a performance enhancement through an adaptive bundling of tasks originating from the same client.
 * The bundle size is computed dynamically, depending on the number of nodes connected to the server, and other factors.
 * @author Laurent Cohen
 */
public class JPPFTaskBundle implements Serializable, Comparable<JPPFTaskBundle>, JPPFDistributedJob
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Type safe enumeration for the values of the bundle state.
	 */
	public enum State
	{
		/**
		 * Means the bundle is used for handshake with the server (inital bundle).
		 */
		INITIAL_BUNDLE,
		/**
		 * Means the bundle is used normally, to transport executable tasks.
		 */
		EXECUTION_BUNDLE
	}

	/**
	 * The unique identifier for this task bundle.
	 */
	private String uuid = null;
	/**
	 * The unique identifier for the request this task bundle is a part of.
	 */
	private String requestUuid = null;
	/**
	 * The unique identifier for the submitting application.
	 */
	private TraversalList<String> uuidPath = new TraversalList<String>();
	/**
	 * The number of tasks in this bundle.
	 */
	private int taskCount = 0;
	/**
	 * The initial number of tasks in this bundle.
	 */
	private int initialTaskCount = 0;
	/**
	 * The shared data provider for this task bundle.
	 */
	private transient byte[] dataProvider = null;
	/**
	 * The tasks to be executed by the node.
	 */
	private transient List<byte[]> tasks = null;
	/**
	 * The time at which this wrapper was added to the queue.
	 */
	private transient long queueEntryTime = 0L;
	/**
	 * The task completion listener to notify, once the execution of this task has completed.
	 */
	private transient TaskCompletionListener completionListener = null;
	/**
	 * The time it took a node to execute this task.
	 */
	private long nodeExecutionTime = 0L;
	/**
	 * The time at which the bundle is taken out of the queue fir sending to a node.
	 */
	private long executionStartTime = 0L;
	/**
	 * The build number of the current version of JPPF. 
	 */
	private int buildNumber = 0;
	/**
	 * The state of this bundle, to indicate whether it is used for handshake with
	 * the server or for transporting tasks to execute.
	 */
	private State state = State.EXECUTION_BUNDLE;
	/**
	 * Map holding the parameters of the request.
	 */
	private Map<Object, Object> parameters = new HashMap<Object, Object>();
	/**
	 * The service level agreement between the job and the server.
	 */
	private JPPFJobSLA jobSLA = new JPPFJobSLA();

	/**
	 * Initialize this task bundle and set its build number.
	 */
	public JPPFTaskBundle()
	{
		buildNumber = VersionUtils.getBuildNumber();
	}

	/**
	 * Get the unique identifier for this task bundle.
	 * @return the uuid as a string.
	 */
	public String getBundleUuid()
	{
		return uuid;
	}

	/**
	 * Set the unique identifier for this task bundle.
	 * @param uuid the uuid as a string.
	 */
	public void setBundleUuid(String uuid)
	{
		this.uuid = uuid;
	}

	/**
	 * Get the unique identifier for the request this task is a part of.
	 * @return the request uuid as a string.
	 */
	public String getRequestUuid()
	{
		return requestUuid;
	}

	/**
	 * Set the unique identifier for the request this task is a part of.
	 * @param requestUuid the request uuid as a string.
	 */
	public void setRequestUuid(String requestUuid)
	{
		this.requestUuid = requestUuid;
	}

	/**
	 * Get shared data provider for this task.
	 * @return a <code>DataProvider</code> instance.
	 */
	public byte[] getDataProvider()
	{
		return dataProvider;
	}

	/**
	 * Set shared data provider for this task.
	 * @param dataProvider a <code>DataProvider</code> instance.
	 */
	public void setDataProvider(byte[] dataProvider)
	{
		this.dataProvider = dataProvider;
	}

	/**
	 * Get the uuid path of the applications (driver or client) in whose classpath the class definition may be found. 
	 * @return the uuid path as a list of string elements.
	 */
	public TraversalList<String> getUuidPath()
	{
		return uuidPath;
	}

	/**
	 * Set the uuid path of the applications (driver or client) in whose classpath the class definition may be found. 
	 * @param uuidPath the uuid path as a list of string elements.
	 */
	public void setUuidPath(TraversalList<String> uuidPath)
	{
		this.uuidPath = uuidPath;
	}

	/**
	 * Get the time at which this wrapper was added to the queue.
	 * @return the time as a long value.
	 */
	public long getQueueEntryTime()
	{
		return queueEntryTime;
	}

	/**
	 * Set the time at which this wrapper was added to the queue.
	 * @param queueEntryTime the time as a long value.
	 */
	public void setQueueEntryTime(long queueEntryTime)
	{
		this.queueEntryTime = queueEntryTime;
	}

	/**
	 * Get the time it took a node to execute this task.
	 * @return the time in milliseconds as a long value.
	 */
	public long getNodeExecutionTime()
	{
		return nodeExecutionTime;
	}

	/**
	 * Set the time it took a node to execute this task.
	 * @param nodeExecutionTime the time in milliseconds as a long value.
	 */
	public void setNodeExecutionTime(long nodeExecutionTime)
	{
		this.nodeExecutionTime = nodeExecutionTime;
	}

	/**
	 * Get the tasks to be executed by the node.
	 * @return the tasks as a <code>List</code> of arrays of bytes.
	 */
	public List<byte[]> getTasks()
	{
		return tasks;
	}

	/**
	 * Set the tasks to be executed by the node.
	 * @param tasks the tasks as a <code>List</code> of arrays of bytes.
	 */
	public void setTasks(List<byte[]> tasks)
	{
		this.tasks = tasks;
	}

	/**
	 * Get the number of tasks in this bundle.
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
		if (initialTaskCount <= 0) initialTaskCount = taskCount;
	}

	/**
	 * Get the task completion listener to notify, once the execution of this task has completed.
	 * @return a <code>TaskCompletionListener</code> instance.
	 */
	public TaskCompletionListener getCompletionListener()
	{
		return completionListener;
	}

	/**
	 * Set the task completion listener to notify, once the execution of this task has completed.
	 * @param listener a <code>TaskCompletionListener</code> instance.
	 */
	public void setCompletionListener(TaskCompletionListener listener)
	{
		this.completionListener = listener;
	}

	/**
	 * Compare two task bundles, based on their respective priorities.<br>
	 * <b>Note:</b> <i>this class has a natural ordering that is inconsistent with equals.</i>
	 * @param bundle the bundle compare this one to.
	 * @return a positive int if this bundle is greater, 0 if both are equal,
	 * or a negative int if this bundless is less than the other.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(JPPFTaskBundle bundle)
	{
		if (bundle == null) return 1;
		int otherPriority = bundle.getJobSLA().getPriority();
		if (jobSLA.getPriority() < otherPriority) return -1;
		if (jobSLA.getPriority() > otherPriority) return 1;
		return 0;
	}

	/**
	 * Get the build number under which this task bundle was created.
	 * @return the build number as an int value.
	 */
	public int getBuildNumber()
	{
		return buildNumber;
	}

	/**
	 * Make a copy of this bundle.
	 * @return a new <code>JPPFTaskBundle</code> instance.
	 */
	public JPPFTaskBundle copy()
	{
		JPPFTaskBundle bundle = new JPPFTaskBundle();
		bundle.setBundleUuid(uuid);
		bundle.setUuidPath(uuidPath);
		bundle.setRequestUuid(requestUuid);
		bundle.setTaskCount(taskCount);
		bundle.setDataProvider(dataProvider);
		synchronized(bundle.getParametersMap())
		{
			for (Map.Entry<Object, Object> entry: parameters.entrySet()) bundle.setParameter(entry.getKey(), entry.getValue());
		}
		bundle.setQueueEntryTime(queueEntryTime);
		bundle.setCompletionListener(completionListener);
		bundle.setJobSLA(jobSLA);
		//bundle.setParameter(BundleParameter.JOB_METADATA, getJobMetadata());

		return bundle;
	}

	/**
	 * Make a copy of this bundle containing only the first nbTasks tasks it contains.
	 * @param nbTasks the number of tasks to include in the copy.
	 * @return a new <code>JPPFTaskBundle</code> instance.
	 */
	public JPPFTaskBundle copy(int nbTasks)
	{
		JPPFTaskBundle bundle = copy();
		bundle.setTaskCount(nbTasks);
		taskCount -= nbTasks;
		return bundle;
	}

	/**
	 * Get the state of this bundle.
	 * @return a <code>State</code> type safe enumeration value.
	 */
	public State getState()
	{
		return state;
	}

	/**
	 * Set the state of this bundle.
	 * @param state a <code>State</code> type safe enumeration value.
	 */
	public void setState(State state)
	{
		this.state = state;
	}

	/**
	 * Get the time at which the bundle is taken out of the queue for sending to a node.
	 * @return the time as a long value.
	 */
	public long getExecutionStartTime()
	{
		return executionStartTime;
	}

	/**
	 * Set the time at which the bundle is taken out of the queue for sending to a node.
	 * @param executionStartTime the time as a long value.
	 */
	public void setExecutionStartTime(long executionStartTime)
	{
		this.executionStartTime = executionStartTime;
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
	 * Set a parameter of this request.
	 * @param name the name of the parameter to set.
	 * @param value the value of the parameter to set.
	 */
	public void setParameter(Object name, Object value)
	{
		synchronized(parameters)
		{
			parameters.put(name, value);
		}
	}

	/**
	 * Get the value of a parameter of this request.
	 * @param name the name of the parameter to get.
	 * @return the value of the parameter, or null if the parameter is not set.
	 */
	public Object getParameter(Object name)
	{
		return parameters.get(name);
	}

	/**
	 * Get the value of a parameter of this request.
	 * @param name the name of the parameter to get.
	 * @param defaultValue the default value to return if the parameter is not set.
	 * @return the value of the parameter, or <code>defaultValue</code> if the parameter is not set.
	 */
	public Object getParameter(Object name, Object defaultValue)
	{
		Object res = parameters.get(name);
		return res == null ? defaultValue : res;
	}

	/**
	 * Remove a parameter from this request.
	 * @param name the name of the parameter to remove.
	 * @return the value of the parameter to remove, or null if the parameter is not set.
	 */
	public Object removeParameter(Object name)
	{
		return parameters.remove(name);
	}

	/**
	 * Get the map holding the parameters of the request.
	 * @return a map of string keys to object values.
	 */
	public Map<Object, Object> getParametersMap()
	{
		return parameters;
	}

	/**
	 * {@inheritDoc}
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

	/**
	 * {@inheritDoc}
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder("[");
		sb.append("jobId=").append(getParameter(BundleParameter.JOB_ID));
		sb.append(", jobUuid=").append(getParameter(BundleParameter.JOB_UUID));
		sb.append(", initialTaskCount=").append(initialTaskCount);
		sb.append(", taskCount=").append(taskCount);
		sb.append(", requeue=").append(getParameter(BundleParameter.JOB_REQUEUE));
		sb.append("]");
		return sb.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getId()
	{
		return (String) getParameter(BundleParameter.JOB_ID);
	}

	/**
	 * {@inheritDoc}
	 */
	public JPPFJobMetadata getJobMetadata()
	{
		return (JPPFJobMetadata) getParameter(BundleParameter.JOB_METADATA);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getJobUuid()
	{
		return (String) getParameter(BundleParameter.JOB_UUID);
	}
}
