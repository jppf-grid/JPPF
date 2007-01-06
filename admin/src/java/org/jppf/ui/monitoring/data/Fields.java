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

import org.jppf.utils.StringUtils;

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
		localName = StringUtils.getLocalized(BASE, name());
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
