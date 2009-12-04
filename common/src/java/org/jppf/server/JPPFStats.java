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
	 * Total number of tasks that have been queued.
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
