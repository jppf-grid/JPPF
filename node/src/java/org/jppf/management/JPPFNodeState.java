/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

package org.jppf.management;

import java.io.Serializable;
import java.util.*;

import org.jppf.node.event.NodeEventType;

/**
 * Instances of this class represent the state of a node.
 * They are used as the result of node JMX monitoring request.
 * @author Laurent Cohen
 */
public class JPPFNodeState implements Serializable
{
	/**
	 * The latest event received from a task.
	 */
	private Serializable taskEvent = "";
	/**
	 * Status of the connection between the node and the server.
	 */
	private String connectionStatus = NodeEventType.UNKNOWN.toString();
	/**
	 * Latest execution status of the node.
	 */
	private String executionStatus = NodeEventType.UNKNOWN.toString();
	/**
	 * The number of tasks executed by the node.
	 */
	private int nbTasksExecuted = 0;
	/**
	 * Holder for all latest task notifications issued from multiple execution threads. 
	 */
	private transient Map<String, Integer> taskIdMap = new HashMap<String, Integer>();
	/**
	 * List of the ids of all tasks currently executing.
	 */
	private Set<String> taskIdSet = new HashSet<String>();
	/**
	 * Size of the node's thread pool.
	 */
	private int threadPoolSize = 1;

	/**
	 * Get the latest event received from a task.
	 * @return the event as an object.
	 */
	public synchronized Serializable getTaskNotification()
	{
		return taskEvent;
	}

	/**
	 * Set the latest event received from a task.
	 * @param taskEvent the event as an object.
	 */
	public synchronized void setTaskEvent(Serializable taskEvent)
	{
		this.taskEvent = taskEvent;
	}

	/**
	 * Get the number of tasks executed by the node.
	 * @return the number of tasks as an int.
	 */
	public synchronized int getNbTasksExecuted()
	{
		return nbTasksExecuted;
	}

	/**
	 * Set the number of tasks executed by the node.
	 * @param nbTasksExecuted the number of tasks as an int.
	 */
	public synchronized void setNbTasksExecuted(int nbTasksExecuted)
	{
		this.nbTasksExecuted = nbTasksExecuted;
	}

	/**
	 * Get the status of the connection between the node and the server.
	 * @return a string representing the connection status.
	 */
	public synchronized String getConnectionStatus()
	{
		return connectionStatus;
	}

	/**
	 * Set the status of the connection between the node and the server.
	 * @param connectionStatus a string representing the connection status.
	 */
	public synchronized void setConnectionStatus(String connectionStatus)
	{
		this.connectionStatus = connectionStatus;
	}

	/**
	 * Get the latest execution status of the node.
	 * @return a string representing the execution status.
	 */
	public synchronized String getExecutionStatus()
	{
		return executionStatus;
	}

	/**
	 * Get the latest execution status of the node.
	 * @param executionStatus a string representing the execution status.
	 */
	public synchronized void setExecutionStatus(String executionStatus)
	{
		this.executionStatus = executionStatus;
	}

	/**
	 * Notification that a task with the specified id has started.
	 * @param id the id of the task.
	 */
	public synchronized void taskStarted(String id)
	{
		if (id == null) return;
		Integer n = taskIdMap.get(id);
		if (n == null) n = 1;
		else n = n + 1;
		if (!taskIdSet.contains(id)) taskIdSet.add(id);
		taskIdMap.put(id, n);
	}

	/**
	 * Notification that a task with the specified id has ended.
	 * @param id the id of the task.
	 */
	public synchronized void taskEnded(String id)
	{
		if (id == null) return;
		Integer n = taskIdMap.get(id);
		if (n == null) return;
		n = n - 1;
		if (n <= 0)
		{
			taskIdMap.remove(id);
			taskIdSet.remove(id);
		}
		else taskIdMap.put(id, n);
	}

	/**
	 * Get the ids of all currently executing tasks.
	 * @return the ids as a set of strings.
	 */
	public synchronized Set<String> getAllTaskIds()
	{
		return Collections.unmodifiableSet(taskIdSet);
	}

	/**
	 * Set the ids of all currently executing tasks.
	 * @param taskIdSet the ids as a set of strings.
	 */
	public synchronized void setTaskIdSet(Set<String> taskIdSet)
	{
		this.taskIdSet = taskIdSet;
	}

	/**
	 * Get the size of the node's thread pool.
	 * @return the size as an int.
	 */
	public int getThreadPoolSize()
	{
		return threadPoolSize;
	}

	/**
	 * Set the size of the node's thread pool.
	 * @param threadPoolSize the size as an int.
	 */
	public void setThreadPoolSize(int threadPoolSize)
	{
		this.threadPoolSize = threadPoolSize;
	}
}
