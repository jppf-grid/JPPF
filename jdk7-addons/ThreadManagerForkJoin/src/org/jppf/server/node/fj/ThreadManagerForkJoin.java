/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.server.node.fj;

import org.jppf.node.NodeExecutionInfo;
import org.jppf.server.node.AbstractThreadManager;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * This class manages the fork join pool for the node's execution manager.
 * @author Martin JANDA
 */
public class ThreadManagerForkJoin extends AbstractThreadManager
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ThreadManagerForkJoin.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The thread pool that really processes the tasks
   */
  private ForkJoinPool threadPool;
  /**
   * The factory used to create thread in the pool.
   */
  private FJThreadFactory threadFactory;

  /**
   * Initialized thread manager.
   * @param poolSize the initial size of the thread pool.
   */
  public ThreadManagerForkJoin(final int poolSize)
  {
    this.threadFactory = new FJThreadFactory("node processing", threadMXBean.isThreadCpuTimeSupported() && threadMXBean.isThreadCpuTimeEnabled());
    threadPool = new ForkJoinPool(poolSize, threadFactory, new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(final Thread t, final Throwable e) {
        StringBuilder sb = new StringBuilder();
        new Formatter(sb).format("UncaughtException in thread[%d:%s] - %s", t.getId(), t.getName(), ExceptionUtils.getMessage(e));
        //System.out.println(sb);
        //e.printStackTrace(System.out);
        log.error(sb.toString(), e);
      }
    }, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected long[] getThreadIds()
  {
    return threadFactory.getThreadIDs();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExecutorService getExecutorService()
  {
    return threadPool;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPoolSize(final int size)
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getPoolSize()
  {
    return threadPool.getParallelism();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getPriority()
  {
    return threadFactory.getPriority();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPriority(final int priority)
  {
    threadFactory.setPriority(priority);
  }

  /**
   *
   */
  protected class FJThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    /**
     * The name used as prefix for the constructed threads name.
     */
    private String name = null;
    /**
     * List of monitored thread IDs.
     */
    private List<Long> threadIDs;
    /**
     * The thread group that contains the threads of this factory.
     */
    private ThreadGroup threadGroup = null;
    /**
     * Priority assigned to the threads created by this factory.
     */
    private int priority = Thread.NORM_PRIORITY;
    /**
     * Summary information for all terminated threads.
     */
    private NodeExecutionInfo terminatedInfo = new NodeExecutionInfo();
    /**
     * List of monitored threads.
     */
    private final List<Thread> threadList = new ArrayList<Thread>();
    /**
     * Initialize this thread factory with the specified name.
     * @param name the name used as prefix for the constructed threads name.
     */
    public FJThreadFactory(final String name)
    {
      this(name, false, Thread.NORM_PRIORITY);
    }

    /**
     * Initialize this thread factory with the specified name.
     * @param name the name used as prefix for the constructed threads name.
     * @param priority priority assigned to the threads created by this factory.
     */
    public FJThreadFactory(final String name, final int priority)
    {
      this(name, false, priority);
    }

    /**
     * Initialize this thread factory with the specified name.
     * @param name the name used as prefix for the constructed threads name.
     * @param monitoringEnabled determines whether the threads created by this factory can be monitored.
     */
    public FJThreadFactory(final String name, final boolean monitoringEnabled)
    {
      this(name, monitoringEnabled, Thread.NORM_PRIORITY);
    }

    /**
     * Initialize this thread factory with the specified name.
     * @param name the name used as prefix for the constructed threads name.
     * @param monitoringEnabled determines whether the threads created by this factory can be monitored.
     * @param priority priority assigned to the threads created by this factory.
     */
    public FJThreadFactory(final String name, final boolean monitoringEnabled, final int priority)
    {
      this.name = name == null ? "JPPFThreadFactory" : name;
      this.priority = priority;
      threadGroup = new ThreadGroup(this.name + " thread group");
      threadGroup.setMaxPriority(Thread.MAX_PRIORITY);
      if (monitoringEnabled)
        threadIDs = new ArrayList<Long>();
      else
        threadIDs = null;
    }

    @Override
    public synchronized ForkJoinWorkerThread newThread(final ForkJoinPool pool)
    {
      ForkJoinWorkerThread thread = new ForkJoinWorkerThread(pool) {
        @Override
        protected void onTermination(final Throwable exception)
        {
          try {
            terminate(this, exception);
          } finally {
            super.onTermination(exception);
          }
        }
      };
      if(threadIDs != null) threadIDs.add(thread.getId());
      threadList.add(thread);
      thread.setPriority(priority);
      return thread;
    }

    /**
     * Process terminated thread that is no longer used by fork join pool.
     * @param thread the thread instance.
     * @param exception the exception of unrecoverable error or {@code null}
     */
    protected synchronized void terminate(final Thread thread, final Throwable exception) {
      if(threadIDs != null) {
        long threadID = thread.getId();
        threadIDs.remove(threadID);
        terminatedInfo.add(computeExecutionInfo(threadID));
      }
      if(exception != null) {
        System.out.printf("Thread [%d:%s] terminated with exception: %s%n", thread.getId(), thread.getName(), exception);
        exception.printStackTrace(System.out);
      }
      threadList.remove(this);
    }

    /**
     * Get the ids of the monitored threads.
     * @return a list of long values.
     */
    public synchronized long[] getThreadIDs()
    {
      if(threadIDs == null || threadIDs.isEmpty())
        return new long[0];
      long[] ids = new long[threadIDs.size()];
      int dstIndex = 0;
      for (Long id : threadIDs) {
        ids[dstIndex++] = id;
      }
      return ids;
    }

    /**
     * Update the priority of all threads created by this factory.
     * @param priority the new priority to set.
     */
    public synchronized void setPriority(final int priority)
    {
      if ((priority < Thread.MIN_PRIORITY) || (priority > Thread.MAX_PRIORITY) || (this.priority == priority)) return;
      for (Thread thread : threadList) {
         thread.setPriority(priority);
      }
      this.priority = priority;
    }

    /**
     * Get the priority assigned to the threads created by this factory.
     * @return the priority as an int value.
     */
    public synchronized int getPriority()
    {
      return priority;
    }

    /**
     * Get the summary information for all terminated threads.
     * @return a <code>NodeExecutionInfo</code> instance.
     */
    public synchronized NodeExecutionInfo getTerminatedInfo() {
      return terminatedInfo;
    }
  }
}
