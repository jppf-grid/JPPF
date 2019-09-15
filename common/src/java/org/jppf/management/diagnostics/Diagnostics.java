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

package org.jppf.management.diagnostics;

import java.io.*;
import java.lang.management.*;

import org.jppf.JPPFException;
import org.jppf.management.diagnostics.provider.MonitoringConstants;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
import org.slf4j.*;

/**
 * Implementation of the {@link DiagnosticsMBean} interface.
 * @author Laurent Cohen
 * @exclude
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
   * Triggers a heap dump based on the JVM implementation.
   */
  private HeapDumpCollector heapDumpCollector;
  static {
    if (threadsMXBean.isThreadCpuTimeSupported()) {
      if (!threadsMXBean.isThreadCpuTimeEnabled()) threadsMXBean.setThreadCpuTimeEnabled(true);
    } else if (debugEnabled) log.debug("CPU time collection is not supported - CPU load will be unavailable");
    if (threadsMXBean.isThreadContentionMonitoringSupported()) threadsMXBean.setThreadContentionMonitoringEnabled(true);
  }

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
    heapDumpCollector = HeapDumpCollector.Factory.newInstance();
    if (heapDumpCollector == null) log.info("a heap dump collector could not be created for this JVM - no heap dumps will be available");
    MonitoringDataProviderHandler.getProviders();
    // provider initialization can take a long time, in particular oshi
    final Runnable r = () -> {
      MonitoringDataProviderHandler.initProviders();
      MonitoringDataProviderHandler.getAllProperties();
    };
    ThreadUtils.startThread(r, "DataProviderInit");
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
    MonitoringDataProviderHandler.getProviders().forEach(provider -> snapshot.putProperties(provider.getValues())); 
    return snapshot;
  }

  @Override
  public String healthSnapshotAsString() throws Exception {
    return healthSnapshot().getProperties().asString();
  }

  @Override
  public String heapDump() throws Exception {
    if (heapDumpCollector == null) throw new JPPFException("heap dumps are not available for this JVM");
    return heapDumpCollector.dumpHeap();
  }

  @Override
  public Double cpuLoad() {
    try {
      return healthSnapshot().getDouble(MonitoringConstants.PROCESS_CPU_LOAD);
    } catch(final Exception e) {
      log.error("error getting cpu load", e);
    }
    return -1d;
  }

  @Override
  public void close() throws IOException {
  }
}
