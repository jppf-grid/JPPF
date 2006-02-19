/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.server;

import java.io.Serializable;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFStats implements Serializable
{
	/**
	 * The total number of tasks executed.
	 */
	public int totalTasksExecuted = 0;
	/**
	 * The total tasks execution time.
	 */
	public long totalExecutionTime = 0L;
	/**
	 * The execution time of the most recently executed task.
	 */
	public long latestExecutionTime = 0L;
	/**
	 * The minimum task execution time.
	 */
	public long minExecutionTime = Long.MAX_VALUE;
	/**
	 * The maximum task execution time.
	 */
	public long maxExecutionTime = 0L;
	/**
	 * Time the latest task (at the time of the request) remained in the queue.
	 */
	public long latestQueueTime = 0L;
	/**
	 * Total time spent in the queue for all tasks.
	 */
	public long totalQueueTime = 0L;
	/**
	 * Minimum time a task remained in the queue.
	 */
	public long minQueueTime = Long.MAX_VALUE;
	/**
	 * Maximum time a task remained in the queue.
	 */
	public long maxQueueTime = 0L;
	/**
	 * Total number of tasks that have been queue.
	 */
	public int totalQueued = 0;
	/**
	 * Current size of the queue.
	 */
	public int queueSize = 0;
	/**
	 * Maximum size of the queue.
	 */
	public int maxQueueSize = 0;
	/**
	 * The current number of nodes connected to the server.
	 */
	public int nbNodes = 0;
	/**
	 * The maximum number of nodes connected to the server.
	 */
	public int maxNodes = 0;
	/**
	 * Property name for the current number of clients connected to the server.
	 */
	public int nbClients = 0;
	/**
	 * Property name for the maximum number of clients connected to the server.
	 */
	public int maxClients = 0;

	/**
	 * Build a copy of this stats object.
	 * @return a new <code>JPPFStats</code> instance, populated with the current values
	 * of the fields in this stats object.
	 */
	public JPPFStats makeCopy()
	{
		JPPFStats s = new JPPFStats();
		s.totalTasksExecuted = totalTasksExecuted;
		s.totalExecutionTime = totalExecutionTime;
		s.latestExecutionTime = latestExecutionTime;
		s.minExecutionTime = minExecutionTime;
		s.maxExecutionTime = maxExecutionTime;
		s.latestQueueTime = latestQueueTime;
		s.totalQueueTime = totalQueueTime;
		s.minQueueTime = minQueueTime;
		s.maxQueueTime = maxQueueTime;
		s.totalQueued = totalQueued;
		s.queueSize = queueSize;
		s.maxQueueSize = maxQueueSize;
		s.nbNodes = nbNodes;
		s.maxNodes = maxNodes;
		s.nbClients = nbClients;
		s.maxClients = maxClients;
		return s;
	}
	
	/**
	 * Get a string representation of this stats object.
	 * @return a string display the various stats values.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("totalTasksExecuted : ").append(totalTasksExecuted).append("\n");
		sb.append("totalExecutionTime : ").append(totalExecutionTime).append("\n");
		sb.append("latestExecutionTime : ").append(latestExecutionTime).append("\n");
		sb.append("minExecutionTime : ").append(minExecutionTime).append("\n");
		sb.append("maxExecutionTime : ").append(maxExecutionTime).append("\n");
		sb.append("latestQueueTime : ").append(latestQueueTime).append("\n");
		sb.append("totalQueueTime : ").append(totalQueueTime).append("\n");
		sb.append("minQueueTime : ").append(minQueueTime).append("\n");
		sb.append("maxQueueTime : ").append(maxQueueTime).append("\n");
		sb.append("totalQueued : ").append(totalQueued).append("\n");
		sb.append("queueSize : ").append(queueSize).append("\n");
		sb.append("maxQueueSize : ").append(maxQueueSize).append("\n");
		sb.append("nbNodes : ").append(nbNodes).append("\n");
		sb.append("maxNodes : ").append(maxNodes).append("\n");
		sb.append("nbClients : ").append(nbClients).append("\n");
		sb.append("maxClients : ").append(maxClients).append("\n");
		return sb.toString();
	}
}
