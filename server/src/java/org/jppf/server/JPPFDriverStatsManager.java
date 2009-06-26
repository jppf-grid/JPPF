/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.server;

import java.util.*;

/**
 * This class is used to collect notifications from various components of the driver about jobs/tasks execution, queuing and performance.
 * It then notifies all listeners that registered with it.
 * @see org.jppf.server.JPPFDriver#getStatsManager()
 * @author Laurent Cohen
 */
public final class JPPFDriverStatsManager
{
	/**
	 * The list of listeners to notify.
	 */
	private List<JPPFDriverListener> listeners = new ArrayList<JPPFDriverListener>();
	/**
	 * The object that holds the stats.
	 */
	private JPPFStats stats = new JPPFStats();
	
	/**
	 * Add a listener to the list of listeners.
	 * @param listener - the listener to add.
	 */
	public void addDriverStatsListener(JPPFDriverListener listener)
	{
		if (listener == null) return;
		synchronized(listeners)
		{
			listeners.add(listener);
		}
	}

	/**
	 * Remove a listener from the list of listeners.
	 * @param listener - the listener to remove.
	 */
	public void removeDriverStatsListener(JPPFDriverListener listener)
	{
		if (listener == null) return;
		synchronized(listeners)
		{
			listeners.remove(listener);
		}
	}

	/**
	 * Called to notify that a new client is connected to he JPPF server.
	 */
	public void newClientConnection()
	{
		synchronized(listeners)
		{
			for (JPPFDriverListener listener: listeners) listener.newClientConnection();
		}
	}

	/**
	 * Called to notify that a new client has disconnected from he JPPF server.
	 */
	public void clientConnectionClosed()
	{
		synchronized(listeners)
		{
			for (JPPFDriverListener listener: listeners) listener.clientConnectionClosed();
		}
	}

	/**
	 * Called to notify that a new node is connected to he JPPF server.
	 */
	public void newNodeConnection()
	{
		synchronized(listeners)
		{
			for (JPPFDriverListener listener: listeners) listener.newNodeConnection();
		}
	}

	/**
	 * Called to notify that a new node is connected to he JPPF server.
	 */
	public void nodeConnectionClosed()
	{
		synchronized(listeners)
		{
			for (JPPFDriverListener listener: listeners) listener.nodeConnectionClosed();
		}
	}

	/**
	 * Called to notify that a task was added to the queue.
	 * @param count - the number of tasks that have been added to the queue.
	 */
	public void taskInQueue(int count)
	{
		synchronized(listeners)
		{
			for (JPPFDriverListener listener: listeners) listener.taskInQueue(count);
		}
	}

	/**
	 * Called to notify that a task was removed from the queue.
	 * @param count - the number of tasks that have been removed from the queue.
	 * @param time - the time the task remained in the queue.
	 */
	public void taskOutOfQueue(int count, long time)
	{
		synchronized(listeners)
		{
			for (JPPFDriverListener listener: listeners) listener.taskOutOfQueue(count, time);
		}
	}
	
	/**
	 * Called when a task execution has completed.
	 * @param count - the number of tasks that have been executed.
	 * @param time - the time it took to execute the task, including transport to and from the node.
	 * @param remoteTime - the time it took to execute the in the node only.
	 * @param size - the size in bytes of the bundle that was sent to the node.
	 */
	public void taskExecuted(int count, long time, long remoteTime, long size)
	{
		synchronized(listeners)
		{
			for (JPPFDriverListener listener: listeners) listener.taskExecuted(count, time, remoteTime, size);
		}
	}
}
