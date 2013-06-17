/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import org.jppf.node.NodeExecutionInfo;

/**
 * 
 * @author Laurent Cohen
 */
public class CpuTimeCollector
{
  /**
   * The platform MBean used to gather statistics about the JVM threads.
   */
  protected static ThreadMXBean threadMXBean = null;
  /**
   * Determines whether the thread cpu time measurement is supported and enabled.
   */
  protected static boolean cpuTimeEnabled = false;
  static
  {
    threadMXBean = ManagementFactory.getThreadMXBean();
    cpuTimeEnabled = threadMXBean.isThreadCpuTimeSupported();
    if (cpuTimeEnabled) threadMXBean.setThreadCpuTimeEnabled(true);
  }

  /**
   * Computes the CPU time used by thread identified by threadID.
   * @param threadID the thread ID.
   * @return a <code>NodeExecutionInfo</code> instance.
   */
  public static NodeExecutionInfo computeExecutionInfo(final long threadID)
  {
    return (!cpuTimeEnabled) ? new NodeExecutionInfo() : new NodeExecutionInfo(threadMXBean.getThreadCpuTime(threadID), threadMXBean.getThreadUserTime(threadID));
  }

  /**
   * Get the current cpu time for the thread identified by the specified id.
   * @param threadId the id of the thread to the cpu time from.
   * @return the cpu time as a long value.
   */
  public static long getCpuTime(final long threadId)
  {
    return cpuTimeEnabled ? threadMXBean.getThreadCpuTime(threadId) : -1L;
  }

  /**
   * Determines whether the thread cpu time measurement is supported and enabled.
   * @return true is cpu time measurement is enabled, false otherwise.
   */
  public static boolean isCpuTimeEnabled()
  {
    return cpuTimeEnabled;
  }
}
