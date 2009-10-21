/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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
package org.jppf.ui.monitoring.data;

import org.jppf.utils.LocalizationUtils;

/**
 * 
 * @author Laurent Cohen
 */
public enum Fields
{
	/**
	 * Name for the total number of tasks executed.
	 */
	TOTAL_TASKS_EXECUTED,
	/**
	 * Name for the total execution time for all tasks.
	 */
	TOTAL_EXECUTION_TIME,
	/**
	 * Name for the execution time of the last executed task.
	 */
	LATEST_EXECUTION_TIME,
	/**
	 * Name for the minimum task execution time.
	 */
	MIN_EXECUTION_TIME,
	/**
	 * Name for the maximum task execution time.
	 */
	MAX_EXECUTION_TIME,
	/**
	 * Name for the average task execution time.
	 */
	AVG_EXECUTION_TIME,
	/**
	 * Name for the total tansport time for all tasks.
	 */
	TOTAL_TRANSPORT_TIME,
	/**
	 * Name for the execution time of the last tansported task.
	 */
	LATEST_TRANSPORT_TIME,
	/**
	 * Name for the minimum task tansport time.
	 */
	MIN_TRANSPORT_TIME,
	/**
	 * Name for the maximum task tansport time.
	 */
	MAX_TRANSPORT_TIME,
	/**
	 * Name for the average task tansport time.
	 */
	AVG_TRANSPORT_TIME,
	/**
	 * Name for the total execution time for all tasks on the nodes.
	 */
	TOTAL_NODE_EXECUTION_TIME,
	/**
	 * Name for the execution time of the last executed task on a node.
	 */
	LATEST_NODE_EXECUTION_TIME,
	/**
	 * Name for the minimum task execution time on a node.
	 */
	MIN_NODE_EXECUTION_TIME,
	/**
	 * Name for the maximum task execution time on a node.
	 */
	MAX_NODE_EXECUTION_TIME,
	/**
	 * Name for the average task execution time on a node.
	 */
	AVG_NODE_EXECUTION_TIME,
	/**
	 * Name for the time the last queued task remained in the queue.
	 */
	LATEST_QUEUE_TIME,
	/**
	 * Name for the total time spent in the queue by all tasks.
	 */
	TOTAL_QUEUE_TIME,
	/**
	 * Name for the minimum time a task remained in the queue .
	 */
	MIN_QUEUE_TIME,
	/**
	 * Name for the maximum time a task remained in the queue .
	 */
	MAX_QUEUE_TIME,
	/**
	 * Name for the maximum time a task remained in the queue .
	 */
	AVG_QUEUE_TIME,
	/**
	 * Name for the total number of tasks that have been queued.
	 */
	TOTAL_QUEUED,
	/**
	 * Name for the current queue size.
	 */
	QUEUE_SIZE,
	/**
	 * Name for the maximum size the queue reached.
	 */
	MAX_QUEUE_SIZE,
	/**
	 * Name for the current number of nodes connected to the server.
	 */
	NB_NODES,
	/**
	 * Name for the maximum number of nodes ever connected to the server.
	 */
	MAX_NODES,
	/**
	 * Name for the current number of clients connected to the server.
	 */
	NB_CLIENTS,
	/**
	 * Name for the maximum number of clients ever connected to the server.
	 */
	MAX_CLIENTS;

	/**
	 * The localized name of this enum item.
	 */
	private String localName = null;
	/**
	 * The base used for resource bundles lookups.
	 */
	private static final String BASE = "org.jppf.ui.i18n.StatFields";
	
	/**
	 * Initialize an enum item with a localized name.
	 */
	private Fields()
	{
		localName = LocalizationUtils.getLocalized(BASE, name());
	}

	/**
	 * Return a localized version of this item name.
	 * @return the localized nbame as a string.
	 * @see java.lang.Enum#toString()
	 */
	public String toString()
	{
		return localName;
	}
}
