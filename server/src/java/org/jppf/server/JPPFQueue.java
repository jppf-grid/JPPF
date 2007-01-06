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

import static org.jppf.server.JPPFStatsUpdater.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of a generic non-blocking queue, to allow asynchronous access from a large number of threads.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class JPPFQueue
{
	/**
	 * The current list of listeners to this queue.
	 */
	List<QueueListener> listeners = new LinkedList<QueueListener>();
	/**
	 * Used for synchronized access to the queue.
	 */
	private ReentrantLock lock = new ReentrantLock();
	/**
	 * The maximum bundle size for the bundles present in the queue.
	 */
	private int maxBundleSize = 0;
	/**
	 * An ordered map of bundle sizes, mapping to a list of bundles of this size.
	 */
	private TreeMap<Integer, List<JPPFTaskBundle>> sizeMap = new TreeMap<Integer, List<JPPFTaskBundle>>(); 
	
	/**
	 * Executable tasks queue, available for execution nodes to pick from. This
	 * queue behaves as a FIFO queue and is thread-safe for atomic
	 * <code>add()</code> and <code>poll()</code> operations.
	 */
	private Queue<JPPFTaskBundle> queue = new PriorityQueue<JPPFTaskBundle>();
	
	/**
	 * Add an object to the queue, and notify all listeners about it.
	 * @param bundle the object to add to the queue.
	 */
	public void addBundle(JPPFTaskBundle bundle)
	{
		bundle.setQueueEntryTime(System.currentTimeMillis());
		lock.lock();
		try
		{
			if (queue.isEmpty()) setMaxBundleSize(0);
			queue.add(bundle);
			int size = bundle.getTaskCount();
			if (size > maxBundleSize) setMaxBundleSize(size);
			List<JPPFTaskBundle> list = sizeMap.get(size);
			if (list == null)
			{
				list = new ArrayList<JPPFTaskBundle>();
				sizeMap.put(size, list);
			}
			list.add(bundle);
		}
		finally
		{
			lock.unlock();
		}
		taskInQueue(bundle.getTaskCount());
		for (QueueListener listener : listeners) listener.newBundle(this);
	}

	/**
	 * Get the next object in the queue.
	 * @param nbTasks the maximum number of tasks to get out of the bundle.
	 * @return the most recent object that was added to the queue.
	 */
	public JPPFTaskBundle nextBundle(int nbTasks)
	{
		JPPFTaskBundle result = null;
		lock.lock();
		try
		{
			JPPFTaskBundle bundle = queue.peek();
			if (bundle == null) return bundle;
			if (nbTasks >= bundle.getTaskCount())
			{
				result = bundle;
				queue.remove(bundle);
				int size = bundle.getInitialTaskCount();
				List<JPPFTaskBundle> list = sizeMap.get(size);
				if (list != null)
				{
					list.remove(bundle);
					if (list.isEmpty()) sizeMap.remove(size);
					//if (sizeMap.isEmpty()) setMaxBundleSize(0);
				}
				if (!sizeMap.isEmpty()) setMaxBundleSize(sizeMap.lastKey());
			}
			else result = bundle.copy(nbTasks);
			result.setExecutionStartTime(System.currentTimeMillis());
		}
		finally
		{
			lock.unlock();
		}
		taskOutOfQueue(result.getTaskCount(), System.currentTimeMillis() - result.getQueueEntryTime());
		return result;
	}
	
	/**
	 * Add a listener to the current list of listeners to this queue.
	 * @param listener the listener to add.
	 */
	public void addListener(QueueListener listener)
	{
		listeners.add(listener);
	}

	/**
	 * Remove a listener from the current list of listeners to this queue.
	 * @param listener the listener to remove.
	 */
	public void removeListener(QueueListener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Queue listener interface.
	 */
	public interface QueueListener
	{
		/**
		 * Notify a listener that a queue event occurred.
		 * @param queue the queue from which the event originated.
		 */
		void newBundle(JPPFQueue queue);
	}

	/**
	 * Determine whether the queue is empty or not.
	 * @return true if the queue is empty, false otherwise.
	 */
	public boolean isEmpty()
	{
		lock.lock();
		try
		{
			return queue.isEmpty();
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Get the maximum bundle size for the bundles present in the queue.
	 * @return the bundle size as an int.
	 */
	public synchronized int getMaxBundleSize()
	{
		return maxBundleSize;
	}

	/**
	 * Set the maximum bundle size for the bundles present in the queue.
	 * @param maxBundleSize the bundle size as an int.
	 */
	public synchronized void setMaxBundleSize(int maxBundleSize)
	{
		this.maxBundleSize = maxBundleSize;
	}
}
