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

import static org.jppf.management.diagnostics.provider.MonitoringConstants.*;

import java.lang.management.*;

import javax.management.*;

import org.jppf.management.diagnostics.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class is the built-in JPPF monitoring data provider.
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
  /**
   * Object that holds all references to Oshi API objects.
   * This allows using this built-in provider on the console side without needing OShi classes in the classpath.
   */
  private Oshi oshi;

  @Override
  public void defineProperties() {
    setDoubleProperty(HEAP_USAGE_RATIO, -1d);
    setDoubleProperty(HEAP_USAGE_MB, -1d);
    setDoubleProperty(NON_HEAP_USAGE_RATIO, -1d);
    setDoubleProperty(NON_HEAP_USAGE_MB, -1d);
    setBooleanProperty(DEADLOCKED, false);
    setIntProperty(LIVE_THREADS_COUNT, -1);
    setDoubleProperty(PROCESS_CPU_LOAD, -1d);
    setDoubleProperty(SYSTEM_CPU_LOAD, -1d);
    setDoubleProperty(PROCESS_RESIDENT_SET_SIZE, -1d);
    setDoubleProperty(PROCESS_VIRTUAL_SIZE, -1d);
    setDoubleProperty(RAM_USAGE_RATIO, -1d);
    setDoubleProperty(RAM_USAGE_MB, -1d);
    setDoubleProperty(SWAP_USAGE_RATIO, -1d);
    setDoubleProperty(SWAP_USAGE_MB, -1d);
    setDoubleProperty(CPU_TEMPERATURE, -1d);
    setStringProperty(OS_NAME, "n/a");
    setDoubleProperty(PROCESS_RESIDENT_SET_SIZE, -1d);
    setDoubleProperty(PROCESS_VIRTUAL_SIZE, -1d);
  }

  @Override
  public void init() {
    oshi = new Oshi().init();
  }

  @Override
  public TypedProperties getValues() {
    final TypedProperties props = oshi.getValues();
    final MemoryInformation memInfo = memoryInformation();
    MemoryUsageInformation mem = memInfo.getHeapMemoryUsage();
    props.setDouble(HEAP_USAGE_RATIO, 100d * mem.getUsedRatio());
    props.setDouble(HEAP_USAGE_MB, (double) mem.getUsed() / MB);
    mem = memInfo.getNonHeapMemoryUsage();
    props.setDouble(NON_HEAP_USAGE_RATIO, 100d * mem.getUsedRatio());
    props.setDouble(NON_HEAP_USAGE_MB, (double) mem.getUsed() / MB);
    final long[] ids = threadsMXBean.findDeadlockedThreads();
    props.setBoolean(DEADLOCKED, (ids != null) && (ids.length > 0));
    props.setInt(LIVE_THREADS_COUNT, threadsMXBean.getThreadCount());
    props.setDouble(PROCESS_CPU_LOAD, 100d * osMXBeanDoubleValue("ProcessCpuLoad"));
    props.setDouble(SYSTEM_CPU_LOAD, 100d * osMXBeanDoubleValue("SystemCpuLoad"));
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
}
