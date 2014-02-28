/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * Abstract superclass for all JPPFQueue implementations.
 * @param <T> the type of jobs that are queued.
 * @param <U> the type of bundles the jobs are split into.
 * @param <V> the type of resulting bundles the jobs are split into.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public abstract class AbstractJPPFQueue<T, U, V> implements JPPFQueue<T, U, V>
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractJPPFQueue.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Used for synchronized access to the queue.
   */
  protected final Lock lock = new ReentrantLock();
  /**
   * An ordered map of bundle sizes, mapping to a list of bundles of this size.
   */
  protected final SetSortedMap<Integer, T> sizeMap = new SetSortedMap<>();
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
  public void addQueueListener(final QueueListener<T, U, V> listener)
  {
    synchronized (queueListeners)
    {
      queueListeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove from the list.
   */
  public void removeQueueListener(final QueueListener<T, U, V> listener)
  {
    synchronized (queueListeners)
    {
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
   * Notify all queue listeners of an event.
   * @param event the event to notify of.
   */
  protected void fireQueueEvent(final QueueEvent<T, U, V> event)
  {
    synchronized (queueListeners)
    {
      for (QueueListener<T, U, V> listener : queueListeners) listener.newBundle(event);
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
  public int getMaxBundleSize()
  {
    return latestMaxSize.get();
  }

  /**
   * Update the value of the max bundle size.
   */
  protected void updateLatestMaxSize()
  {
    if (!sizeMap.isEmpty()) latestMaxSize.set(sizeMap.lastKey());
  }

  @Override
  public Iterator<T> iterator()
  {
    return priorityMap.iterator(lock);
  }

  @Override
  public boolean isEmpty()
  {
    lock.lock();
    try {
      return priorityMap.isEmpty();
    } finally {
      lock.unlock();
    }
  }

}
