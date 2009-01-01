/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import java.io.Serializable;

import org.apache.commons.logging.*;
import org.jppf.management.*;
import org.jppf.node.event.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.JPPFUuid;

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
	 * Unique id for this mbean.
	 */
	private String uuid = new JPPFUuid(JPPFUuid.ALPHA_NUM, 24).toString();

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
		s.setTaskEvent(nodeState.getTaskNotification());
		s.setTaskIdSet(nodeState.getAllTaskIds());
		s.setThreadPoolSize(node.getExecutionManager().getThreadPoolSize());
		s.setThreadPriority(node.getExecutionManager().getThreadsPriority());
		s.setCpuTime(node.getExecutionManager().getCpuTime());
		return s;
	}

	/**
	 * Get the latest state information from the node.
	 * @return a <code>JPPFNodeState</code> information.
	 * @see org.jppf.management.JPPFNodeAdminMBean#state()
	 */
	public Serializable notification()
	{
		return nodeState.getTaskNotification();
	}

	/**
	 * Receive a notification that an event occurred within a task.
	 * @param event the event that occurred.
	 * @see org.jppf.server.protocol.JPPFTaskListener#eventOccurred(org.jppf.server.protocol.JPPFTaskEvent)
	 */
	public synchronized void eventOccurred(JPPFTaskEvent event)
	{
		nodeState.setTaskEvent((Serializable) event.getSource());
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
		//nodeState.setNbTasksExecuted(node.getTaskCount());
	}

	/**
	 * Cancel the execution of the tasks with the specified id.
	 * @param id the id of the tasks to cancel.
	 * @see org.jppf.management.JPPFNodeAdminMBean#cancelTask(java.lang.String)
	 */
	public void cancelTask(String id)
	{
		node.getExecutionManager().cancelTask(id);
	}

	/**
	 * Restart the execution of the tasks with the specified id.<br>
	 * The task(s) will be restarted even if their execution has already completed.
	 * @param id the id of the task or tasks to restart.
	 * @see org.jppf.management.JPPFNodeAdminMBean#restartTask(java.lang.String)
	 */
	public void restartTask(String id)
	{
		node.getExecutionManager().restartTask(id);
	}

	/**
	 * Notification that a task witht the specified id has started.
	 * @param id the id of the task.
	 */
	public void taskStarted(String id)
	{
		if (debugEnabled) log.debug("task id#" + id + " started");
		nodeState.taskStarted(id);
	}

	/**
	 * Notification that a task witht the specified id has ended.
	 * @param id the id of the task.
	 */
	public void taskEnded(String id)
	{
		if (debugEnabled) log.debug("task id#" + id + " ended");
		nodeState.taskEnded(id);
	}

	/**
	 * Set the size of the node's thread pool.
	 * @param size the size as an int.
	 * @see org.jppf.management.JPPFNodeAdminMBean#updateThreadPoolSize(java.lang.Integer)
	 */
	public void updateThreadPoolSize(Integer size)
	{
		node.getExecutionManager().setThreadPoolSize(size);
	}

	/**
	 * Get detailed information about the node's JVM properties, environment variables
	 * and runtime information such as memory usage and available processors.
	 * @return a <code>JPPFSystemInformation</code> instance.
	 * @see org.jppf.management.JPPFNodeAdminMBean#systemInformation()
	 */
	public JPPFSystemInformation systemInformation()
	{
		JPPFSystemInformation info = new JPPFSystemInformation();
		info.populate();
		info.getRuntime().setProperty("cpuTime", ""+node.getExecutionManager().getCpuTime());
		return info;
	}

	/**
	 * Restart the node.
	 * @see org.jppf.management.JPPFNodeAdminMBean#restart()
	 */
	public void restart()
	{
		node.shutdown(true);
	}

	/**
	 * Shutdown the node.
	 * @see org.jppf.management.JPPFNodeAdminMBean#shutdown()
	 */
	public void shutdown()
	{
		node.shutdown(false);
	}

	/**
	 * Reset the node's executed tasks counter to zero. 
	 * @see org.jppf.management.JPPFNodeAdminMBean#resetTaskCounter()
	 */
	public void resetTaskCounter()
	{
		setTaskCounter(0);
	}

	/**
	 * Set the node's executed tasks counter to the specified value.
	 * @param n the new value of the task counter.
	 */
	public void setTaskCounter(int n)
	{
		node.setTaskCount(n);
		nodeState.setNbTasksExecuted(n);
	}

	/**
	 * Update the priority of all execution threads.
	 * @param newPriority the new priority to set.
	 * @see org.jppf.management.JPPFNodeAdminMBean#updateThreadsPriority(int)
	 */
	public void updateThreadsPriority(Integer newPriority)
	{
		node.getExecutionManager().updateThreadsPriority(newPriority);
	}
}
