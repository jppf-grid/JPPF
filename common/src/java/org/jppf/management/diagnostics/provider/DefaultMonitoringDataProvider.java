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

package org.jppf.management.diagnostics.provider;

import java.lang.management.*;

import javax.management.*;

import org.jppf.management.diagnostics.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class DefaultMonitoringDataProvider extends MonitoringDataProvider {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(DefaultMonitoringDataProvider.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Base path for localzation bundles.
   */
  private static final String I18N_BASE = "org.jppf.management.diagnostics.provider.DefaultMonitoringDataProvider";
  /**
   * 
   */
  private static final long KB = 1024L, MB = KB * 1024L;
  /**
   * Reference to the platform's {@link ThreadMXBean} instance.
   */
  private static final ThreadMXBean threadsMXBean = ManagementFactory.getThreadMXBean();
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

  @Override
  public void defineProperties() {
    setDoubleProperty("heapUsedRatio", -1d);
    setDoubleProperty("heapUsed", -1d);
    setDoubleProperty("nonheapUsedRatio", -1d);
    setDoubleProperty("nonheapUsed", -1d);
    setBooleanProperty("deadlocked", false);
    setIntProperty("liveThreads", -1);
    setDoubleProperty("processCpuLoad", -1d);
    setDoubleProperty("systemCpuLoad", -1d);
    setDoubleProperty("ramUsedRatio", -1d);
    setDoubleProperty("ramUsed", -1d);
  }

  @Override
  public void init() {
  }

  @Override
  public TypedProperties getValues() {
    final TypedProperties props = new TypedProperties();
    final MemoryInformation memInfo = memoryInformation();
    MemoryUsageInformation mem = memInfo.getHeapMemoryUsage();
    props.setDouble("heapUsedRatio", 100d * mem.getUsedRatio());
    props.setDouble("heapUsed", (double) mem.getUsed() / MB);
    mem = memInfo.getNonHeapMemoryUsage();
    props.setDouble("nonheapUsedRatio", 100d * mem.getUsedRatio());
    props.setDouble("nonheapUsed", (double) mem.getUsed() / MB);
    final long[] ids = threadsMXBean.findDeadlockedThreads();
    props.setBoolean("deadlocked", (ids != null) && (ids.length > 0));
    props.setInt("liveThreads", threadsMXBean.getThreadCount());
    props.setDouble("processCpuLoad", 100d * osMXBeanDoubleValue("ProcessCpuLoad"));
    props.setDouble("systemCpuLoad", 100d * osMXBeanDoubleValue("SystemCpuLoad"));
    final long freeRam = osMXBeanLongValue("FreePhysicalMemorySize");
    if (freeRam >= 0L) {
      final long totalRam = osMXBeanLongValue("TotalPhysicalMemorySize");
      final long ramUsed = totalRam - freeRam;
      props.setDouble("ramUsed", (double) ramUsed / MB);
      props.setDouble("ramUsedRatio", 100d * ramUsed / totalRam);
    }
    return props;
  }

  @Override
  protected String getLocalizationBase() {
    return I18N_BASE;
  }

  /**
   * Get the memory info for the whole JVM.
   * @return a {@link MemoryInformation} instance.
   */
  private static MemoryInformation memoryInformation() {
    return new MemoryInformation();
  }

  /**
   * Get the value of a double attribute from the OS mxbean.
   * @param attribute the name of the attribute to get the value from.
   * @return the attribute value as a double.
   */
  private static double osMXBeanDoubleValue(final String attribute) {
    if (osMXBeanAvailable) {
      try {
        return ((Number) mbeanServer.getAttribute(osMXBeanName, attribute)).doubleValue();
      } catch (final Exception e) {
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
  private static long osMXBeanLongValue(final String attribute) {
    if (osMXBeanAvailable) {
      try {
        return ((Number) mbeanServer.getAttribute(osMXBeanName, attribute)).longValue();
      } catch (final Exception e) {
        if (debugEnabled) log.debug("error getting attribute '{}': {}", attribute, ExceptionUtils.getMessage(e));
      }
    }
    return -1L;
  }
}
