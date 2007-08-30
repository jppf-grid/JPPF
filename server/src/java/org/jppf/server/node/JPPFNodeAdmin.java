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

package org.jppf.server.node;

import static org.jppf.management.NodeParameter.COMMAND_PARAM;

import org.apache.commons.logging.*;
import org.jppf.management.*;
import org.jppf.node.event.*;
import org.jppf.server.protocol.*;

/**
 * Management bean for a JPPF node.
 * @author Laurent Cohen
 */
public class JPPFNodeAdmin implements JPPFNodeAdminMBean, JPPFTaskListener, NodeListener
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFNodeAdmin.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The latest event that occurred within a task.
	 */
	private JPPFNodeState nodeState = new JPPFNodeState();
	/**
	 * The node whose state is monitored.
	 */
	private transient JPPFNode node = null;

	/**
	 * Initialize this node management bean with the specified node.
	 * @param node the node whose state is monitored.
	 */
	public JPPFNodeAdmin (JPPFNode node)
	{
		this.node = node;
	}

	/**
	 * Perform a node administration request specified by its parameters.
	 * @param request an object specifying the request parameters.
	 * @return a <code>JPPFManagementResponse</code> instance.
	 * @see org.jppf.management.JPPFAdminMBean#performAdminRequest(org.jppf.management.JPPFManagementRequest)
	 */
	public JPPFManagementResponse performAdminRequest(JPPFManagementRequest<NodeParameter, Object> request)
	{
		if (debugEnabled) log.debug("received request: " + request);
		JPPFManagementResponse response = null;
		try
		{
			NodeParameter command = (NodeParameter) request.getParameter(COMMAND_PARAM);
			switch(command)
			{
				case REFRESH_STATE:
					response = new JPPFManagementResponse(state(), null);
					break;
			}
		}
		catch(Exception e)
		{
			response = new JPPFManagementResponse(e);
		}
		return response;
	}

	/**
	 * Get the latest state information from the node.
	 * @return a <code>JPPFNodeState</code> information.
	 * @see org.jppf.management.JPPFNodeAdminMBean#state()
	 */
	public JPPFNodeState state()
	{
		JPPFNodeState s = new JPPFNodeState();
		s.setNbTasksExecuted(nodeState.getNbTasksExecuted());
		s.setConnectionStatus(nodeState.getConnectionStatus());
		s.setExecutionStatus(nodeState.getExecutionStatus());
		s.setTaskEvent(nodeState.getTaskEvent());
		return s;
	}

	/**
	 * Receive a notification that an event occurred within a task.
	 * @param event the event that occurred.
	 * @see org.jppf.server.protocol.JPPFTaskListener#eventOccurred(org.jppf.server.protocol.JPPFTaskEvent)
	 */
	public synchronized void eventOccurred(JPPFTaskEvent event)
	{
		nodeState.setTaskEvent(event.getSource());
	}

	/**
	 * Called to notify a listener that a node event has occurred.
	 * @param event the event that triggered the notification.
	 * @see org.jppf.node.event.NodeListener#eventOccurred(org.jppf.node.event.NodeEvent)
	 */
	public synchronized void eventOccurred(NodeEvent event)
	{
		NodeEventType type = event.getType();
		switch(type)
		{
			case START_CONNECT:
			case END_CONNECT:
			case DISCONNECTED:
				nodeState.setConnectionStatus(type.toString());
				break;

			case START_EXEC:
			case END_EXEC:
			case TASK_EXECUTED:
				nodeState.setExecutionStatus(type.toString());
				break;
		}
		nodeState.setNbTasksExecuted(node.getTaskCount());
	}
}
