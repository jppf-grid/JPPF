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
package org.jppf.node.event;

import java.util.EventObject;

/**
 * Intances of this class describe events that occur within a nodes life cycle.
 * @author Laurent Cohen
 */
public class NodeEvent extends EventObject
{
	/**
	 * Holds the number of executed tasks.
	 */
	private int nbTasks = 0;
	
	/**
	 * Initialize this event with a specified event source.
	 * @param source the source of the event.
	 */
	public NodeEvent(NodeEventType source)
	{
		super(source);
	}
	
	/**
	 * Create an event for the execution of a specified number of tasks.
	 * @param nbTasks the number of tasks as an int.
	 */
	public NodeEvent(int nbTasks)
	{
		super(NodeEventType.TASK_EXECUTED);
		this.nbTasks = nbTasks;
	}
	
	/**
	 * Get the type of this event.
	 * @return the type of event as an enumerated value.
	 */
	public NodeEventType getType()
	{
		return (NodeEventType) getSource();
	}

	/**
	 * Get the number of executed tasks.
	 * @return the number of tasks as an int.
	 */
	public int getNbTasks()
	{
		return nbTasks;
	}
}
