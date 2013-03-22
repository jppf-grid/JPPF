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

package org.jppf.management.diagnostics;

import java.lang.management.*;

import javax.management.NotificationBroadcasterSupport;

import org.jppf.JPPFException;
import org.slf4j.*;

/**
 * Implementation of the {@link DiagnosticsMBean} interface.
 * @author Laurent Cohen
 */
public class Diagnostics extends NotificationBroadcasterSupport implements DiagnosticsMBean
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(Diagnostics.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Reference to the platform's {@link ThreadMXBean} instance.
   */
  private final ThreadMXBean threadsMXBean = ManagementFactory.getThreadMXBean();
  /**
   * Reference to the platform's {@link ThreadMXBean} instance.
   */
  private final OperatingSystemMXBean systemMXBean = ManagementFactory.getOperatingSystemMXBean();
  /**
   * Collects regular snapshots of the total CPU time.
   */
  private CPUTimeCollector cpuTimeCollector = null;
  /**
   * Triggers a heap dump based on the JVM implementation.
   */
  private HeapDumpCollector heapDumpCollector = null;

  /**
   * Initialize this MBean.
   */
  public Diagnostics()
  {
    if (debugEnabled) log.debug("initializing " + getClass().getSimpleName());
    if (threadsMXBean.isThreadCpuTimeSupported())
    {
      if (debugEnabled) log.debug("Starting CPU time collector thread");
      if (!threadsMXBean.isThreadCpuTimeEnabled()) threadsMXBean.setThreadCpuTimeEnabled(true);
      cpuTimeCollector = new CPUTimeCollector();
      Thread thread = new Thread(cpuTimeCollector, "CPUTimeCollector");
      thread.setDaemon(true);
      thread.start();
    }
    else if (debugEnabled) log.debug("CPU time collection is not supported - CPU load will be unavailable");
    heapDumpCollector = HeapDumpCollector.Factory.newInstance();
    if (heapDumpCollector == null)
    {
      if (debugEnabled) log.debug("a heap dump collector could not be created for this JVM - no heap dumps will be available");
    }
  }

  @Override
  public MemoryInformation memoryInformation() throws Exception
  {
    return new MemoryInformation();
  }

  @Override
  public void gc() throws Exception
  {
    System.gc();
  }

  @Override
  public String[] threadNames() throws Exception
  {
    long[] ids = threadsMXBean.getAllThreadIds();
    ThreadInfo[] infos = threadsMXBean.getThreadInfo(ids, 0);
    String[] result = new String[infos.length];
    for (int i=0; i<infos.length; i++) result[i] = infos[i].getThreadName();
    return result;
  }

  @Override
  public ThreadDump threadDump() throws Exception
  {
    checkThreadCapabilities();
    return new ThreadDump(ManagementFactory.getThreadMXBean());
  }

  /**
   * Ensure that thread contention monitoring is enabled, if it is supported.
   */
  private void checkThreadCapabilities()
  {
    if (threadsMXBean.isThreadContentionMonitoringSupported() && !threadsMXBean.isThreadContentionMonitoringEnabled())
      threadsMXBean.setThreadContentionMonitoringEnabled(true);
  }

  @Override
  public Boolean hasDeadlock() throws Exception
  {
    long[] ids = threadsMXBean.findDeadlockedThreads();
    return (ids != null) && (ids.length > 0);
  }

  @Override
  public HealthSnapshot healthSnapshot() throws Exception
  {
    HealthSnapshot snapshot = new HealthSnapshot();
    MemoryInformation memInfo = memoryInformation();
    MemoryUsageInformation mem = memInfo.getHeapMemoryUsage();
    snapshot.heapUsedRatio = mem.getUsedRatio();
    snapshot.heapUsed = mem.getUsed();
    mem = memInfo.getNonHeapMemoryUsage();
    snapshot.nonheapUsedRatio = mem.getUsedRatio();
    snapshot.nonheapUsed = mem.getUsed();
    snapshot.deadlocked = hasDeadlock();
    snapshot.liveThreads = threadsMXBean.getThreadCount();
    //snapshot.cpuLoad = systemMXBean.getSystemLoadAverage();
    if (cpuTimeCollector != null) snapshot.cpuLoad = cpuTimeCollector.getLoad();
    else snapshot.cpuLoad = -1d;
    return snapshot;
  }

  /**
   * Get the number of live threads i the JVM.
   * @return the number of threads as an int.
   */
  private int liveThreads()
  {
    return threadsMXBean.getThreadCount();
  }

  @Override
  public String heapDump() throws Exception
  {
    if (heapDumpCollector == null) throw new JPPFException("heap dumps are not available for this JVM");
    return heapDumpCollector.dumpHeap();
  }
}
