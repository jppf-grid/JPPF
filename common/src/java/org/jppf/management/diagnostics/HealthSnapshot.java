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
   * 
   */
  private final TypedProperties properties = new TypedProperties();
  /**
   * Ratio of used / max for heap memory.
   */
  //double heapUsedRatio = -1d;
  /**
   * Used heap memory in bytes.
   */
  //long heapUsed = -1L;
  /**
   * Ratio of used / max for non-heap memory.
   */
  //double nonheapUsedRatio = -1d;
  /**
   * Used non-heap memory in bytes.
   */
  //long nonheapUsed = -1L;
  /**
   * Determines whether a deadlock was detected.
   */
  //boolean deadlocked = false;
  /**
   * Number of live threads in the JVM.
   */
  //int liveThreads = -1;
  /**
   * The process cpu load.
   */
  //double processCpuLoad = -1d;
  /**
   * The system cpu load.
   */
  //double systemCpuLoad = -1d;
  /**
   * Ratio of used / max for physical memory.
   */
  //double ramUsedRatio = -1d;
  /**
   * Used physical memory in bytes.
   */
  //long ramUsed = -1L;

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
   * @deprecated use {@code getDouble("heapUsedRatio")} instead.
   */
  public double getHeapUsedRatio() {
    return properties.getDouble("heapUsedRatio");
  }

  /**
   * Get the ratio of used / max for non-heap memory.
   * @return the ratio as a double value in the range [0, 1].
   * @deprecated use {@code getDouble("nonheapUsedRatio")} instead.
   */
  public double getNonheapUsedRatio() {
    return properties.getDouble("nonheapUsedRatio");
  }

  /**
   * Determine whether a deadlock was detected.
   * @return {@code true} if a deadlock was dertected, <code>false</code> otherwise.
   * @deprecated use {@code getBoolean("deadlocked")} instead.
   */
  public boolean isDeadlocked() {
    return properties.getBoolean("deadlocked");
  }

  /**
   * Get the used heap memory in bytes.
   * @return the heap used as a long.
   * @deprecated use {@code getLong("heapUsed")} instead.
   */
  public long getHeapUsed() {
    return properties.getLong("heapUsed");
  }

  /**
   * Get the used non-heap memory in bytes.
   * @return the non-heap used as a long.
   * @deprecated use {@code getLong("nonheapUsed")} instead.
   */
  public long getNonheapUsed() {
    return properties.getLong("nonheapUsed");
  }

  /**
   * Get the number of live threads in the JVM.
   * @return the number of threads as an int.
   * @deprecated use {@code getInt("liveThreads")} instead.
   */
  public int getLiveThreads() {
    return properties.getInt("liveThreads");
  }

  /**
   * Get the cpu load of the current process.
   * @return the cpu load as a double in the range {@code [0 ... 1]}, or {@code -1d} if it is unknown.
   * @deprecated use {@code getDouble("processCpuLoad")} instead.
   */
  public double getCpuLoad() {
    return properties.getDouble("processCpuLoad");
  }

  /**
   * Get the cpu load of the system.
   * @return the cpu load as a double in the range {@code [0 ... 1]}, or {@code -1d} if it is unknown.
   * @deprecated use {@code getDouble("systemCpuLoad")} instead.
   */
  public double getSystemCpuLoad() {
    return properties.getDouble("systemCpuLoad");
  }

  /**
   * Get the ratio of used / max for physical memory.
   * @return the percentage of used RAM in the range {@code [0 ... 1]}, or {@code -1d} if it is unknown.
   * @deprecated use {@code getDouble("ramUsedRatio")} instead.
   */
  public double getRamUsedRatio() {
    return properties.getDouble("ramUsedRatio");
  }

  /**
   * Get the amount of used physical memory in bytes.
   * @return the amount of used RAM in bytes, or {@code -1L} if it is unknown.
   * @deprecated use {@code getLong("ramUsed")} instead.
   */
  public long getRamUsed() {
    return properties.getLong("ramUsed");
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
