/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.server;

import java.io.Serializable;
import java.util.*;
import org.jppf.security.JPPFSecurityContext;
import org.jppf.server.event.TaskCompletionListener;
import org.jppf.utils.VersionUtils;

/**
 * Instances of this class group tasks from the same client together, so they are sent to the same node,
 * avoiding unnecessary transport overhead.<br>
 * The goal is to provide a performance enhancement through an adaptive bundling of tasks originating from the same client.
 * The bundle size is computed dynamically, depending on the number of nodes connected to the server, and other factors.
 * @author Laurent Cohen
 */
public class JPPFTaskBundle implements Serializable, Comparable<JPPFTaskBundle>
{
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
	 * The unique identifier for the request this task is a part of.
	 */
	private String requestUuid = null;
	/**
	 * The unique identifier for the submitting application.
	 */
	private String appUuid = null;
	/**
	 * The number of tasks in this bundle.
	 */
	private int taskCount = 0;
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
	 * The priority of this task bundle.
	 */
	private int priority = 0;
	/**
	 * The build number of the current version of JPPF. 
	 */
	private int buildNumber = 0;
	/**
	 * Security credentials associated with the sender of this bundle.
	 */
	private JPPFSecurityContext credentials = null;
	/**
	 * The state of this bundle, to indicate whether it is used for handshake with
	 * the server or for transporting tasks to execute.
	 */
	private State state = State.EXECUTION_BUNDLE;

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
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * Set the unique identifier for this task bundle.
	 * @param uuid the uuid as a string.
	 */
	public void setUuid(String uuid)
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
	 * Get the unique identifier for the submitting application.
	 * @return the application uuid as a string.
	 */
	public String getAppUuid()
	{
		return appUuid;
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
	 * Set the unique identifier for the submitting application.
	 * @param appUuid the application uuid as a string.
	 */
	public void setAppUuid(String appUuid)
	{
		this.appUuid = appUuid;
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
	 * Compare two task bundles, based on their respective priorities.
	 * @param bundle the bundle compare this one to.
	 * @return a positive int if this bundle is greater, 0 if both are equal,
	 * or a negative int if this bundless is less than the other.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(JPPFTaskBundle bundle)
	{
		if (bundle == null) return 1;
		if (priority < bundle.getPriority()) return -1;
		if (priority > bundle.getPriority()) return 1;
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
	 * Make a copy of this bundle containing only the first nbTasks tasks it contains.
	 * @param nbTasks the number of tasks to include in the copy.
	 * @return a new <code>JPPFTaskBundle</code> instance.
	 */
	public JPPFTaskBundle copy(int nbTasks)
	{
		JPPFTaskBundle bundle = new JPPFTaskBundle();
		bundle.setUuid(uuid);
		bundle.setAppUuid(appUuid);
		bundle.setRequestUuid(requestUuid);
		bundle.setTaskCount(nbTasks);
		bundle.setDataProvider(dataProvider);
		List<byte[]> list = new ArrayList<byte[]>();
		bundle.setTasks(list);
		for (int i=0; i<nbTasks; i++) list.add(tasks.get(0));
		taskCount -= nbTasks;
		bundle.setQueueEntryTime(queueEntryTime);
		bundle.setCompletionListener(completionListener);
		bundle.setCredentials(credentials);

		return bundle;
	}

	/**
	 * Get the security credentials associated with the sender of this bundle.
	 * @return a <code>JPPFCredential</code> instance.
	 */
	public JPPFSecurityContext getCredentials()
	{
		return credentials;
	}

	/**
	 * Set the security credentials associated with the sender of this bundle.
	 * @param credentials a <code>JPPFCredential</code> instance.
	 */
	public void setCredentials(JPPFSecurityContext credentials)
	{
		this.credentials = credentials;
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
}
