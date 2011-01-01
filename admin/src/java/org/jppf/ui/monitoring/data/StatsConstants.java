/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import static org.jppf.ui.monitoring.data.Fields.*;

import org.jppf.utils.CollectionUtils;

/**
 * Constants for the JPPF statistics collected form the server.
 * @author Laurent Cohen
 */
public interface StatsConstants
{
	/**
	 * List of stats properties related to network connections.
	 */
	Fields[] CONNECTION_PROPS = new Fields[]
	{
		NB_NODES, MAX_NODES, NB_CLIENTS, MAX_CLIENTS
	};
	/**
	 * List of stats properties related to queue operations.
	 */
	Fields[] QUEUE_PROPS = new Fields[]
	{
		LATEST_QUEUE_TIME, TOTAL_QUEUE_TIME, MIN_QUEUE_TIME, MAX_QUEUE_TIME, AVG_QUEUE_TIME, TOTAL_QUEUED, QUEUE_SIZE,
		MAX_QUEUE_SIZE
	};
	/**
	 * List of stats properties related to tasks execution.
	 */
	Fields[] EXECUTION_PROPS = new Fields[]
	{
		TOTAL_TASKS_EXECUTED, TOTAL_EXECUTION_TIME, LATEST_EXECUTION_TIME, MIN_EXECUTION_TIME, MAX_EXECUTION_TIME,
		AVG_EXECUTION_TIME
	};

	/**
	 * List of stats properties related to tasks execution.
	 */
	Fields[] NODE_EXECUTION_PROPS = new Fields[]
	{
		TOTAL_NODE_EXECUTION_TIME, LATEST_NODE_EXECUTION_TIME, MIN_NODE_EXECUTION_TIME, MAX_NODE_EXECUTION_TIME,
		AVG_NODE_EXECUTION_TIME
	};
	/**
	 * List of stats properties related to tasks execution.
	 */
	Fields[] TRANSPORT_PROPS = new Fields[]
	{
		TOTAL_TRANSPORT_TIME, LATEST_TRANSPORT_TIME, MIN_TRANSPORT_TIME, MAX_TRANSPORT_TIME, AVG_TRANSPORT_TIME
	};
	/**
	 * List of all fields.
	 */
	Fields[] ALL_FIELDS =
		CollectionUtils.concatArrays(EXECUTION_PROPS, NODE_EXECUTION_PROPS, TRANSPORT_PROPS, QUEUE_PROPS, CONNECTION_PROPS);
}
