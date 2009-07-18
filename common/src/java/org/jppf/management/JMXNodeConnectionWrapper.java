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

package org.jppf.management;

import java.io.Serializable;
import java.util.Map;

/**
 * Node-specific connection wrapper, implementing a user-friendly interface for the monitoring
 * and management of the node.
 * @author Laurent Cohen
 */
public class JMXNodeConnectionWrapper extends JMXConnectionWrapper implements JPPFNodeAdminMBean
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
	 * @see org.jppf.server.node.node.JPPFNodeAdminMBean#cancelTask(java.lang.String)
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
	 * @see org.jppf.server.node.node.JPPFNodeAdminMBean#restartTask(java.lang.String)
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
	 * @see org.jppf.server.node.node.JPPFNodeAdminMBean#state()
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
	 * @see org.jppf.server.node.node.JPPFNodeAdminMBean#notification()
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
	 * @see org.jppf.server.node.node.JPPFNodeAdminMBean#updateThreadPoolSize(java.lang.Integer)
	 */
	public void updateThreadPoolSize(Integer size) throws Exception
	{
		invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "updateThreadPoolSize",
			new Object[] { size }, new String[] { "java.lang.Integer" }); 
	}

	/**
	 * Get detailed information about the node's JVM properties, environment variables
	 * and runtime information such as memory usage and available processors.
	 * @return a <code>JPPFSystemInformation</code> instance.
	 * @throws Exception if an error occurs while invoking the Node MBean.
	 * @see org.jppf.server.node.node.JPPFNodeAdminMBean#systemInformation()
	 */
	public JPPFSystemInformation systemInformation() throws Exception
	{
		return (JPPFSystemInformation) invoke(JPPFAdminMBean.NODE_MBEAN_NAME,
			"systemInformation", (Object[]) null, (String[]) null); 
	}

	/**
	 * Shutdown the node.
	 * @throws Exception if an error is raised when invoking the node mbean.
	 * @see org.jppf.server.node.node.JPPFNodeAdminMBean#shutdown()
	 */
	public void shutdown() throws Exception
	{
		invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "shutdown", (Object[]) null, (String[]) null); 
	}

	/**
	 * Restart the node.
	 * @throws Exception if an error is raised when invoking the node mbean.
	 * @see org.jppf.server.node.node.JPPFNodeAdminMBean#restart()
	 */
	public void restart() throws Exception
	{
		invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "restart", (Object[]) null, (String[]) null); 
	}

	/**
	 * Reset the node's executed tasks counter to zero. 
	 * @throws Exception if an error is raised when invoking the node mbean.
	 * @see org.jppf.server.node.node.JPPFNodeAdminMBean#resetTaskCounter()
	 */
	public void resetTaskCounter() throws Exception
	{
		invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "resetTaskCounter", (Object[]) null, (String[]) null); 
	}

	/**
	 * Set the node's executed tasks counter to the specified value.
	 * @param n - the new value of the task counter.
	 * @throws Exception if an error is raised when invoking the node mbean.
	 * @see org.jppf.server.node.node.JPPFNodeAdminMBean#setTaskCounter(java.lang.Integer)
	 */
	public void setTaskCounter(Integer n) throws Exception
	{
		invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "setTaskCounter", new Object[] { n }, new String[] { "java.lang.Integer" }); 
	}

	/**
	 * Update the priority of all execution threads.
	 * @param newPriority the new priority to set.
	 * @throws Exception if an error is raised when invoking the node mbean.
	 * @see org.jppf.server.node.node.JPPFNodeAdminMBean#updateThreadsPriority(java.lang.Integer)
	 */
	public void updateThreadsPriority(Integer newPriority) throws Exception
	{
		invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "updateThreadsPriority",
			new Object[] { newPriority }, new String[] { "java.lang.Integer" }); 
	}

	/**
	 * Update the configuration properties of the node. 
	 * @param config the set of properties to update.
	 * @param reconnect specifies whether the node should reconnect ot the driver after updating the properties.
	 * @throws Exception if an error is raised when invoking the node mbean.
	 * @see org.jppf.server.node.node.JPPFNodeAdminMBean#updateConfiguration(java.util.Map, java.lang.Boolean)
	 */
	public void updateConfiguration(Map<String, String> config, Boolean reconnect) throws Exception
	{
		invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "updateConfiguration",
			new Object[] { config, reconnect }, new String[] { "java.util.Map", "java.lang.Boolean" }); 
	}

	/**
	 * Cancel the job with the specified id.
	 * @param jobId - the id of the job to cancel.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.management.JPPFNodeAdminMBean#cancelJob(java.lang.String)
	 */
	public void cancelJob(String jobId) throws Exception
	{
		invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "cancelJob", new Object[] { jobId }, new String[] { "java.util.String" }); 
	}
}
