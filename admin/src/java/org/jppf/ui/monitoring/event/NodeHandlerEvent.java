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
package org.jppf.ui.monitoring.event;

import java.util.EventObject;

import org.jppf.ui.monitoring.data.*;

/**
 * Event sent when the state of a node has changed.
 * @author Laurent Cohen
 */
public class NodeHandlerEvent extends EventObject
{
	/**
	 * Notification that a driver was added.
	 */
	public static final int ADD_DRIVER = 0;
	/**
	 * Notification that a driver was removed.
	 */
	public static final int REMOVE_DRIVER = 1;
	/**
	 * Notification that a node was added to a driver.
	 */
	public static final int ADD_NODE = 2;
	/**
	 * Notification that a node was removed from a driver.
	 */
	public static final int REMOVE_NODE = 3;
	/**
	 * Notification that the state of a node was updated.
	 */
	public static final int UPDATE_NODE = 4;
	/**
	 * The name of the driver to which the node is attached.
	 */
	private String driverName = null;
	/**
	 * The node connection and state information
	 */
	private transient NodeInfoHolder infoHolder = null;

	/**
	 * Initialize this event with a specified source <code>NodeHandler</code>.
	 * @param source the node handler whose data has changed.
	 * @param driverName The name of the driver to which the node is attached.
	 * @param infoHolder The node connection and state information.
	 * @param operation the type of operation to notify for. The value must be one of
	 * ADD_DRIVER, REMOVE_DRIVER, ADD_NODE, RENOVE_NODE or UPDATE_NODE.
	 */
	public NodeHandlerEvent(NodeHandler source, String driverName, NodeInfoHolder infoHolder, int operation)
	{
		super(source);
		this.driverName = driverName;
		this.infoHolder = infoHolder;
	}
	
	/**
	 * Get the <code>NodeHandler</code> source of this event.
	 * @return a <code>NodeHandler</code> instance.
	 */
	public NodeHandler getNodeHandler()
	{
		return (NodeHandler) getSource();
	}

	/**
	 * Get the name of the driver to which the node is attached.
	 * @return the name as a string. 
	 */
	public String getDriverName()
	{
		return driverName;
	}

	/**
	 * Get the node connection and state information.
	 * @return a <code>NodeInfoHolder</code> instance.
	 */
	public NodeInfoHolder getInfoHolder()
	{
		return infoHolder;
	}
}
