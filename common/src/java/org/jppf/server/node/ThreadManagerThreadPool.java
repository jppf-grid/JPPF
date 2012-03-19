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

package org.jppf.server.node;

import org.jppf.utils.JPPFThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class manages the thread pool pool for the node's execution manager.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public class ThreadManagerThreadPool extends ThreadManager
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ThreadManagerThreadPool.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The thread pool that really processes the tasks
   */
  private ThreadPoolExecutor threadPool;
  /**
   * The factory used to create thread in the pool.
   */
  private JPPFThreadFactory threadFactory = null;
  /**
   * The platform MBean used to gather statistics about the JVM threads.
   */
  private ThreadMXBean threadMXBean;

  /**
   * Initialized thread manager.
   * @param poolSize the initial size of the thread pool.
   * @param threadMXBean the MBean used to gather statistics about the JVM threads.
   */
  public ThreadManagerThreadPool(final int poolSize, final ThreadMXBean threadMXBean)
  {
    this.threadMXBean = threadMXBean;
    threadFactory = new JPPFThreadFactory("node processing", threadMXBean.isThreadCpuTimeSupported() && threadMXBean.isThreadCpuTimeEnabled());
    LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    threadPool = new ThreadPoolExecutor(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.MICROSECONDS, queue, threadFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NodeExecutionInfo computeExecutionInfo()
  {
    List<Long> ids = threadFactory.getThreadIDs();
    long cpuTime = 0L;
    long userTime = 0L;
    for (Long id: ids)
    {
      cpuTime += threadMXBean.getThreadCpuTime(id);
      userTime += threadMXBean.getThreadUserTime(id);
    }
    NodeExecutionInfo info = new NodeExecutionInfo();
    info.cpuTime = cpuTime;
    info.userTime = userTime;
    return info;
  }

  /**
   * Computes the CPU time used by thread identified by threadID.
   * @param threadID the thread ID.
   * @return a <code>NodeExecutionInfo</code> instance.
   */
  protected NodeExecutionInfo computeExecutionInfo(final long threadID) {
    NodeExecutionInfo info = new NodeExecutionInfo();
    info.cpuTime = threadMXBean.getThreadCpuTime(threadID);
    info.userTime = threadMXBean.getThreadUserTime(threadID);
    return info;
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
    int n = getPoolSize();
    if (n == size) return;

    ThreadPoolExecutor tpe = threadPool;
    if (size > tpe.getCorePoolSize())
    {
      tpe.setMaximumPoolSize(size);
      tpe.setCorePoolSize(size);
    }
    else if (size < tpe.getCorePoolSize())
    {
      tpe.setCorePoolSize(size);
      tpe.setMaximumPoolSize(size);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getPoolSize()
  {
    return threadPool.getMaximumPoolSize();
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
    threadFactory.updatePriority(priority);
  }
}
