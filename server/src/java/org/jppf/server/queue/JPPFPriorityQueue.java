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

package org.jppf.server.queue;

import static org.jppf.server.JPPFStatsUpdater.*;
import static org.jppf.utils.CollectionUtils.*;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.io.BundleWrapper;
import org.jppf.server.protocol.JPPFTaskBundle;

/**
 * A JPPF queue whose elements are ordered by decreasing priority.
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
	private TreeMap<JPPFPriority, List<BundleWrapper>> priorityMap = new TreeMap<JPPFPriority, List<BundleWrapper>>();

	/**
	 * Add an object to the queue, and notify all listeners about it.
	 * @param bundleWrapper the object to add to the queue.
	 * @see org.jppf.server.queue.JPPFQueue#addBundle(org.jppf.io.BundleWrapper)
	 */
	public void addBundle(BundleWrapper bundleWrapper)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		bundle.setQueueEntryTime(System.currentTimeMillis());
		try
		{
			lock.lock();
			if (debugEnabled) log.debug("adding bundle with [priority=" + bundle.getPriority()+", initialTasksCount=" +
				bundle.getInitialTaskCount() + ", taskCount=" + bundle.getTaskCount() + "]");
			putInListMap(new JPPFPriority(bundle.getPriority()), bundleWrapper, priorityMap);
			putInListMap(getSize(bundleWrapper), bundleWrapper, sizeMap);
		}
		finally
		{
			lock.unlock();
		}
		if (debugEnabled) log.debug("Maps size information:\n" + formatSizeMapInfo("priorityMap", priorityMap) + "\n" +
			formatSizeMapInfo("sizeMap", sizeMap));
		taskInQueue(bundle.getTaskCount());
		for (QueueListener listener : listeners) listener.newBundle(this);
	}

	/**
	 * Get the next object in the queue.
	 * @param nbTasks the maximum number of tasks to get out of the bundle.
	 * @return the most recent object that was added to the queue.
	 * @see org.jppf.server.queue.AbstractJPPFQueue#nextBundle(int)
	 */
	public BundleWrapper nextBundle(int nbTasks)
	{
		Iterator<BundleWrapper> it = iterator();
		if (it.hasNext()) return nextBundle(it.next(),  nbTasks);
		return null;
	}

	/**
	 * Get the next object in the queue.
	 * @param bundleWrapper the bundle to either remove or extract a sub-bundle from.
	 * @param nbTasks the maximum number of tasks to get out of the bundle.
	 * @return the most recent object that was added to the queue.
	 * @see org.jppf.server.queue.AbstractJPPFQueue#nextBundle(org.jppf.io.BundleWrapper, int)
	 */
	public BundleWrapper nextBundle(BundleWrapper bundleWrapper, int nbTasks)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		BundleWrapper result = null;
		try
		{
			lock.lock();
			if (debugEnabled) log.debug("requesting bundle with " + nbTasks + " tasks");
			if (debugEnabled) log.debug("next bundle has " + bundle.getTaskCount() + " tasks");
			int size = getSize(bundleWrapper);
			removeFromListMap(size, bundleWrapper, sizeMap);
			if (nbTasks >= bundle.getTaskCount())
			{
				if (debugEnabled) log.debug("removing bundle from queue");
				result = bundleWrapper;
				removeFromListMap(new JPPFPriority(bundle.getPriority()), bundleWrapper, priorityMap);
			}
			else
			{
				if (debugEnabled) log.debug("removing " + nbTasks + " tasks from bundle");
				result = bundleWrapper.copy(nbTasks);
				int newSize = bundle.getTaskCount();
				List<BundleWrapper> list = sizeMap.get(newSize);
				if (list == null)
				{
					list = new ArrayList<BundleWrapper>();
					//sizeMap.put(newSize, list);
					sizeMap.put(size, list);
				}
				list.add(bundleWrapper);
			}
			result.getBundle().setExecutionStartTime(System.currentTimeMillis());
		}
		finally
		{
			lock.unlock();
		}
		if (debugEnabled) log.debug("Maps size information:\n" + formatSizeMapInfo("priorityMap", priorityMap) + "\n" +
			formatSizeMapInfo("sizeMap", sizeMap));
		taskOutOfQueue(result.getBundle().getTaskCount(), System.currentTimeMillis() - result.getBundle().getQueueEntryTime());
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
	public Iterator<BundleWrapper> iterator()
	{
		return new BundleIterator();
	}

	/**
	 * Iterator that traverses the collection of task bundles in descending order of their priority.
	 * This iterator is read-only and does not support the <code>remove()</code> operation.
	 */
	private class BundleIterator implements Iterator<BundleWrapper>
	{
		/**
		 * Iterator over the entries in the priority map.
		 */
		private Iterator<Map.Entry<JPPFPriority, List<BundleWrapper>>> entryIterator = null;
		/**
		 * Iterator over the task bundles in the map entry specified by <code>entryIterator</code>.
		 */
		private Iterator<BundleWrapper> listIterator = null;

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
		public BundleWrapper next()
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
		 * @throws UnsupportedOperationException as this operation is not supported.
		 * @see java.util.Iterator#remove()
		 */
		public void remove() throws UnsupportedOperationException
		{
			throw new UnsupportedOperationException("remove() is not supported on a BundleIterator");
		}
	}
}
