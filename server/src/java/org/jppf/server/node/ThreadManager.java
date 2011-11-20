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

package org.jppf.server.node;

import java.lang.management.*;
import java.util.List;
import java.util.concurrent.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class manages the thread for the node's execution manager.
 * @author Laurent Cohen
 */
public class ThreadManager extends ThreadSynchronization
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ThreadManager.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The thread pool that really processes the tasks
   */
  private ThreadPoolExecutor threadPool = null;
  /**
   * The factory used to create thread in the pool.
   */
  private JPPFThreadFactory threadFactory = null;
  /**
   * The platform MBean used to gather statistics about the JVM threads.
   */
  private ThreadMXBean threadMXBean = null;
  /**
   * Determines whether the thread cpu time measurement is supported and enabled.
   */
  private boolean cpuTimeEnabled = false;

  /**
   * Initialize this execution manager with the specified node.
   */
  public ThreadManager()
  {
    TypedProperties props = JPPFConfiguration.getProperties();
    int poolSize = props.getInt("processing.threads", -1);
    if (poolSize < 0)
    {
      poolSize = Runtime.getRuntime().availableProcessors();
      props.setProperty("processing.threads", Integer.toString(poolSize));
    }
    log.info("Node running " + poolSize + " processing thread" + (poolSize > 1 ? "s" : ""));
    threadMXBean = ManagementFactory.getThreadMXBean();
    cpuTimeEnabled = threadMXBean.isThreadCpuTimeSupported();
    threadFactory = new JPPFThreadFactory("node processing", cpuTimeEnabled);
    LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    threadPool = new ThreadPoolExecutor(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.MICROSECONDS, queue, threadFactory);
    if (debugEnabled) log.debug("thread cpu time supported = " + cpuTimeEnabled);
    if (cpuTimeEnabled) threadMXBean.setThreadCpuTimeEnabled(true);
  }

  /**
   * Set the size of the node's thread pool.
   * @param size the size as an int.
   */
  public void setThreadPoolSize(final int size)
  {
    if (size <= 0)
    {
      log.warn("ignored attempt to set the thread pool size to 0 or less: " + size);
      return;
    }
    int n = getThreadPoolSize();
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
    log.info("Node thread pool size changed from " + n + " to " + size);
    JPPFConfiguration.getProperties().setProperty("processing.threads", Integer.toString(size));
  }

  /**
   * Get the size of the node's thread pool.
   * @return the size as an int.
   */
  public int getThreadPoolSize()
  {
    if (threadPool == null) return 0;
    return threadPool.getCorePoolSize();
  }

  /**
   * Get the total cpu time used by the task processing threads.
   * @return the cpu time on milliseconds.
   */
  public long getCpuTime()
  {
    if (!cpuTimeEnabled) return 0L;
    return computeExecutionInfo().cpuTime;
  }

  /**
   * Computes the total CPU time used by the execution threads.
   * @return a <code>NodeExecutionInfo</code> instance.
   */
  protected NodeExecutionInfo computeExecutionInfo()
  {
    if (!cpuTimeEnabled) return null;
    NodeExecutionInfo info = new NodeExecutionInfo();
    List<Long> ids = threadFactory.getThreadIDs();
    for (Long id: ids)
    {
      info.cpuTime += threadMXBean.getThreadCpuTime(id);
      info.userTime += threadMXBean.getThreadUserTime(id);
    }
    info.cpuTime /= 1.0e6;
    info.userTime /= 1.0e6;
    return info;
  }

  /**
   * Get the current cpu time for the thread identified by the specified id.
   * @param threadId the id of the thread to the cpu time from.
   * @return the cpu time as a long value.
   */
  public long getCpuTime(final long threadId)
  {
    return threadMXBean.getThreadCpuTime(threadId);
  }

  /**
   * Get the priority assigned to the execution threads.
   * @return the priority as an int value.
   */
  public int getThreadsPriority()
  {
    return threadFactory.getPriority();
  }

  /**
   * Update the priority of all execution threads.
   * @param newPriority the new priority to set.
   */
  public void updateThreadsPriority(final int newPriority)
  {
    threadFactory.updatePriority(newPriority);
  }

  /**
   * Get the thread pool that really processes the tasks
   * @return a {@link ThreadPoolExecutor} instance.
   */
  public ThreadPoolExecutor getThreadPool()
  {
    return threadPool;
  }

  /**
   * Determines whether the thread cpu time measurement is supported and enabled.
   * @return true is cpu time measurement is enabled, false otherwise.
   */
  public boolean isCpuTimeEnabled()
  {
    return cpuTimeEnabled;
  }

  /**
   * Get the factory used to create thread in the pool.
   * @return a {@link JPPFThreadFactory} instance.
   */
  public JPPFThreadFactory getThreadFactory()
  {
    return threadFactory;
  }

  /**
   * Get the platform MBean used to gather statistics about the JVM threads.
   * @return a {@link ThreadMXBean} instance.
   */
  public ThreadMXBean getThreadMXBean()
  {
    return threadMXBean;
  }
}
