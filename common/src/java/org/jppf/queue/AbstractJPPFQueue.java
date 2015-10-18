/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.queue;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

import org.jppf.utils.collections.LinkedListSortedMap;

/**
 * Abstract superclass for all JPPFQueue implementations.
 * @param <T> the type of jobs that are queued.
 * @param <U> the type of bundles the jobs are split into.
 * @param <V> the type of resulting bundles the jobs are split into.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public abstract class AbstractJPPFQueue<T, U, V> implements JPPFQueue<T, U, V> {
  /**
   * Used for synchronized access to the queue.
   */
  protected final Lock lock = new ReentrantLock();
  /**
   * An ordered map of bundle sizes, mapping to a count of bundles of this size.
   */
  protected final SortedMap<Integer, AtomicInteger> sizeMap = new TreeMap<>();
  /**
   *
   */
  protected AtomicInteger latestMaxSize = new AtomicInteger(0);
  /**
   * The list of registered listeners.
   */
  protected final List<QueueListener<T, U, V>> queueListeners = new ArrayList<>();
  /**
   * Comparator for job priority.
   */
  private static final Comparator<Integer> PRIORITY_COMPARATOR = new Comparator<Integer>() {
    @Override
    public int compare(final Integer o1, final Integer o2) {
      if (o1 == null) return (o2 == null) ? 0 : 1;
      else if (o2 == null) return -1;
      return o2.compareTo(o1);
    }
  };
  /**
   * A map of task bundles, ordered by descending priority.
   */
  protected final LinkedListSortedMap<Integer, T> priorityMap = new LinkedListSortedMap<>(PRIORITY_COMPARATOR);
  /**
   * Contains the ids of all queued jobs.
   */
  protected final Map<String, T> jobMap = new HashMap<>();

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add to the list.
   */
  public void addQueueListener(final QueueListener<T, U, V> listener) {
    synchronized (queueListeners) {
      queueListeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove from the list.
   */
  public void removeQueueListener(final QueueListener<T, U, V> listener) {
    synchronized(queueListeners) {
      queueListeners.remove(listener);
    }
  }

  /**
   * Get the bundle size to use for bundle size tuning.
   * @param bundleWrapper the bundle to get the size from.
   * @return the bundle size as an int.
   */
  protected abstract int getSize(final T bundleWrapper);

  /**
   * Notify all queue listeners that a bundle was added tot he queue.
   * @param event the event to notify of.
   */
  public void fireBundleAdded(final QueueEvent<T, U, V> event) {
    synchronized(queueListeners) {
      for (QueueListener<T, U, V> listener : queueListeners) listener.bundleAdded(event);
    }
  }

  /**
   * Notify all queue listeners of that a bundle was removed form the queue.
   * @param event the event to notify of.
   * @since 4.1
   */
  public void fireBundleRemoved(final QueueEvent<T, U, V> event) {
    synchronized (queueListeners) {
      for (QueueListener<T, U, V> listener : queueListeners) listener.bundleRemoved(event);
    }
  }

  /**
   * Get the lock used for synchronized access to the queue.
   * @return a <code>Lock</code> instance.
   */
  public Lock getLock()
  {
    return lock;
  }

  @Override
  public int getMaxBundleSize() {
    return latestMaxSize.get();
  }

  /**
   * Update the value of the max bundle size.
   */
  public void updateLatestMaxSize() {
    if (!sizeMap.isEmpty()) latestMaxSize.set(sizeMap.lastKey());
  }

  @Override
  public Iterator<T> iterator() {
    return priorityMap.iterator(lock);
  }

  @Override
  public boolean isEmpty() {
    lock.lock();
    try {
      return priorityMap.isEmpty();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the size of this job queue.
   * <p>This method should be used with caution, as its cost is in O(n),
   * with n being the number of jobs in the queue.
   * @return the number of jobs currently in the queue.
   * @since 4.1
   */
  public int getQueueSize() {
    lock.lock();
    try {
      return priorityMap.size();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Increment the count of jobs that have the specified size.
   * @param size the size for which to increment the count.
   */
  public void incrementSizeCount(final int size) {
    synchronized(sizeMap) {
      AtomicInteger count = sizeMap.get(size);
      if (count == null) sizeMap.put(size, new AtomicInteger(1));
      else count.incrementAndGet();
    }
  }

  /**
   * Decrement the count of jobs that have the specified size.
   * @param size the size for which to decrement the count.
   */
  public void decrementSizeCount(final int size) {
    synchronized(sizeMap) {
      AtomicInteger count = sizeMap.get(size);
      if (count == null) return;
      int n = count.decrementAndGet();
      if (n <= 0) sizeMap.remove(size);
    }
  }
}
