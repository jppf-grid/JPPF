/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
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
	 * 
	 */
	private static final long serialVersionUID = -8382232218856942352L;
	/**
	 * Event type to specify a node is about to attempt connecting to the server. 
	 */
	public static String START_CONNECT = "start.connect";
	/**
	 * Event type to specify a node has successfully connected to the server. 
	 */
	public static String END_CONNECT = "end.connect";
	/**
	 * Event type to specify a node is disconnected form the server. 
	 */
	public static String DISCONNECTED = "disconnected";
	/**
	 * Event type to specify a node is starting to execute a task. 
	 */
	public static String START_EXEC = "start.exec";
	/**
	 * Event type to specify a node finished executing a task. 
	 */
	public static String END_EXEC = "end.exec";

	/**
	 * Initialize this event with a specified event source.
	 * @param source the source of the event.
	 */
	public NodeEvent(String source)
	{
		super(source);
	}
	
	/**
	 * Get the type of this event.
	 * @return the type as a string.
	 */
	public String getType()
	{
		return (String) getSource();
	}
}
