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

import java.io.Serializable;
import java.util.Map;

import org.jppf.management.diagnostics.provider.MonitoringConstants;
import org.jppf.utils.TypedProperties;

/**
 * This class represents a snapshot of the JVM health.
 * @author Laurent Cohen
 */
public class HealthSnapshot implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Contains the attributes of this snapshot with their values.
   */
  private final TypedProperties properties = new TypedProperties();

  /**
   * Initialize this snapshot with default values.
   */
  public HealthSnapshot() {
  }

  /**
   * Add or merge the specified properties ot the existing ones.
   * @param properties the proerties to add.
   */
  void putProperties(final TypedProperties properties) {
    this.properties.putAll(properties);
  }

  /**
   * Get the value of a property as an int.
   * @param name the name of the property to lookup.
   * @return the value of the property.
   */
  public int getInt(final String name) {
    return properties.getInt(name);
  }

  /**
   * Get the value of a property as a long.
   * @param name the name of the property to lookup.
   * @return the value of the property.
   */
  public long getLong(final String name) {
    return properties.getLong(name);
  }

  /**
   * Get the value of a property as a float.
   * @param name the name of the property to lookup.
   * @return the value of the property.
   */
  public float getFloat(final String name) {
    return properties.getFloat(name);
  }

  /**
   * Get the value of a property as a double.
   * @param name the name of the property to lookup.
   * @return the value of the property.
   */
  public double getDouble(final String name) {
    return properties.getDouble(name);
  }

  /**
   * Get the value of a property as a boolean.
   * @param name the name of the property to lookup.
   * @return the value of the property.
   */
  public boolean getBoolean(final String name) {
    return properties.getBoolean(name);
  }

  /**
   * Get the value of a property as a String.
   * @param name the name of the property to lookup.
   * @return the value of the property.
   */
  public String getString(final String name) {
    return properties.getString(name);
  }

  /**
   * Get the ratio of used / max for heap memory.
   * @return the ratio as a double value in the range [0, 1].
   * @deprecated use {@code getDouble(DefaultMonitoringDataProvider.HEAP_USAGE_RATIO)} instead.
   */
  public double getHeapUsedRatio() {
    return properties.getDouble(MonitoringConstants.HEAP_USAGE_RATIO);
  }

  /**
   * Get the ratio of used / max for non-heap memory.
   * @return the ratio as a double value in the range [0, 1].
   * @deprecated use {@code getDouble(DefaultMonitoringDataProvider.NON_HEAP_USAGE_RATIO)} instead.
   */
  public double getNonheapUsedRatio() {
    return properties.getDouble(MonitoringConstants.NON_HEAP_USAGE_RATIO);
  }

  /**
   * Determine whether a deadlock was detected.
   * @return {@code true} if a deadlock was dertected, {@code false} otherwise.
   * @deprecated use {@code getBoolean(DefaultMonitoringDataProvider.DEADLOCKED)} instead.
   */
  public boolean isDeadlocked() {
    return properties.getBoolean(MonitoringConstants.DEADLOCKED);
  }

  /**
   * Get the used heap memory in bytes.
   * @return the heap used as a long.
   * @deprecated use {@code getLong(DefaultMonitoringDataProvider.HEAP_USAGE_MB)} instead.
   */
  public long getHeapUsed() {
    return properties.getLong(MonitoringConstants.HEAP_USAGE_MB);
  }

  /**
   * Get the used non-heap memory in bytes.
   * @return the non-heap used as a long.
   * @deprecated use {@code getLong(DefaultMonitoringDataProvider.NON_HEAP_USAGE_MB)} instead.
   */
  public long getNonheapUsed() {
    return properties.getLong(MonitoringConstants.NON_HEAP_USAGE_MB);
  }

  /**
   * Get the number of live threads in the JVM.
   * @return the number of threads as an int.
   * @deprecated use {@code getInt(DefaultMonitoringDataProvider.LIVE_THREADS_COUNT)} instead.
   */
  public int getLiveThreads() {
    return properties.getInt(MonitoringConstants.LIVE_THREADS_COUNT);
  }

  /**
   * Get the cpu load of the current process.
   * @return the cpu load as a double in the range {@code [0 ... 1]}, or {@code -1d} if it is unknown.
   * @deprecated use {@code getDouble(DefaultMonitoringDataProvider.PROCESS_CPU_LOAD)} instead.
   */
  public double getCpuLoad() {
    return properties.getDouble(MonitoringConstants.PROCESS_CPU_LOAD);
  }

  /**
   * Get the cpu load of the system.
   * @return the cpu load as a double in the range {@code [0 ... 1]}, or {@code -1d} if it is unknown.
   * @deprecated use {@code getDouble(DefaultMonitoringDataProvider.SYSTEM_CPU_LOAD)} instead.
   */
  public double getSystemCpuLoad() {
    return properties.getDouble(MonitoringConstants.SYSTEM_CPU_LOAD);
  }

  /**
   * Get the ratio of used / max for physical memory.
   * @return the percentage of used RAM in the range {@code [0 ... 1]}, or {@code -1d} if it is unknown.
   * @deprecated use {@code getDouble(DefaultMonitoringDataProvider.RAM_USAGE_RATIO)} instead.
   */
  public double getRamUsedRatio() {
    return properties.getDouble(MonitoringConstants.RAM_USAGE_RATIO);
  }

  /**
   * Get the amount of used physical memory in bytes.
   * @return the amount of used RAM in bytes, or {@code -1L} if it is unknown.
   * @deprecated use {@code getLong(DefaultMonitoringDataProvider.RAM_USAGE_MB)} instead.
   */
  public long getRamUsed() {
    return properties.getLong(MonitoringConstants.RAM_USAGE_MB);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    int count = 0;
    for (final Map.Entry<Object, Object> entry: properties.entrySet()) {
      if ((entry.getKey() instanceof String) && (entry.getKey() instanceof String)) {
        final String key = (String) entry.getKey(), value = (String) entry.getValue();
        if (count > 0) sb.append(", ");
        sb.append(key).append('=').append(value);
        count++;
      }
    }
    return sb.append(']').toString();
  }
}
