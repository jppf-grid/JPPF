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
 * Exposed interface of the JPPF node management bean.
 * @author Laurent Cohen
 */
public interface JPPFNodeAdminMBean extends JPPFAdminMBean<NodeParameter, Object>
{
	/**
	 * Get the latest state information from the node.
	 * @return a <code>JPPFNodeState</code> information.
	 */
	JPPFNodeState state();
	/**
	 * Get the latest task notification from the node.
	 * @return the notification as a <code>Serializable</code> object.
	 */
	Serializable notification();
	/**
	 * Cancel the execution of the tasks with the specified id.
	 * @param id the id of the tasks to cancel.
	 */
	void cancelTask(String id);
	/**
	 * Restart the execution of the tasks with the specified id.<br>
	 * The task(s) will be restarted even if their execution has already completed.
	 * @param id the id of the task or tasks to restart.
	 */
	void restartTask(String id);
	/**
	 * Set the size of the node's thread pool.
	 * @param size the size as an int.
	 */
	void updateThreadPoolSize(Integer size);
	/**
	 * Get detailed information about the node's JVM properties, environment variables
	 * and runtime information such as memory usage and available processors.
	 * @return a <code>JPPFSystemInformation</code> instance.
	 */
	JPPFSystemInformation systemInformation();
	/**
	 * Restart the node.
	 */
	void restart();
	/**
	 * Shutdown the node.
	 */
	void shutdown();
	/**
	 * Reset the node's executed tasks counter to zero. 
	 */
	void resetTaskCounter();
}
