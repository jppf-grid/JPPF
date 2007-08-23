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
	 * The latest event that occurred within a task.
	 */
	private JPPFNodeState nodeState = new JPPFNodeState();
	/**
	 * The node whose state is monitored.
	 */
	private JPPFNode node = null;

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
		JPPFNodeState state = new JPPFNodeState();
		state.setNbTasksExecuted(nodeState.getNbTasksExecuted());
		state.setNodeEventType(nodeState.getNodeEventType());
		state.setTaskEvent(nodeState.getTaskEvent());
		return new JPPFManagementResponse(state, null);
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
		nodeState.setNodeEventType(event.getType());
		nodeState.setNbTasksExecuted(node.getTaskCount());
	}
}
