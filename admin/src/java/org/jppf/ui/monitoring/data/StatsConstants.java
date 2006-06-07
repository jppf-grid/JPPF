/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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

import org.jppf.utils.CollectionUtils;

/**
 * Constants for the JPPF statistics collected form the server.
 * @author Laurent Cohen
 */
public interface StatsConstants
{
	/**
	 * Property name for the total number of tasks executed.
	 */
	String TOTAL_TASKS_EXECUTED = "Total Tasks Executed";
	/**
	 * Property name for the total execution time for all tasks.
	 */
	String TOTAL_EXECUTION_TIME = "Total Execution Time";
	/**
	 * Property name for the execution time of the last executed task.
	 */
	String LATEST_EXECUTION_TIME = "Latest Execution Time";
	/**
	 * Property name for the minimum task execution time.
	 */
	String MIN_EXECUTION_TIME = "Minimum Execution Time";
	/**
	 * Property name for the maximum task execution time.
	 */
	String MAX_EXECUTION_TIME = "Maximum Execution Time";
	/**
	 * Property name for the average task execution time.
	 */
	String AVG_EXECUTION_TIME = "Average Execution Time";
	/**
	 * Property name for the total tansport time for all tasks.
	 */
	String TOTAL_TRANSPORT_TIME = "Total Tansport Time";
	/**
	 * Property name for the execution time of the last tansported task.
	 */
	String LATEST_TRANSPORT_TIME = "Latest Tansport Time";
	/**
	 * Property name for the minimum task tansport time.
	 */
	String MIN_TRANSPORT_TIME = "Minimum Tansport Time";
	/**
	 * Property name for the maximum task tansport time.
	 */
	String MAX_TRANSPORT_TIME = "Maximum Tansport Time";
	/**
	 * Property name for the average task tansport time.
	 */
	String AVG_TRANSPORT_TIME = "Average Tansport Time";
	/**
	 * Property name for the total execution time for all tasks on the nodes.
	 */
	String TOTAL_NODE_EXECUTION_TIME = "Total Node Execution Time";
	/**
	 * Property name for the execution time of the last executed task on a node.
	 */
	String LATEST_NODE_EXECUTION_TIME = "Latest Node Execution Time";
	/**
	 * Property name for the minimum task execution time on a node.
	 */
	String MIN_NODE_EXECUTION_TIME = "Minimum Node Execution Time";
	/**
	 * Property name for the maximum task execution time on a node.
	 */
	String MAX_NODE_EXECUTION_TIME = "Maximum Node Execution Time";
	/**
	 * Property name for the average task execution time on a node.
	 */
	String AVG_NODE_EXECUTION_TIME = "Average Node Execution Time";
	/**
	 * Property name for the time the last queued task remained in the queue.
	 */
	String LATEST_QUEUE_TIME = "Latest Queue Time";
	/**
	 * Property name for the total time spent in the queue by all tasks.
	 */
	String TOTAL_QUEUE_TIME = "Total Queue Time";
	/**
	 * Property name for the minimum time a task remained in the queue .
	 */
	String MIN_QUEUE_TIME = "Minimum Queue Time";
	/**
	 * Property name for the maximum time a task remained in the queue .
	 */
	String MAX_QUEUE_TIME = "Maximum Queue Time";
	/**
	 * Property name for the maximum time a task remained in the queue .
	 */
	String AVG_QUEUE_TIME = "Average Queue Time";
	/**
	 * Property name for the total number of tasks that have been queued.
	 */
	String TOTAL_QUEUED = "Total Queued";
	/**
	 * Property name for the current queue size.
	 */
	String QUEUE_SIZE = "Queue Size";
	/**
	 * Property name for the maximum size the queue reached.
	 */
	String MAX_QUEUE_SIZE = "Maximum Queue Size";
	/**
	 * Property name for the current number of nodes connected to the server.
	 */
	String NB_NODES = "Number of Nodes";
	/**
	 * Property name for the maximum number of nodes ever connected to the server.
	 */
	String MAX_NODES = "Maximum Number of Nodes";
	/**
	 * Property name for the current number of clients connected to the server.
	 */
	String NB_CLIENTS = "Number of Clients";
	/**
	 * Property name for the maximum number of clients ever connected to the server.
	 */
	String MAX_CLIENTS = "Maximum Number of Clients";
	/**
	 * Property name for the maximum number of clients ever connected to the server.
	 */
	String AVG_KILOBYTE_TRANPORT = "Average megabyte transport";
	/**
	 * List of stats properties related to network connections.
	 */
	String[] CONNECTION_PROPS = new String[]
	{
		NB_NODES, MAX_NODES, NB_CLIENTS, MAX_CLIENTS
	};
	/**
	 * List of stats properties related to queue operations.
	 */
	String[] QUEUE_PROPS = new String[]
	{
		LATEST_QUEUE_TIME, TOTAL_QUEUE_TIME, MIN_QUEUE_TIME, MAX_QUEUE_TIME, AVG_QUEUE_TIME, TOTAL_QUEUED, QUEUE_SIZE,
		MAX_QUEUE_SIZE
	};
	/**
	 * List of stats properties related to tasks execution.
	 */
	String[] EXECUTION_PROPS = new String[]
	{
		TOTAL_TASKS_EXECUTED, TOTAL_EXECUTION_TIME, LATEST_EXECUTION_TIME, MIN_EXECUTION_TIME, MAX_EXECUTION_TIME,
		AVG_EXECUTION_TIME
	};

	/**
	 * List of stats properties related to tasks execution.
	 */
	String[] NODE_EXECUTION_PROPS = new String[]
	{
		TOTAL_NODE_EXECUTION_TIME, LATEST_NODE_EXECUTION_TIME, MIN_NODE_EXECUTION_TIME, MAX_NODE_EXECUTION_TIME,
		AVG_NODE_EXECUTION_TIME
	};
	/**
	 * List of stats properties related to tasks execution.
	 */
	String[] TRANSPORT_PROPS = new String[]
	{
		TOTAL_TRANSPORT_TIME, LATEST_TRANSPORT_TIME, MIN_TRANSPORT_TIME, MAX_TRANSPORT_TIME, AVG_TRANSPORT_TIME
	};
	/**
	 * List of all fields.
	 */
	String[] ALL_FIELDS =
		CollectionUtils.concatArrays(EXECUTION_PROPS, NODE_EXECUTION_PROPS, TRANSPORT_PROPS, QUEUE_PROPS, CONNECTION_PROPS);
}