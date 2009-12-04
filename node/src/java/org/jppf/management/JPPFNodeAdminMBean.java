/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
 * Exposed interface of the JPPF node management bean.
 * @author Laurent Cohen
 */
public interface JPPFNodeAdminMBean extends JPPFAdminMBean<NodeParameter, Object>
{
	/**
	 * Get the latest state information from the node.
	 * @return a <code>JPPFNodeState</code> information.
	 * @throws Exception if any error occurs.
	 */
	JPPFNodeState state() throws Exception;
	/**
	 * Get the latest task notification from the node.
	 * @return the notification as a <code>Serializable</code> object.
	 * @throws Exception if any error occurs.
	 */
	Serializable notification() throws Exception;
	/**
	 * Cancel the execution of the tasks with the specified id.
	 * @param id the id of the tasks to cancel.
	 * @throws Exception if any error occurs.
	 */
	void cancelTask(String id) throws Exception;
	/**
	 * Restart the execution of the tasks with the specified id.<br>
	 * The task(s) will be restarted even if their execution has already completed.
	 * @param id the id of the task or tasks to restart.
	 * @throws Exception if any error occurs.
	 */
	void restartTask(String id) throws Exception;
	/**
	 * Set the size of the node's thread pool.
	 * @param size the size as an int.
	 * @throws Exception if any error occurs.
	 */
	void updateThreadPoolSize(Integer size) throws Exception;
	/**
	 * Update the priority of all execution threads.
	 * @param newPriority the new priority to set.
	 * @throws Exception if an error is raised when invoking the node mbean.
	 */
	void updateThreadsPriority(Integer newPriority) throws Exception;
	/**
	 * Get detailed information about the node's JVM properties, environment variables
	 * and runtime information such as memory usage and available processors.
	 * @return a <code>JPPFSystemInformation</code> instance.
	 * @throws Exception if any error occurs.
	 */
	JPPFSystemInformation systemInformation() throws Exception;
	/**
	 * Restart the node.
	 * @throws Exception if any error occurs.
	 */
	void restart() throws Exception;
	/**
	 * Shutdown the node.
	 * @throws Exception if any error occurs.
	 */
	void shutdown() throws Exception;
	/**
	 * Reset the node's executed tasks counter to zero. 
	 * @throws Exception if any error occurs.
	 */
	void resetTaskCounter() throws Exception;
	/**
	 * Update the configuration properties of the node. 
	 * @param config the set of properties to update.
	 * @param reconnect specifies whether the node should reconnect ot the driver after updating the properties.
	 * @throws Exception if any error occurs.
	 */
	void updateConfiguration(Map<String, String> config, Boolean reconnect) throws Exception;
}
