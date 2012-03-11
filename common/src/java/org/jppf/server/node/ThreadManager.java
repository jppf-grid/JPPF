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

import java.lang.management.*;
import java.util.concurrent.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class manages the thread for the node's execution manager.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public abstract class ThreadManager
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
   * The platform MBean used to gather statistics about the JVM threads.
   */
  private ThreadMXBean threadMXBean = null;

  /**
   * Initialize this execution manager with the specified node.
   */
  protected ThreadManager()
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
    boolean cpuTimeEnabled = threadMXBean.isThreadCpuTimeSupported();
//    threadPool = new ForkJoinPool(poolSize);
    if (debugEnabled) log.debug("thread cpu time supported = " + cpuTimeEnabled);
    if (cpuTimeEnabled) threadMXBean.setThreadCpuTimeEnabled(true);

//    threadService = new ThreadServiceThreadPool(poolSize, threadMXBean);
//    threadService = new ThreadServiceForkJoin(poolSize, threadMXBean);
  }

  /**
   * Set the size of the node's thread pool.
   * @param size the size as an int.
   */
  public abstract void setPoolSize(final int size);

  /**
   * Get the size of the node's thread pool.
   * @return the size as an int.
   */
  public abstract int getPoolSize();

  /**
   * Computes the total CPU time used by the execution threads.
   * @return a <code>NodeExecutionInfo</code> instance.
   */
  public abstract NodeExecutionInfo computeExecutionInfo();

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
  public abstract int getPriority();

  /**
   * Update the priority of all execution threads.
   * @param newPriority the new priority to set.
   */
  public abstract void setPriority(final int newPriority);

  /**
   * Get the thread pool that really processes the tasks
   * @return a {@link ThreadPoolExecutor} instance.
   */
  public abstract ExecutorService getExecutorService();

  /**
   * Determines whether the thread cpu time measurement is supported and enabled.
   * @return true is cpu time measurement is enabled, false otherwise.
   */
  public boolean isCpuTimeEnabled()
  {
    return threadMXBean.isThreadCpuTimeSupported() && threadMXBean.isThreadCpuTimeEnabled();
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
