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

import java.util.concurrent.locks.ReentrantLock;

/**
 * Instances of this class are used to collect statistics on the JPPF server.
 * @author Laurent Cohen
 */
public final class JPPFStatsUpdater
{
	/**
	 * Used to synchronize access and updates to the stats from a large number of threads.
	 */
	private static ReentrantLock lock = new ReentrantLock();
	/**
	 * The object that holds the stats.
	 */
	private static JPPFStats stats = new JPPFStats();
	/**
	 * Flag to determine whether the statistics collection is enabled.
	 */
	private static boolean statsEnabled = true;
	
	/**
	 * Called to notify that a new client is connected to he JPPF server.
	 */
	public static void newClientConnection()
	{
		lock.lock();
		try
		{
			stats.nbClients++;
			if (stats.nbClients > stats.maxClients) stats.maxClients++;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Called to notify that a new client has disconnected from he JPPF server.
	 */
	public static void clientConnectionClosed()
	{
		lock.lock();
		try
		{
			stats.nbClients--;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Called to notify that a new node is connected to he JPPF server.
	 */
	public static void newNodeConnection()
	{
		lock.lock();
		try
		{
			stats.nbNodes++;
			if (stats.nbNodes > stats.maxNodes) stats.maxNodes++;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Called to notify that a new node is connected to he JPPF server.
	 */
	public static void nodeConnectionClosed()
	{
		lock.lock();
		try
		{
			stats.nbNodes--;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Called to notify that a task was added to the queue.
	 * @param count the number of tasks that have been added to the queue.
	 */
	public static void taskInQueue(int count)
	{
		lock.lock();
		try
		{
			stats.queueSize += count;
			if (stats.queueSize > stats.maxQueueSize) stats.maxQueueSize = stats.queueSize;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Called to notify that a task was removed from the queue.
	 * @param count the number of tasks that have been removed from the queue.
	 * @param time the time the task remained in the queue.
	 */
	public static void taskOutOfQueue(int count, long time)
	{
		lock.lock();
		try
		{
			stats.queueSize -= count;
			stats.totalQueued += count;
			stats.queue.newTime(time, count, stats.totalQueued);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	/**
	 * Called when a task execution has completed.
	 * @param count the number of tasks that have been executed.
	 * @param time the time it took to execute the task, including transport to and from the node.
	 * @param remoteTime the time it took to execute the in the node only.
	 * @param size the size in bytes of the bundle that was sent to the node.
	 */
	public static void taskExecuted(int count, long time, long remoteTime, long size)
	{
		lock.lock();
		try
		{
			stats.totalTasksExecuted += count;
			stats.execution.newTime(time, count, stats.totalTasksExecuted);
			stats.nodeExecution.newTime(remoteTime, count, stats.totalTasksExecuted);
			stats.transport.newTime(time - remoteTime, count, stats.totalTasksExecuted);
			stats.footprint += size;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Get the stats maintained by this updater.
	 * @return a <code>JPPFStats</code> instance.
	 */
	public static JPPFStats getStats()
	{
		return stats;
	}
	/*
	public static JPPFStats getStats()
	{
		lock.lock();
		try
		{
			return stats.makeCopy();
		}
		finally
		{
			lock.unlock();
		}
	}
	*/

	/**
	 * Determine whether the statistics collection is enabled.
	 * @return true if the statistics collection is enabled, false otherwise.
	 */
	public static boolean isStatsEnabled()
	{
		return statsEnabled;
	}
	
	/**
	 * Get the current number of nodes connected to the server.
	 * @return the numbe rof nodes as an int.
	 */
	public static int getNbNodes()
	{
		return stats.nbNodes;
	}

	/**
	 * Get the maximum number of tasks in each task bundle.
	 * @return the bundle size as an int.
	 */
	public static int getStaticBundleSize()
	{
		lock.lock();
		try
		{
			return stats.bundleSize;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Set the maximum number of tasks in each task bundle.
	 * @param bundleSize the bundle size as an int.
	 */
	public static void setStaticBundleSize(int bundleSize)
	{
		lock.lock();
		try
		{
			stats.bundleSize = bundleSize;
		}
		finally
		{
			lock.unlock();
		}
	}
}
