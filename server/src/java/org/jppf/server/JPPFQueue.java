/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
			queue.add(bundle);
		}
		finally
		{
			lock.unlock();
		}
		taskInQueue(bundle.getTaskCount());
		for (QueueListener listener : listeners) listener.newBundle(this);
	}

	/**
	 * Get the next object in the queue. This method waits until the queue has
	 * at least one object.
	 * 
	 * @return the most recent object that was added to the queue.
	 */
	/*
	public JPPFTaskBundle nextBundle()
	{
		JPPFTaskBundle bundle = null;
		lock.lock();
		try
		{
			bundle = queue.poll();
		}
		finally
		{
			lock.unlock();
		}
		if (bundle != null)
		{
			taskOutOfQueue(bundle.getTaskCount(),
				System.currentTimeMillis() - bundle.getQueueEntryTime());
		}
		return bundle;
	}
	*/
	
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
			}
			else result = bundle.copy(nbTasks);
			result.setExecutionStartTime(System.currentTimeMillis());
		}
		finally
		{
			lock.unlock();
		}
		taskOutOfQueue(result.getTaskCount(),
				System.currentTimeMillis() - result.getQueueEntryTime());
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
}
