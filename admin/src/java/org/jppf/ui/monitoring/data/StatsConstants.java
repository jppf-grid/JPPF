/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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