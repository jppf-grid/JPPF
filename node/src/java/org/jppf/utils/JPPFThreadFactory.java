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

package org.jppf.utils;

import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom thread factory used mostly to specify the names of created threads.
 * @author Laurent Cohen
 */
public class JPPFThreadFactory implements ThreadFactory
{
  /**
   * The name used as prefix for the constructed threads name.
   */
  private String name = null;
  /**
   * Count of created threads.
   */
  private AtomicInteger count = new AtomicInteger(0);
  /**
   * Determines whether the threads created by this factory can be monitored.
   */
  private boolean monitoringEnabled = false;
  /**
   * List of monitored thread IDs.
   */
  private List<Long> threadIDs = null;
  /**
   * The thread group that contains the threads of this factory.
   */
  private ThreadGroup threadGroup = null;
  /**
   * Priority assigned to the threads created by this factory.
   */
  private int priority = Thread.NORM_PRIORITY;

  /**
   * Initialize this thread factory with the specified name.
   * @param name the name used as prefix for the constructed threads name.
   */
  public JPPFThreadFactory(final String name)
  {
    this(name, false, Thread.NORM_PRIORITY);
  }

  /**
   * Initialize this thread factory with the specified name.
   * @param name the name used as prefix for the constructed threads name.
   * @param priority priority assigned to the threads created by this factory.
   */
  public JPPFThreadFactory(final String name, final int priority)
  {
    this(name, false, priority);
  }

  /**
   * Initialize this thread factory with the specified name.
   * @param name the name used as prefix for the constructed threads name.
   * @param monitoringEnabled determines whether the threads created by this factory can be monitored.
   */
  public JPPFThreadFactory(final String name, final boolean monitoringEnabled)
  {
    this(name, monitoringEnabled, Thread.NORM_PRIORITY);
  }

  /**
   * Initialize this thread factory with the specified name.
   * @param name the name used as prefix for the constructed threads name.
   * @param monitoringEnabled determines whether the threads created by this factory can be monitored.
   * @param priority priority assigned to the threads created by this factory.
   */
  public JPPFThreadFactory(final String name, final boolean monitoringEnabled, final int priority)
  {
    this.name = name == null ? "JPPFThreadFactory" : name;
    threadGroup = new ThreadGroup(this.name + " thread group");
    threadGroup.setMaxPriority(Thread.MAX_PRIORITY);
    this.monitoringEnabled = monitoringEnabled;
    if (monitoringEnabled) threadIDs = new ArrayList<Long>();
  }

  /**
   * Constructs a new Thread.
   * @param r a runnable to be executed by the new thread instance.
   * @return the constructed thread.
   * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
   */
  @Override
  public synchronized Thread newThread(final Runnable r)
  {
    Thread thread = new Thread(threadGroup, r, name + "-thread-" + incrementCount());
    if (monitoringEnabled) threadIDs.add(thread.getId());
    thread.setPriority(priority);
    //thread.setDaemon(false);
    return thread;
  }

  /**
   * Get the ids of the monitored threads.
   * @return a list of long values.
   */
  public List<Long> getThreadIDs()
  {
    if (!monitoringEnabled) return null;
    return Collections.unmodifiableList(threadIDs);
  }

  /**
   * Increment and return the created thread count.
   * @return the created thread count.
   */
  private int incrementCount()
  {
    return count.incrementAndGet();
  }

  /**
   * Update the priority of all threads created by this factory.
   * @param newPriority the new priority to set.
   */
  public synchronized void updatePriority(final int newPriority)
  {
    if ((newPriority < Thread.MIN_PRIORITY) || (newPriority > Thread.MAX_PRIORITY) || (priority == newPriority)) return;
    int count = threadGroup.activeCount();
    // count is an estimate only, so we play it safe and take 2x its value.
    Thread[] threads = new Thread[2 * count];
    int n = threadGroup.enumerate(threads);
    for (int i=0; i<n; i++) threads[i].setPriority(newPriority);
    priority = newPriority;
  }

  /**
   * Get the priority assigned to the threads created by this factory.
   * @return the priority as an int value.
   */
  public synchronized int getPriority()
  {
    return priority;
  }
}
