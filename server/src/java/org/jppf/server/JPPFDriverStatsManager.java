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
package org.jppf.server;

import org.jppf.utils.EventEmitter;

/**
 * This class is used to collect notifications from various components of the driver about jobs/tasks execution, queuing and performance.
 * It then notifies all listeners that registered with it.
 * @see org.jppf.server.JPPFDriver#getStatsManager()
 * @author Laurent Cohen
 */
public final class JPPFDriverStatsManager extends EventEmitter<JPPFDriverListener> implements JPPFDriverListener
{
	/**
	 * Called to notify that a new client is connected to he JPPF server.
	 */
	@Override
	public synchronized void newClientConnection()
	{
		for (JPPFDriverListener listener: eventListeners) listener.newClientConnection();
	}

	/**
	 * Called to notify that a new client has disconnected from he JPPF server.
	 */
	@Override
	public synchronized void clientConnectionClosed()
	{
		for (JPPFDriverListener listener: eventListeners) listener.clientConnectionClosed();
	}

	/**
	 * Called to notify that a new node is connected to he JPPF server.
	 */
	@Override
	public synchronized void newNodeConnection()
	{
		for (JPPFDriverListener listener: eventListeners) listener.newNodeConnection();
	}

	/**
	 * Called to notify that a new node is connected to he JPPF server.
	 */
	@Override
	public synchronized void nodeConnectionClosed()
	{
		for (JPPFDriverListener listener: eventListeners) listener.nodeConnectionClosed();
	}

	/**
	 * Called to notify that a task was added to the queue.
	 * @param count the number of tasks that have been added to the queue.
	 */
	@Override
	public synchronized void taskInQueue(int count)
	{
		for (JPPFDriverListener listener: eventListeners) listener.taskInQueue(count);
	}

	/**
	 * Called to notify that a task was removed from the queue.
	 * @param count the number of tasks that have been removed from the queue.
	 * @param time the time the task remained in the queue.
	 */
	@Override
	public synchronized void taskOutOfQueue(int count, long time)
	{
		for (JPPFDriverListener listener: eventListeners) listener.taskOutOfQueue(count, time);
	}
	
	/**
	 * Called when a task execution has completed.
	 * @param count the number of tasks that have been executed.
	 * @param time the time it took to execute the task, including transport to and from the node.
	 * @param remoteTime the time it took to execute the in the node only.
	 * @param size the size in bytes of the bundle that was sent to the node.
	 */
	@Override
  public synchronized void taskExecuted(int count, long time, long remoteTime, long size)
	{
		for (JPPFDriverListener listener: eventListeners) listener.taskExecuted(count, time, remoteTime, size);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset()
	{
		for (JPPFDriverListener listener: eventListeners) listener.reset();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void idleNodes(final int nbIdleNodes)
	{
		for (JPPFDriverListener listener: eventListeners) listener.idleNodes(nbIdleNodes);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void jobQueued(int nbTasks)
	{
		for (JPPFDriverListener listener: eventListeners) listener.jobQueued(nbTasks);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void jobEnded(long time)
	{
		for (JPPFDriverListener listener: eventListeners) listener.jobEnded(time);
	}
}
