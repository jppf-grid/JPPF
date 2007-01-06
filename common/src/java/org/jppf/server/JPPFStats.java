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
package org.jppf.server;

import java.io.Serializable;
import org.jppf.utils.JPPFConfiguration;

/**
 * Instances of this class hold server-wide statitics and settings.
 * @author Laurent Cohen
 */
public class JPPFStats implements Serializable
{
	/**
	 * The total number of tasks executed.
	 */
	public int totalTasksExecuted = 0;
	/**
	 * Time statistics for the tasks execution, including network transport time and node execution time.
	 */
	public TimeSnapshot execution = new TimeSnapshot("execution");
	/**
	 * Time statistics for the tasks execution within the nodes only.
	 */
	public TimeSnapshot nodeExecution = new TimeSnapshot("node execution");
	/**
	 * Time statistics for the tasks network transport time between the nodes and server.
	 */
	public TimeSnapshot transport = new TimeSnapshot("transport");
	/**
	 * Time statistics for the tasks management overhead within the server.
	 */
	public TimeSnapshot server = new TimeSnapshot("server");
	/**
	 * Total footprint of all the data that was sent to the nodes.
	 */
	public long footprint = 0L;
	/**
	 * The average time to tranport one byte of data.
	 */
	public double avgTransportPerByte = 0d;
	/**
	 * Time statistics for the tasks queue time.
	 */
	public TimeSnapshot queue = new TimeSnapshot("queue");
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
	 * Determines the maximum number of tasks in each task bundle.
	 */
	public int bundleSize = JPPFConfiguration.getProperties().getInt("task.bundle.size", 5);

	/**
	 * Build a copy of this stats object.
	 * @return a new <code>JPPFStats</code> instance, populated with the current values
	 * of the fields in this stats object.
	 */
	public JPPFStats makeCopy()
	{
		JPPFStats s = new JPPFStats();
		s.bundleSize = bundleSize;
		s.totalTasksExecuted = totalTasksExecuted;
		s.execution = execution.makeCopy();
		s.nodeExecution = nodeExecution.makeCopy();
		s.transport = transport.makeCopy();
		s.queue = queue.makeCopy();
		s.totalQueued = totalQueued;
		s.queueSize = queueSize;
		s.maxQueueSize = maxQueueSize;
		s.nbNodes = nbNodes;
		s.maxNodes = maxNodes;
		s.nbClients = nbClients;
		s.maxClients = maxClients;
		s.footprint = footprint;
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
		sb.append(execution.toString());
		sb.append(nodeExecution.toString());
		sb.append(transport.toString());
		sb.append(queue.toString());
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
