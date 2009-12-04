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

/**
 * Type safe enumeration of the parameters used for node administration requests.
 * @author Laurent Cohen
 */
public enum NodeParameter
{
	/**
	 * Parameter for the type of request sent to the JMX server.
	 */
	COMMAND_PARAM,
	/**
	 * Parameter for reading the state of the node.
	 */
	REFRESH_STATE,
	/**
	 * Parameter for reading the latest task notification.
	 */
	REFRESH_NOTIFICATION,
	/**
	 * Parameter for the node status (ie connected, executing, etc).
	 */
	NODE_STATUS_PARAM,
	/**
	 * Parameter for the number of tasks executed by the node.
	 */
	NB_TASKS_EXECUTED_PARAM,
	/**
	 * Parameter for the latest task event.
	 */
	TASK_NOTIFICATION_PARAM
}
