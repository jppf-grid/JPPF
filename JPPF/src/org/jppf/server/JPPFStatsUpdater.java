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

import java.util.concurrent.locks.ReentrantLock;

/**
 * Instances of this class are used to collect statistics on the JPPF server.
 * @author Laurent Cohen
 */
public final class JPPFStatsUpdater
{
	/**
	 * Used to synchronize access and updates tot he stats from a large number of threads.
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
	 */
	public static void taskInQueue()
	{
		lock.lock();
		try
		{
			stats.queueSize++;
			if (stats.queueSize > stats.maxQueueSize) stats.maxQueueSize++;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Called to notify that a task was removed from the queue.
	 * @param time the time the task remained in the queue.
	 */
	public static void taskOutOfQueue(long time)
	{
		lock.lock();
		try
		{
			stats.queueSize--;
			stats.totalQueued++;
			stats.totalQueueTime += time;
			stats.latestQueueTime = time;
			if (time > stats.maxQueueTime) stats.maxQueueTime = time;
			if (time < stats.minQueueTime) stats.minQueueTime = time;
		}
		finally
		{
			lock.unlock();
		}
	}
	
	/**
	 * Called when a task execution has completed.
	 * @param time the time it took to execute the task.
	 */
	public static void taskExecuted(long time)
	{
		lock.lock();
		try
		{
			stats.totalTasksExecuted++;
			stats.totalExecutionTime += time;
			stats.latestExecutionTime = time;
			if (time > stats.maxExecutionTime) stats.maxExecutionTime = time;
			if (time < stats.minExecutionTime) stats.minExecutionTime = time;
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

	/**
	 * Determine whether the statistics collection is enabled.
	 * @return true if the statistics collection is enabled, false otherwise.
	 */
	public static boolean isStatsEnabled()
	{
		return statsEnabled;
	}
}
