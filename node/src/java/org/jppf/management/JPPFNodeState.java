/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

import org.jppf.node.event.NodeEvent;

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
	private Object taskEvent = "";
	/**
	 * Type of the latest life cycle event of the node.
	 */
	private NodeEvent.EventType nodeEventType = NodeEvent.EventType.UNKNOWN;
	/**
	 * The number of tasks executed by the node.
	 */
	private int nbTasksExecuted = 0;

	/**
	 * Get the latest event received from a task.
	 * @return the event as an object.
	 */
	public synchronized Object getTaskEvent()
	{
		return taskEvent;
	}

	/**
	 * Set the latest event received from a task.
	 * @param taskEvent the event as an object.
	 */
	public synchronized void setTaskEvent(Object taskEvent)
	{
		this.taskEvent = taskEvent;
	}

	/**
	 * Get the type of the latest life cycle event of the node.
	 * @return a <code>NodeEvent.EventType</code> enum value.
	 */
	public synchronized NodeEvent.EventType getNodeEventType()
	{
		return nodeEventType;
	}

	/**
	 * Set the type of the latest life cycle event of the node.
	 * @param nodeEventType a <code>NodeEvent.EventType</code> enum value.
	 */
	public synchronized void setNodeEventType(NodeEvent.EventType nodeEventType)
	{
		this.nodeEventType = nodeEventType;
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
}
