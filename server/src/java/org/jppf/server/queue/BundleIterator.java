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

package org.jppf.server.queue;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import org.jppf.server.protocol.BundleWrapper;

/**
 * Iterator that traverses the collection of task bundles in descending order of their priority.
 * This iterator is read-only and does not support the <code>remove()</code> operation.
 */
class BundleIterator implements Iterator<BundleWrapper>
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
	 * Used for synchronized access to the queue.
	 */
	private ReentrantLock lock = new ReentrantLock();

	/**
	 * Initialize this iterator.
	 * @param priorityMap the map of prioritized jobs.
	 * @param lock used to synchronize with the queue.
	 */
	public BundleIterator(TreeMap<JPPFPriority, List<BundleWrapper>> priorityMap, ReentrantLock lock)
	{
		this.lock = lock;
		lock.lock();
		try
		{
			entryIterator = priorityMap.entrySet().iterator();
			if (entryIterator.hasNext()) listIterator = entryIterator.next().getValue().iterator();
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Determines whether an element remains to visit.
	 * @return true if there is at least one element that hasn't been visited, false otherwise.
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
    public boolean hasNext()
	{
		lock.lock();
		try
		{
			return entryIterator.hasNext() || ((listIterator != null) && listIterator.hasNext());
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Get the next element for this iterator.
	 * @return the next element as a <code>JPPFTaskBundle</code> instance.
	 * @see java.util.Iterator#next()
	 */
	@Override
    public BundleWrapper next()
	{
		lock.lock();
		try
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
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * This operation is not supported and throws an <code>UnsupportedOperationException</code>.
	 * @throws UnsupportedOperationException as this operation is not supported.
	 * @see java.util.Iterator#remove()
	 */
	@Override
    public void remove() throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException("remove() is not supported on a BundleIterator");
	}
}