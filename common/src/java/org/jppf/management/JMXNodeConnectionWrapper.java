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

/**
 * Node-specific connection wrapper, implementing a user-friendly interface for the monitoring
 * and management of the node.
 * @author Laurent Cohen
 */
public class JMXNodeConnectionWrapper extends JMXConnectionWrapper
{
	/**
	 * Initialize the connection to the remote MBean server.
	 * @param host the host the server is running on.
	 * @param port the RMI port used by the server.
	 */
	public JMXNodeConnectionWrapper(String host, int port)
	{
		super(host, port, JPPFAdminMBean.NODE_SUFFIX);
	}

	/**
	 * Cancel the execution of the tasks with the specified id.
	 * @param id the id of the tasks to cancel.
	 * @throws Exception if an error occurs while invoking the Node MBean.
	 */
	public void cancelTask(String id) throws Exception
	{
		invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "cancelTask",
			new Object[] { id }, new String[] { "java.lang.String" }); 
	}

	/**
	 * Restart the execution of the tasks with the specified id.<br>
	 * The task(s) will be restarted even if their execution has already completed.
	 * @param id the id of the task or tasks to restart.
	 * @throws Exception if an error occurs while invoking the Node MBean.
	 */
	public void restartTask(String id) throws Exception
	{
		invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "restartTask",
			new Object[] { id }, new String[] { "java.lang.String" }); 
	}

	/**
	 * Get the latest state information from the node.
	 * @return a <code>JPPFNodeState</code> information.
	 * @throws Exception if an error occurs while invoking the Node MBean.
	 */
	public JPPFNodeState state() throws Exception
	{
		return (JPPFNodeState) invoke(JPPFAdminMBean.NODE_MBEAN_NAME,
			"state", (Object[]) null, (String[]) null); 
	}

	/**
	 * Get the latest task notification from the node.
	 * @return a the notification as a <code>Serializable</code> object.
	 * @throws Exception if an error occurs while invoking the Node MBean.
	 */
	public Serializable notification() throws Exception
	{
		return (Serializable) invoke(JPPFAdminMBean.NODE_MBEAN_NAME,
			"notification", (Object[]) null, (String[]) null); 
	}

	/**
	 * Set the size of the node's thread pool.
	 * @param size the size as an int.
	 * @throws Exception if an error occurs while invoking the Node MBean.
	 */
	public void updateThreadPoolSize(int size) throws Exception
	{
		invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "updateThreadPoolSize",
			new Object[] { size }, new String[] { "java.lang.Integer" }); 
	}

	/**
	 * Get detailed information about the node's JVM properties, environment variables
	 * and runtime information such as memory usage and available processors.
	 * @return a <code>JPPFSystemInformation</code> instance.
	 * @throws Exception if an error occurs while invoking the Node MBean.
	 * @see org.jppf.management.JPPFNodeAdminMBean#systemInformation()
	 */
	public JPPFSystemInformation systemInformation() throws Exception
	{
		return (JPPFSystemInformation) invoke(JPPFAdminMBean.NODE_MBEAN_NAME,
			"systemInformation", (Object[]) null, (String[]) null); 
	}

	/**
	 * Shutdown the node.
	 * @throws Exception if an error is raised when invoking the node mbean.
	 */
	public void shutdown() throws Exception
	{
		invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "shutdown", (Object[]) null, (String[]) null); 
	}

	/**
	 * Restart the node.
	 * @throws Exception if an error is raised when invoking the node mbean.
	 */
	public void restart() throws Exception
	{
		invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "restart", (Object[]) null, (String[]) null); 
	}

	/**
	 * Reset the node's executed tasks counter to zero. 
	 * @throws Exception if an error is raised when invoking the node mbean.
	 */
	public void resetTaskCounter() throws Exception
	{
		invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "resetTaskCounter", (Object[]) null, (String[]) null); 
	}
}
