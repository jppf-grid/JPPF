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

package org.jppf.server.queue;

import static org.jppf.server.JPPFStatsUpdater.*;
import static org.jppf.utils.CollectionUtils.*;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.server.protocol.JPPFTaskBundle;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFPriorityQueue extends AbstractJPPFQueue
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFPriorityQueue.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * An of task bundles, ordered by descending priority.
	 */
	private TreeMap<JPPFPriority, List<JPPFTaskBundle>> priorityMap = new TreeMap<JPPFPriority, List<JPPFTaskBundle>>();

	/**
	 * Add an object to the queue, and notify all listeners about it.
	 * @param bundle the object to add to the queue.
	 * @see org.jppf.server.queue.JPPFQueue#addBundle(org.jppf.server.protocol.JPPFTaskBundle)
	 */
	public void addBundle(JPPFTaskBundle bundle)
	{
		bundle.setQueueEntryTime(System.currentTimeMillis());
		try
		{
			lock.lock();
			if (debugEnabled) log.debug("adding bundle with [initialTasksCount=" + bundle.getInitialTaskCount() +
				", taskCount=" + bundle.getTaskCount() + "]");
			putInListMap(new JPPFPriority(bundle.getPriority()), bundle, priorityMap);
			putInListMap(getSize(bundle), bundle, sizeMap);
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
	 * @see org.jppf.server.queue.JPPFQueue#nextBundle(JPPFTaskBundle, int)
	 */
	public JPPFTaskBundle nextBundle(int nbTasks)
	{
		Iterator<JPPFTaskBundle> it = iterator();
		if (it.hasNext()) return nextBundle(it.next(),  nbTasks);
		return null;
	}

	/**
	 * Get the next object in the queue.
	 * @param queuedBundle the bundle to either remove or extract a sub-bundle from.
	 * @param nbTasks the maximum number of tasks to get out of the bundle.
	 * @return the most recent object that was added to the queue.
	 * @see org.jppf.server.queue.JPPFQueue#nextBundle(JPPFTaskBundle, int)
	 */
	public JPPFTaskBundle nextBundle(JPPFTaskBundle queuedBundle, int nbTasks)
	{
		JPPFTaskBundle result = null;
		try
		{
			lock.lock();
			if (debugEnabled) log.debug("requesting bundle with " + nbTasks + " tasks");
			JPPFTaskBundle bundle = queuedBundle;
			if (debugEnabled) log.debug("next bundle has " + bundle.getTaskCount() + " tasks");
			int size = getSize(bundle);
			removeFromListMap(size, bundle, sizeMap);
			if (nbTasks >= bundle.getTaskCount())
			{
				if (debugEnabled) log.debug("removing bundle from queue");
				result = bundle;
				removeFromListMap(new JPPFPriority(bundle.getPriority()), bundle, priorityMap);
			}
			else
			{
				if (debugEnabled) log.debug("removing " + nbTasks + " tasks from bundle");
				result = bundle.copy(nbTasks);
				int newSize = bundle.getTaskCount();
				List<JPPFTaskBundle> list = sizeMap.get(newSize);
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
		if (debugEnabled) logSizeMapInfo();
		taskOutOfQueue(result.getTaskCount(), System.currentTimeMillis() - result.getQueueEntryTime());
		return result;
	}

	/**
	 * Determine whether the queue is empty or not.
	 * @return true if the queue is empty, false otherwise.
	 * @see org.jppf.server.queue.JPPFQueue#isEmpty()
	 */
	public boolean isEmpty()
	{
		lock.lock();
		try
		{
			return priorityMap.isEmpty();
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Get the maximum bundle size for the bundles present in the queue.
	 * @return the bundle size as an int.
	 * @see org.jppf.server.queue.JPPFQueue#getMaxBundleSize()
	 */
	public int getMaxBundleSize()
	{
		latestMaxSize = sizeMap.isEmpty() ? latestMaxSize : sizeMap.lastKey();
		return latestMaxSize;
	}

	/**
	 * Get an iterator on the task bundles in this queue.
	 * @return an iterator.
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<JPPFTaskBundle> iterator()
	{
		return new BundleIterator();
	}

	/**
	 * Iterator that traverses the collection of task bundles in descending order of their priority.
	 * This iterator is read-only and does not support the <code>remove()</code> operation.
	 */
	private class BundleIterator implements Iterator<JPPFTaskBundle>
	{
		/**
		 * Iterator over the entries in the priority map.
		 */
		private Iterator<Map.Entry<JPPFPriority, List<JPPFTaskBundle>>> entryIterator = null;
		/**
		 * Iterator over the task bundles in the map entry specified by <code>entryIterator</code>.
		 */
		private Iterator<JPPFTaskBundle> listIterator = null;

		/**
		 * Initialize this iterator.
		 */
		public BundleIterator()
		{
			entryIterator = priorityMap.entrySet().iterator();
			if (entryIterator.hasNext()) listIterator = entryIterator.next().getValue().iterator();
		}

		/**
		 * Determines whether an element remains to visit.
		 * @return true if there is at least one element that hasn't been visited, false otherwise.
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext()
		{
			return entryIterator.hasNext() || ((listIterator != null) && listIterator.hasNext());
		}

		/**
		 * Get the next element for this iterator.
		 * @return the next element as a <code>JPPFTaskBundle</code> instance.
		 * @see java.util.Iterator#next()
		 */
		public JPPFTaskBundle next()
		{
			if (listIterator != null)
			{
				 if (listIterator.hasNext()) return listIterator.next();
				 if (entryIterator.hasNext())
				 {
					 listIterator = entryIterator.next().getValue().iterator();
					 if (listIterator.hasNext()) return listIterator.next();
				 }
			}
			throw new NoSuchElementException("no more element in this BundleIterator");
		}

		/**
		 * This operation is not supported and throws an <code>UnsupportedOperationException</code>.
		 * @see java.util.Iterator#remove()
		 */
		public void remove()
		{
			throw new UnsupportedOperationException("remove() is not supported on a BundleIterator");
		}
	}
}
