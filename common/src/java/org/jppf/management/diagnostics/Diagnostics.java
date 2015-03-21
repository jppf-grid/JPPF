/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.io.*;
import java.lang.management.*;

import javax.management.*;

import org.jppf.JPPFException;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Implementation of the {@link DiagnosticsMBean} interface.
 * @author Laurent Cohen
 */
public class Diagnostics implements DiagnosticsMBean, Closeable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(Diagnostics.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
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
   * Whether the full operating system MXBean features are available or not.
   */
  private boolean osMXBeanAvailable = true;
  /**
   * The object name of the operating system MXBean.
   */
  private ObjectName osMXBeanName = null;
  /**
   * The platform MBean server.
   */
  private static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

  /**
   * Initialize this MBean implementation.
   * @param closeableType the name of the type of closeable to use.
   */
  public Diagnostics(final String closeableType) {
    init();
    CloseableHandler.addResetCloseable(closeableType, this);
  }

  /**
   * Initialize this Mbean.
   */
  private void init() {
    if (debugEnabled) log.debug("initializing " + getClass().getSimpleName());
    if (threadsMXBean.isThreadCpuTimeSupported()) {
      if (debugEnabled) log.debug("Starting CPU time collector thread");
      if (!threadsMXBean.isThreadCpuTimeEnabled()) threadsMXBean.setThreadCpuTimeEnabled(true);
      try {
        Class.forName("com.sun.management.OperatingSystemMXBean");
        osMXBeanName = new ObjectName("java.lang", "type", "OperatingSystem");
      } catch (Exception e) {
        osMXBeanAvailable = false;
        //System.out.println("OperatingSystemMXBean not avaialble!");
      }
      if (!osMXBeanAvailable) {
        cpuTimeCollector = new CPUTimeCollector();
        Thread thread = new Thread(cpuTimeCollector, "CPUTimeCollector");
        thread.setDaemon(true);
        thread.start();
      }
    } else if (debugEnabled) log.debug("CPU time collection is not supported - CPU load will be unavailable");
    if (threadsMXBean.isThreadContentionMonitoringSupported()) {
      if (!threadsMXBean.isThreadContentionMonitoringEnabled()) threadsMXBean.setThreadContentionMonitoringEnabled(true);
    }
    heapDumpCollector = HeapDumpCollector.Factory.newInstance();
    if (heapDumpCollector == null) {
      if (debugEnabled) log.debug("a heap dump collector could not be created for this JVM - no heap dumps will be available");
    }
  }

  @Override
  public MemoryInformation memoryInformation() throws Exception {
    return new MemoryInformation();
  }

  @Override
  public void gc() throws Exception {
    System.gc();
  }

  @Override
  public String[] threadNames() throws Exception {
    long[] ids = threadsMXBean.getAllThreadIds();
    ThreadInfo[] infos = threadsMXBean.getThreadInfo(ids, 0);
    String[] result = new String[infos.length];
    for (int i=0; i<infos.length; i++) result[i] = infos[i].getThreadName();
    return result;
  }

  @Override
  public ThreadDump threadDump() throws Exception {
    checkThreadCapabilities();
    return new ThreadDump(ManagementFactory.getThreadMXBean());
  }

  /**
   * Ensure that thread contention monitoring is enabled, if it is supported.
   */
  private void checkThreadCapabilities() {
    if (threadsMXBean.isThreadContentionMonitoringSupported() && !threadsMXBean.isThreadContentionMonitoringEnabled())
      threadsMXBean.setThreadContentionMonitoringEnabled(true);
  }

  @Override
  public Boolean hasDeadlock() throws Exception {
    long[] ids = threadsMXBean.findDeadlockedThreads();
    return (ids != null) && (ids.length > 0);
  }

  @Override
  public HealthSnapshot healthSnapshot() throws Exception {
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
    snapshot.processCpuLoad = cpuLoad();
    snapshot.systemCpuLoad = osMXBeanDoubleValue("SystemCpuLoad");
    long freeRam = osMXBeanLongValue("FreePhysicalMemorySize");
    if (freeRam >= 0L) {
      long totalRam = osMXBeanLongValue("TotalPhysicalMemorySize");
      snapshot.ramUsed = totalRam - freeRam;
      snapshot.ramUsedRatio = (double) snapshot.ramUsed / (double) totalRam;
    } else {
      snapshot.ramUsed = -1L;
      snapshot.ramUsedRatio = -1d;
    }
    return snapshot;
  }

  /**
   * Get the number of live threads i the JVM.
   * @return the number of threads as an int.
   */
  private int liveThreads() {
    return threadsMXBean.getThreadCount();
  }

  @Override
  public String heapDump() throws Exception {
    if (heapDumpCollector == null) throw new JPPFException("heap dumps are not available for this JVM");
    return heapDumpCollector.dumpHeap();
  }

  @Override
  public Double cpuLoad() {
    if (osMXBeanAvailable) return osMXBeanDoubleValue("ProcessCpuLoad");
    return cpuTimeCollector == null ? -1d : cpuTimeCollector.getLoad();
  }

  @Override
  public void close() throws IOException {
    if (cpuTimeCollector != null) cpuTimeCollector.setStopped(true);
  }

  /**
   * Get the value of a double attribute from the OS mxbean.
   * @param attribute the name of the attribute to get the value from.
   * @return the attribute value as a double.
   */
  private double osMXBeanDoubleValue(final String attribute) {
    if (osMXBeanAvailable) {
      try {
        return (Double) mbeanServer.getAttribute(osMXBeanName, attribute);
      } catch (Exception e) {
        if (debugEnabled) log.debug("error getting attribute '{}': {}", attribute, ExceptionUtils.getMessage(e));
      }
    }
    return -1d;
  }

  /**
   * Get the value of a double attribute from the OS mxbean.
   * @param attribute the name of the attribute to get the value from.
   * @return the attribute value as a double.
   */
  private long osMXBeanLongValue(final String attribute) {
    if (osMXBeanAvailable) {
      try {
        return (long) mbeanServer.getAttribute(osMXBeanName, attribute);
      } catch (Exception e) {
        if (debugEnabled) log.debug("error getting attribute '{}': {}", attribute, ExceptionUtils.getMessage(e));
      }
    }
    return -1L;
  }
}
