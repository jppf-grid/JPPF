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

package org.jppf.management.diagnostics.provider;

import static org.jppf.management.diagnostics.provider.MonitoringConstants.*;

import java.lang.management.*;
import java.util.*;

import javax.management.*;

import org.jppf.management.diagnostics.*;
import org.jppf.management.diagnostics.provider.MonitoringValueConverter.*;
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
   * Reference to the platform's {@link RuntimeMXBean} instance.
   */
  private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
  /**
   * The platform MBean server.
   */
  private static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
  static {
    if (threadsMXBean.isThreadCpuTimeSupported()) {
      if (!threadsMXBean.isThreadCpuTimeEnabled()) threadsMXBean.setThreadCpuTimeEnabled(true);
    } else if (debugEnabled) log.debug("CPU time collection is not supported - CPU load will be unavailable");
    if (threadsMXBean.isThreadContentionMonitoringSupported()) threadsMXBean.setThreadContentionMonitoringEnabled(true);
  }
  /**
   * Double converter with 2 fractional digits.
   */
  private static final MonitoringValueConverter FRACTION_2_CONVERTER = new DoubleConverterWithFractionDigits(2, null, new Range<>(0d, Double.MAX_VALUE));
  /**
   * Double converter with 2 fractional digits.
   */
  private static final MonitoringValueConverter PCT_CONVERTER = new DoubleConverterWithFractionDigits(2, null, new Range<>(0d, 100d));
  /**
   * Object that holds all references to Oshi API objects.
   * This allows using this built-in provider on the console side without needing OShi classes in the classpath.
   */
  private Oshi oshi;
  /**
   * Collects regular snapshots of the total CPU time for the current process.
   */
  private CPUTimeCollector cpuTimeCollector;

  /**
   * 
   */
  public DefaultMonitoringDataProvider() {
  }

  @Override
  public void defineProperties() {
    setDoubleProperty(HEAP_USAGE_RATIO, -1d).setConverter(HEAP_USAGE_RATIO, PCT_CONVERTER);
    setDoubleProperty(HEAP_USAGE_MB, -1d).setConverter(HEAP_USAGE_MB, FRACTION_2_CONVERTER);
    setDoubleProperty(NON_HEAP_USAGE_RATIO, -1d).setConverter(NON_HEAP_USAGE_RATIO, PCT_CONVERTER);
    setDoubleProperty(NON_HEAP_USAGE_MB, -1d).setConverter(NON_HEAP_USAGE_MB, FRACTION_2_CONVERTER);
    setBooleanProperty(DEADLOCKED, false);
    setIntProperty(LIVE_THREADS_COUNT, -1);
    setIntProperty(PEAK_THREADS_COUNT, -1);
    setLongProperty(STARTED_THREADS_COUNT, -1L);
    setDoubleProperty(PROCESS_CPU_LOAD, -1d).setConverter(PROCESS_CPU_LOAD, PCT_CONVERTER);
    setDoubleProperty(SYSTEM_CPU_LOAD, -1d).setConverter(SYSTEM_CPU_LOAD, PCT_CONVERTER);
    setDoubleProperty(PROCESS_RESIDENT_SET_SIZE, -1d).setConverter(PROCESS_RESIDENT_SET_SIZE, FRACTION_2_CONVERTER);
    setDoubleProperty(PROCESS_VIRTUAL_SIZE, -1d).setConverter(PROCESS_VIRTUAL_SIZE, FRACTION_2_CONVERTER);
    setDoubleProperty(RAM_USAGE_RATIO, -1d).setConverter(RAM_USAGE_RATIO, PCT_CONVERTER);
    setDoubleProperty(RAM_USAGE_MB, -1d).setConverter(RAM_USAGE_MB, FRACTION_2_CONVERTER);
    setDoubleProperty(SWAP_USAGE_RATIO, -1d).setConverter(SWAP_USAGE_RATIO, PCT_CONVERTER);
    setDoubleProperty(SWAP_USAGE_MB, -1d).setConverter(SWAP_USAGE_MB, FRACTION_2_CONVERTER);
    setDoubleProperty(CPU_TEMPERATURE, -1d);
    setStringProperty(OS_NAME, "n/a");
    setDoubleProperty(PROCESS_RESIDENT_SET_SIZE, -1d).setConverter(PROCESS_RESIDENT_SET_SIZE, FRACTION_2_CONVERTER);
    setDoubleProperty(PROCESS_VIRTUAL_SIZE, -1d).setConverter(PROCESS_VIRTUAL_SIZE, FRACTION_2_CONVERTER);
    setLongProperty(JVM_UPTIME, -1L).setConverter(JVM_UPTIME, (LongConverter) StringUtils::toStringDuration);
  }

  @Override
  public void init() {
    oshi = new Oshi().init();
    if (debugEnabled) log.debug("Starting CPU time collector thread");
    cpuTimeCollector = CPUTimeCollector.getInstance();
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
    final double[] values = mxBeanDoubleValues(threadsMXBean.getObjectName(), "ThreadCount", "PeakThreadCount", "TotalStartedThreadCount");
    props.setInt(LIVE_THREADS_COUNT, (int) values[0]);
    props.setInt(PEAK_THREADS_COUNT, (int) values[1]);
    props.setLong(STARTED_THREADS_COUNT, (int) values[2]);
    double d = cpuTimeCollector.getLoad();
    if ((d < 0d) || (d > 1d)) d = -1d;
    props.setDouble(PROCESS_CPU_LOAD, 100d * d);
    props.setLong(JVM_UPTIME, runtimeMXBean.getUptime());
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
   * @param mbeanName the object name of the MXBean.
   * @param attribute the name of the attribute to get the value from.
   * @return the attribute value as a double.
   */
  static double mxBeanDoubleValue(final ObjectName mbeanName, final String attribute) {
    try {
      return ((Number) mbeanServer.getAttribute(mbeanName, attribute)).doubleValue();
    } catch (final Exception e) {
      if (debugEnabled) log.debug("error getting attribute '{}' of MBean '{}': {}", attribute, mbeanName, ExceptionUtils.getMessage(e));
    }
    return -1d;
  }

  /**
   * Get the value of a double attribute from the OS mxbean.
   * @param mbeanName the object name of the MXBean.
   * @param attributes the name of the attributes to get the value from.
   * @return the attribute value as a double.
   */
  private static double[] mxBeanDoubleValues(final ObjectName mbeanName, final String...attributes) {
    try {
      final Map<String, Double> values = new HashMap<>();
      final AttributeList attrs = mbeanServer.getAttributes(mbeanName, attributes);
      if (attrs != null) {
        final List<Attribute> attrList = attrs.asList();
        for (final Attribute attr: attrList) {
          values.put(attr.getName(), ((Number) attr.getValue()).doubleValue());
        }
      }
      final double[] result = new double[attributes.length];
      for (int i=0; i<attributes.length; i++) {
        final Double d = values.get(attributes[i]);
        result[i] = (d == null) ? -1d : d;
      }
      return result;
    } catch (final Exception e) {
      if (debugEnabled) log.debug("error getting attributes '{}': {}", Arrays.toString(attributes), ExceptionUtils.getMessage(e));
    }
    return new double[attributes.length];
  }
}
