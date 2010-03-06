/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

import org.jppf.utils.*;

/**
 * Instances of this class hold server-wide statitics and settings.
 * @author Laurent Cohen
 */
public class JPPFStats implements Serializable
{
	/**
	 * The total number of tasks executed.
	 */
	private int totalTasksExecuted = 0;
	/**
	 * Time statistics for the tasks execution, including network transport time and node execution time.
	 */
	private TimeSnapshot execution = new TimeSnapshot("execution");
	/**
	 * Time statistics for the tasks execution within the nodes only.
	 */
	private TimeSnapshot nodeExecution = new TimeSnapshot("node execution");
	/**
	 * Time statistics for the tasks network transport time between the nodes and server.
	 */
	private TimeSnapshot transport = new TimeSnapshot("transport");
	/**
	 * Time statistics for the tasks management overhead within the server.
	 */
	private TimeSnapshot server = new TimeSnapshot("server");
	/**
	 * Total footprint of all the data that was sent to the nodes.
	 */
	private long footprint = 0L;
	/**
	 * Time statistics for the queued tasks.
	 */
	private TimeSnapshot queue = new TimeSnapshot("queue");
	/**
	 * Total number of tasks that have been queued.
	 */
	private int totalQueued = 0;
	/**
	 * The current size of the queue.
	 */
	private int queueSize = 0;
	/**
	 * The maximum size of the queue.
	 */
	private int maxQueueSize = 0;
	/**
	 * The current number of nodes connected to the server.
	 */
	private int nbNodes = 0;
	/**
	 * The maximum number of nodes connected to the server.
	 */
	private int maxNodes = 0;
	/**
	 * The current number of clients connected to the server.
	 */
	private int nbClients = 0;
	/**
	 * The maximum number of clients connected to the server.
	 */
	private int maxClients = 0;

