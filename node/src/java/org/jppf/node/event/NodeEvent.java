/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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
package org.jppf.node.event;

import java.util.EventObject;

/**
 * Intances of this class describe events that occur within a nodes life cycle.
 * @author Laurent Cohen
 */
public class NodeEvent extends EventObject
{
	/**
	 * Enumeration of all possible event types.
	 */
	public enum EventType
	{
		/**
		 * Event type to specify a node is about to attempt connecting to the server. 
		 */
		START_CONNECT,
		/**
		 * Event type to specify a node has successfully connected to the server. 
		 */
		END_CONNECT,
		/**
		 * Event type to specify a node is disconnected from the server. 
		 */
		DISCONNECTED,
		/**
		 * Event type to specify a node is starting to execute one or more tasks,
		 * and is switching from idle to executing state. 
		 */
		START_EXEC,
		/**
		 * Event type to specify a node finished executing one or more tasks,
		 * and is switching from executing to idle state. 
		 */
		END_EXEC,
		/**
		 * Event type to specify a task was executed. 
		 */
		TASK_EXECUTED
	}

	/**
	 * Initialize this event with a specified event source.
	 * @param source the source of the event.
	 */
	public NodeEvent(EventType source)
	{
		super(source);
	}
	
	/**
	 * Get the type of this event.
	 * @return the type of event as an enumerated value.
	 */
	public EventType getType()
	{
		return (EventType) getSource();
	}
}
