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

/**
 * ENumeration of the built-in monitoring data fields.
 * @author Laurent Cohen
 */
public class MonitoringConstants {
  /**
   * Convenience constants.
   */
  static final long KB = 1024L, MB = KB * 1024L;
  /**
   * Constant for the name of the heap usage ratio property.
   */
  public static final String HEAP_USAGE_RATIO = "heapUsedRatio";
  /**
   * Constant for the name of the heap usage in MB property.
   */
  public static final String HEAP_USAGE_MB = "heapUsed";
  /**
   * Constant for the name of the non-heap usage ratio property.
   */
  public static final String NON_HEAP_USAGE_RATIO = "nonheapUsedRatio";
  /**
   * Constant for the name of the non-heap usage in MB property.
   */
  public static final String NON_HEAP_USAGE_MB = "nonheapUsed";
  /**
   * Constant for the name of the deadlock indicator property.
   */
  public static final String DEADLOCKED = "deadlocked";
  /**
   * Constant for the name of the live threads count property.
   */
  public static final String LIVE_THREADS_COUNT = "liveThreads";
  /**
   * Constant for the name of the current process cpu load in % property.
   */
  public static final String PROCESS_CPU_LOAD = "processCpuLoad";
  /**
   * Constant for the name of the system cpu load in % property.
   */
  public static final String SYSTEM_CPU_LOAD = "systemCpuLoad";
  /**
   * Constant for the name of the ram usage ratio property.
   */
  public static final String RAM_USAGE_RATIO = "ramUsedRatio";
  /**
   * Constant for the name of the ram usage in MB property.
   */
  public static final String RAM_USAGE_MB = "ramUsed";
  /**
   * Constant for the name of the swap memory usage ratio property.
   */
  public static final String SWAP_USAGE_RATIO = "swapUsedRatio";
  /**
   * Constant for the name of the swap memory usage in MB property.
   */
  public static final String SWAP_USAGE_MB = "swapUsed";
  /**
   * Constant for the name of the cpu temperature property.
   */
  public static final String CPU_TEMPERATURE = "cpuTemperature";
  /**
   * Constant for the name of the os name and version property.
   */
  public static final String OS_NAME = "osName";
  /**
   * Constant for the name of the process resident size property.
   */
  public static final String PROCESS_RESIDENT_SET_SIZE = "rss";
  /**
   * Constant for the name of the process virtual size property.
   */
  public static final String PROCESS_VIRTUAL_SIZE = "vsz";
  
}
