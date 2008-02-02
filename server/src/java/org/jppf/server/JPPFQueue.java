/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

import static org.jppf.server.JPPFStatsUpdater.*;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.*;
import org.jppf.server.protocol.JPPFTaskBundle;

/**
 * Implementation of a generic non-blocking queue, to allow asynchronous access from a large number of threads.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class JPPFQueue
{
	/**
	 * Log4j logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFQueue.class);
	/**
	 * Determines whether the debug level is enabled in the log4j configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The current list of listeners to this queue.
	 */
	List<QueueListener> listeners = new LinkedList<QueueListener>();
	/**
	 * Used for synchronized access to the queue.
	 */
	private ReentrantLock lock = new ReentrantLock();
	/**
	 * An ordered map of bundle sizes, mapping to a list of bundles of this size.
	 */
	private TreeMap<Integer, List<JPPFTaskBundle>> sizeMap = new TreeMap<Integer, List<JPPFTaskBundle>>();
	/**
	 * 
	 */
	private int latestMaxSize = 0;
	
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
		try
		{
			lock.lock();
			if (debugEnabled) log.debug("adding bundle with [initialTasksCount=" + bundle.getInitialTaskCount() +
				", taskCount=" + bundle.getTaskCount() + "]");
			queue.add(bundle);
			int size = getSize(bundle);
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
		if (debugEnabled) log.debug("sizeMap size = " + sizeMap.size());
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
		try
		{
			lock.lock();
			if (debugEnabled) log.debug("requesting bundle with " + nbTasks + " tasks");
			JPPFTaskBundle bundle = queue.peek();
			if (bundle == null) return null;
			if (debugEnabled) log.debug("next bundle has " + bundle.getTaskCount() + " tasks");
			int size = getSize(bundle);
			List<JPPFTaskBundle> list = sizeMap.get(size);
			if (list != null)
			{
				list.remove(bundle);
				if (list.isEmpty()) sizeMap.remove(size);
			}
			if (nbTasks >= bundle.getTaskCount())
			{
				if (debugEnabled) log.debug("removing bundle from queue");
				result = bundle;
				queue.remove(bundle);
			}
			else
			{
				if (debugEnabled) log.debug("removing " + nbTasks + " tasks from bundle");
				result = bundle.copy(nbTasks);
				int newSize = bundle.getTaskCount();
				list = sizeMap.get(newSize);
				if (list == null)
				{
					list = new ArrayList<JPPFTaskBundle>();
					//sizeMap.put(newSize, list);
					sizeMap.put(size, list);
				}
				list.add(bundle);
			}
			result.setExecutionStartTime(System.currentTimeMillis());
		}
		finally
		{
			lock.unlock();
		}
		if (debugEnabled)
		{
			log.debug("sizeMap size = " + sizeMap.size());
			int count = 0;
			for (int n: sizeMap.keySet()) count += sizeMap.get(n).size();
			log.debug("total bundles in sizeMap = " + count);
		}
		taskOutOfQueue(result.getTaskCount(), System.currentTimeMillis() - result.getQueueEntryTime());
		return result;
	}

	/**
	 * Get the bundle size to use for bundle size tuning.
	 * @param bundle the bundle to get the size from.
	 * @return the bundle size as an int.
	 */
	private int getSize(JPPFTaskBundle bundle)
	{
		//return bundle.getTaskCount();
		return bundle.getInitialTaskCount();
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
		latestMaxSize = sizeMap.isEmpty() ? latestMaxSize : sizeMap.lastKey();
		return latestMaxSize;
	}
}
