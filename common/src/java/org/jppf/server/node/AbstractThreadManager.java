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
import java.util.concurrent.ExecutorService;

import org.jppf.node.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class manages the thread for the node's execution manager.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public abstract class AbstractThreadManager implements ThreadManager
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractThreadManager.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The platform MBean used to gather statistics about the JVM threads.
   */
  protected ThreadMXBean threadMXBean = null;
  /**
   * Determines whether the thread cpu time measurement is supported and enabled.
   */
  protected boolean cpuTimeEnabled = false;

  /**
   * Initialize this execution manager with the specified node.
   */
  protected AbstractThreadManager()
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
    props.setProperty("cpuTimeSupported", Boolean.toString(cpuTimeEnabled));
    if (cpuTimeEnabled) threadMXBean.setThreadCpuTimeEnabled(true);
    log.info("Thread CPU time measurement is " + (cpuTimeEnabled ? "" : "not ") + "supported");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract void setPoolSize(final int size);

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract int getPoolSize();

  /**
   * {@inheritDoc}
   */
  @Override
  public NodeExecutionInfo computeExecutionInfo()
  {
    NodeExecutionInfo info = new NodeExecutionInfo();
    long[] ids = getThreadIds();
    for (long id: ids) info.add(computeExecutionInfo(id));
    return info;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NodeExecutionInfo computeExecutionInfo(final long threadID)
  {
    return (!cpuTimeEnabled) ? new NodeExecutionInfo() : new NodeExecutionInfo(threadMXBean.getThreadCpuTime(threadID), threadMXBean.getThreadUserTime(threadID));
  }

  /**
   * Get the ids of all managed threads. 
   * @return the ids as an array of long values, may be an empty but never null array.
   */
  protected abstract long[] getThreadIds();

  /**
   * {@inheritDoc}
   */
  @Override
  public long getCpuTime(final long threadId)
  {
    return cpuTimeEnabled ? threadMXBean.getThreadCpuTime(threadId) : -1L;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract int getPriority();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract void setPriority(final int newPriority);

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ExecutorService getExecutorService();

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isCpuTimeEnabled()
  {
    return cpuTimeEnabled;
  }
}