	/**
	 * Build a copy of this stats object.
	 * @return a new <code>JPPFStats</code> instance, populated with the current values
	 * of the fields in this stats object.
	 */
	public JPPFStats makeCopy()
	{
		JPPFStats s = new JPPFStats();
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

	/**
	 * Set the total number of tasks executed.
	 * @param totalTasksExecuted - the number of tasks as an int value.
	 */
	public void setTotalTasksExecuted(int totalTasksExecuted)
	{
		this.totalTasksExecuted = totalTasksExecuted;
	}

	/**
	 * Get the total number of tasks executed.
	 * @return the number of tasks as an int value.
	 */
	public int getTotalTasksExecuted()
	{
		return totalTasksExecuted;
	}

	/**
	 * Set the time statistics for the tasks execution, including network transport and node execution time.
	 * @param execution - a <code>TimeSnapshot</code> instance.
	 */
	public void setExecution(TimeSnapshot execution)
	{
		this.execution = execution;
	}

	/**
	 * Get the time statistics for the tasks execution, including network transport and node execution time.
	 * @return a <code>TimeSnapshot</code> instance.
	 */
	public TimeSnapshot getExecution()
	{
		return execution;
	}

	/**
	 * Set the time statistics for execution within the nodes.
	 * @param nodeExecution - a <code>TimeSnapshot</code> instance.
	 */
	public void setNodeExecution(TimeSnapshot nodeExecution)
	{
		this.nodeExecution = nodeExecution;
	}

	/**
	 * Get the time statistics for execution within the nodes.
	 * @return a <code>TimeSnapshot</code> instance.
	 */
	public TimeSnapshot getNodeExecution()
	{
		return nodeExecution;
	}

	/**
	 * Set the time statistics for the network transport between nodes and server.
	 * @param transport - a <code>TimeSnapshot</code> instance.
	 */
	public void setTransport(TimeSnapshot transport)
	{
		this.transport = transport;
	}

	/**
	 * Get the time statistics for the network transport between nodes and server.
	 * @return a <code>TimeSnapshot</code> instance.
	 */
	public TimeSnapshot getTransport()
	{
		return transport;
	}

	/**
	 * Set the time statistics for the server overhead.
	 * @param server - a <code>TimeSnapshot</code> instance.
	 */
	public void setServer(TimeSnapshot server)
	{
		this.server = server;
	}

	/**
	 * Get the time statistics for the server overhead.
	 * @return a <code>TimeSnapshot</code> instance.
	 */
	public TimeSnapshot getServer()
	{
		return server;
	}

	/**
	 * Set the total footprint of all the data that was sent to the nodes.
	 * @param footprint - the footprint as a long value.
	 */
	public void setFootprint(long footprint)
	{
		this.footprint = footprint;
	}

	/**
	 * Get the total footprint of all the data that was sent to the nodes.
	 * @return the footprint as a long value.
	 */
	public long getFootprint()
	{
		return footprint;
	}

	/**
	 * Set the time statistics for the queued tasks.
	 * @param queue - a <code>TimeSnapshot</code> instance.
	 */
	public void setQueue(TimeSnapshot queue)
	{
		this.queue = queue;
	}

	/**
	 * Get the time statistics for the queued tasks.
	 * @return a <code>TimeSnapshot</code> instance.
	 */
	public TimeSnapshot getQueue()
	{
		return queue;
	}

	/**
	 * Set the total number of tasks that have been queued.
	 * @param totalQueued - the number of tasks as an int value.
	 */
	public void setTotalQueued(int totalQueued)
	{
		this.totalQueued = totalQueued;
	}

	/**
	 * Get the total number of tasks that have been queued.
	 * @return the number of tasks as an int value.
	 */
	public int getTotalQueued()
	{
		return totalQueued;
	}

	/**
	 * Set the current queue size.
	 * @param queueSize - the current queue size as an int value.
	 */
	public void setQueueSize(int queueSize)
	{
		this.queueSize = queueSize;
	}

	/**
	 * Get the current queue size.
	 * @return the current queue size as an int value.
	 */
	public int getQueueSize()
	{
		return queueSize;
	}

	/**
	 * Set the maximum queue size.
	 * @param maxQueueSize - the maximum queue size as an int value.
	 */
	public void setMaxQueueSize(int maxQueueSize)
	{
		this.maxQueueSize = maxQueueSize;
	}

	/**
	 * Get the maximum queue size.
	 * @return the maximum queue size as an int value.
	 */
	public int getMaxQueueSize()
	{
		return maxQueueSize;
	}

	/**
	 * Set the current number of nodes connected to the server.
	 * @param nbNodes - the current number of nodes as an int value.
	 */
	public void setNbNodes(int nbNodes)
	{
		this.nbNodes = nbNodes;
	}

	/**
	 * Get the current number of nodes connected to the server.
	 * @return the current number of nodes as an int value. 
	 */
	public int getNbNodes()
	{
		return nbNodes;
	}

	/**
	 * Set the maximum number of nodes connected to the server.
	 * @param maxNodes - the maximum number of nodes as an int value.
	 */
	public void setMaxNodes(int maxNodes)
	{
		this.maxNodes = maxNodes;
	}

	/**
	 * Get the maximum number of nodes connected to the server.
	 * @return the maximum number of nodes as an int value. 
	 */
	public int getMaxNodes()
	{
		return maxNodes;
	}

	/**
	 * Set the current number of clients connected to the server.
	 * @param nbClients - the current number of clients as an int value.
	 */
	public void setNbClients(int nbClients)
	{
		this.nbClients = nbClients;
	}

	/**
	 * Get the current number of clients connected to the server.
	 * @return the current number of clients as an int value. 
	 */
	public int getNbClients()
	{
		return nbClients;
	}

	/**
	 * Set the maximum number of clients connected to the server.
	 * @param maxClients - the maximum number of clients as an int value.
	 */
	public void setMaxClients(int maxClients)
	{
		this.maxClients = maxClients;
	}

	/**
	 * Get the maximum number of clients connected to the server.
	 * @return the maximum number of clients as an int value. 
	 */
	public int getMaxClients()
	{
		return maxClients;
	}
}
