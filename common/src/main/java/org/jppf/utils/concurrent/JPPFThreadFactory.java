/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.utils.concurrent;

import java.security.*;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.utils.ExceptionUtils;

/**
 * Custom thread factory used mostly to specify the names of created threads.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFThreadFactory implements ThreadFactory {
  /**
   * The name used as prefix for the constructed threads name.
   */
  private String name;
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
  private List<Long> threadIDs;
  /**
   * Whether created threads are daemon threads.
   */
  private long[] threadIDsArray = new long[0];
  /**
   * The thread group that contains the threads of this factory.
   */
  ThreadGroup threadGroup;
  /**
   * Priority assigned to the threads created by this factory.
   */
  private int priority = Thread.NORM_PRIORITY;
  /**
   * 
   */
  private static ExceptionHandler defaultExceptionHandler = new ExceptionHandler();
  /**
   * Indicates whether new thread should be created in PrivilegedAction.
   */
  private final boolean doPrivileged;
  /**
   * Indicates whether new thread should be created as daemon threads.
   */
  private final boolean daemonThreads;

  /**
   * Initialize this thread factory with the specified name.
   * @param name the name used as prefix for the constructed threads name.
   */
  public JPPFThreadFactory(final String name) {
    this(name, false, Thread.NORM_PRIORITY, false, true);
  }

  /**
   * Initialize this thread factory with the specified name.
   * @param name the name used as prefix for the constructed threads name.
   * @param priority priority assigned to the threads created by this factory.
   */
  public JPPFThreadFactory(final String name, final int priority) {
    this(name, false, priority, false, true);
  }

  /**
   * Initialize this thread factory with the specified name.
   * @param name the name used as prefix for the constructed threads name.
   * @param monitoringEnabled determines whether the threads created by this factory can be monitored.
   */
  public JPPFThreadFactory(final String name, final boolean monitoringEnabled) {
    this(name, monitoringEnabled, Thread.NORM_PRIORITY, false, true);
  }

  /**
   * Initialize this thread factory with the specified name.
   * @param name the name used as prefix for the constructed threads name.
   * @param monitoringEnabled determines whether the threads created by this factory can be monitored.
   * @param daemon whether created threads are daemon threads.
   */
  public JPPFThreadFactory(final String name, final boolean monitoringEnabled, final boolean daemon) {
    this(name, monitoringEnabled, Thread.NORM_PRIORITY, false, daemon);
  }

  /**
   * Initialize this thread factory with the specified name.
   * @param name the name used as prefix for the constructed threads name.
   * @param monitoringEnabled determines whether the threads created by this factory can be monitored.
   * @param priority priority assigned to the threads created by this factory.
   * @param doPrivileged indicates whether thread should be created in PrivilegedAction.
   * @param daemon whether created threads are daemon threads.
   */
  public JPPFThreadFactory(final String name, final boolean monitoringEnabled, final int priority, final boolean doPrivileged, final boolean daemon) {
    this.name = name == null ? "JPPFThreadFactory" : name;
    threadGroup = new ThreadGroup(this.name + " thread group");
    threadGroup.setMaxPriority(Thread.MAX_PRIORITY);
    this.monitoringEnabled = monitoringEnabled;
    this.priority = priority;
    this.doPrivileged = doPrivileged;
    this.daemonThreads = daemon;
    if (monitoringEnabled) threadIDs = new ArrayList<>();
  }

  @Override
  public synchronized Thread newThread(final Runnable r) {
    final Thread thread;
    final int n = count.incrementAndGet();
    final String threadName = String.format("%s-%04d", name, n);
    if (doPrivileged) thread = AccessController.doPrivileged((PrivilegedAction<Thread>) () -> new DebuggableThread(threadGroup, r, threadName));
    else thread = new DebuggableThread(threadGroup, r, threadName);
    if (monitoringEnabled) {
      threadIDs.add(thread.getId());
      computeThreadIDs();
    }
    thread.setPriority(priority);
    thread.setUncaughtExceptionHandler(defaultExceptionHandler);
    thread.setDaemon(daemonThreads);
    return thread;
  }

  /**
   * Get the ids of the monitored threads.
   * @return a list of long values.
   */
  public synchronized long[] getThreadIDs() {
    return threadIDsArray;
  }

  /**
   * Compute the ids of the monitored threads.
   */
  private synchronized void computeThreadIDs() {
    if (!monitoringEnabled || (threadIDs == null) || threadIDs.isEmpty()) return;
    threadIDsArray = new long[threadIDs.size()];
    int i = 0;
    for (long id: threadIDs) threadIDsArray[i++] = id;
  }

  /**
   * Update the priority of all threads created by this factory.
   * @param newPriority the new priority to set.
   */
  public synchronized void updatePriority(final int newPriority) {
    if ((newPriority < Thread.MIN_PRIORITY) || (newPriority > Thread.MAX_PRIORITY) || (priority == newPriority)) return;
    final int count = threadGroup.activeCount();
    // count is an estimate only, so we play it safe and take 2x its value.
    final Thread[] threads = new Thread[2 * count];
    final int n = threadGroup.enumerate(threads);
    for (int i=0; i<n; i++) threads[i].setPriority(newPriority);
    priority = newPriority;
  }

  /**
   * Get the priority assigned to the threads created by this factory.
   * @return the priority as an int value.
   */
  public synchronized int getPriority() {
    return priority;
  }

  /**
   * Default uncaught exception handler set onto the threeads created by the thread factory.
   */
  private static class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
      System.out.println("exception caught from thread " + t + " :\n" + ExceptionUtils.getStackTrace(e));
    }
  }
}
