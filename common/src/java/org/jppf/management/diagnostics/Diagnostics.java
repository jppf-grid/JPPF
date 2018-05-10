/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
import org.jppf.management.diagnostics.provider.*;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
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
  private static final ThreadMXBean threadsMXBean = ManagementFactory.getThreadMXBean();
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
  private static boolean osMXBeanAvailable = true;
  /**
   * The object name of the operating system MXBean.
   */
  private static ObjectName osMXBeanName = null;
  /**
   * The platform MBean server.
   */
  private static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
  static {
    if (threadsMXBean.isThreadCpuTimeSupported()) {
      if (!threadsMXBean.isThreadCpuTimeEnabled()) threadsMXBean.setThreadCpuTimeEnabled(true);
      try {
        Class.forName("com.sun.management.OperatingSystemMXBean");
        osMXBeanName = new ObjectName("java.lang", "type", "OperatingSystem");
        if (debugEnabled) log.debug("CPU load collection from OperatingSystemMXBean is enabled");
      } catch (@SuppressWarnings("unused") final Exception e) {
        osMXBeanAvailable = false;
        log.info("OperatingSystemMXBean not avaialble, an approximation of the process CPU load will be computed");
      }
    } else if (debugEnabled) log.debug("CPU time collection is not supported - CPU load will be unavailable");
    if (threadsMXBean.isThreadContentionMonitoringSupported()) threadsMXBean.setThreadContentionMonitoringEnabled(true);
  }
  /**
   * The monitoring data provider handler.
   */
  private final MonitoringDataProviderHandler dataProvidersHandler = new MonitoringDataProviderHandler();

  /**
   * Initialize this MBean implementation.
   * @param closeableType the name of the type of closeable to use.
   */
  public Diagnostics(final String closeableType) {
    try {
      init();
      CloseableHandler.addResetCloseable(closeableType, this);
    } catch (final RuntimeException e) {
      log.error(e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Initialize this Mbean.
   */
  private void init() {
    if (debugEnabled) log.debug("initializing " + getClass().getSimpleName());
    if (!osMXBeanAvailable) {
      if (debugEnabled) log.debug("Starting CPU time collector thread");
      cpuTimeCollector = new CPUTimeCollector();
      ThreadUtils.startDaemonThread(cpuTimeCollector, "CPUTimeCollector");
    }
    heapDumpCollector = HeapDumpCollector.Factory.newInstance();
    if (heapDumpCollector == null) {
      if (debugEnabled) log.debug("a heap dump collector could not be created for this JVM - no heap dumps will be available");
    }
    dataProvidersHandler.loadProviders();
    dataProvidersHandler.initProviders();
    dataProvidersHandler.defineProperties();
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
    final long[] ids = threadsMXBean.getAllThreadIds();
    final ThreadInfo[] infos = threadsMXBean.getThreadInfo(ids, 0);
    final String[] result = new String[infos.length];
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
  private static void checkThreadCapabilities() {
    if (threadsMXBean.isThreadContentionMonitoringSupported() && !threadsMXBean.isThreadContentionMonitoringEnabled())
      threadsMXBean.setThreadContentionMonitoringEnabled(true);
  }

  @Override
  public Boolean hasDeadlock() throws Exception {
    final long[] ids = threadsMXBean.findDeadlockedThreads();
    return (ids != null) && (ids.length > 0);
  }

  @Override
  public HealthSnapshot healthSnapshot() throws Exception {
    final HealthSnapshot snapshot = new HealthSnapshot();
    for (final MonitoringDataProvider provider: dataProvidersHandler.getProviders()) {
      snapshot.putProperties(provider.getValues());
    }
    return snapshot;
  }

  @Override
  public String heapDump() throws Exception {
    if (heapDumpCollector == null) throw new JPPFException("heap dumps are not available for this JVM");
    return heapDumpCollector.dumpHeap();
  }

  @Override
  public Double cpuLoad() {
    if (osMXBeanAvailable) return 100d * osMXBeanDoubleValue("ProcessCpuLoad");
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
  private static double osMXBeanDoubleValue(final String attribute) {
    if (osMXBeanAvailable) {
      try {
        return (Double) mbeanServer.getAttribute(osMXBeanName, attribute);
      } catch (final Exception e) {
        if (debugEnabled) log.debug("error getting attribute '{}': {}", attribute, ExceptionUtils.getMessage(e));
      }
    }
    return -1d;
  }
}
