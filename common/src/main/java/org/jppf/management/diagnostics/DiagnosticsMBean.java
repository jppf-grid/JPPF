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

import java.util.List;

import org.jppf.management.doc.*;
import org.jppf.utils.configuration.JPPFProperty;

/**
 * Interface for the diagnostics MBean.
 * @author Laurent Cohen
 */
@MBeanDescription("management and monitoring of the JVM health")
public interface DiagnosticsMBean {
  /**
   * The name of this MBean in a driver.
   */
  String MBEAN_NAME_DRIVER = "org.jppf:name=diagnostics,type=driver";
  /**
   * The name of this MBean in a node.
   */
  String MBEAN_NAME_NODE = "org.jppf:name=diagnostics,type=node";

  /**
   * Get the current state of the heap and non-heap memory for the JVM.
   * @return a {@link MemoryInformation} instance.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get the current state of the heap and non-heap memory for the JVM")
  MemoryInformation memoryInformation() throws Exception;

  /**
   * Get the names of all live threads in the current JVM.
   * @return an arrray of thread names as strings.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get the names of all live threads in the current JVM")
  String[] threadNames() throws Exception;

  /**
   * Perform a full garbage collection. This method calls <code>System.gc()</code>.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("perform a full garbage collection by calling System.gc()")
  void gc() throws Exception;

  /**
   * Get a full thread dump, including detection of deadlocks.
   * @return a {@link ThreadDump} instance.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get a full thread dump, including detection of deadlocks")
  ThreadDump threadDump() throws Exception;

  /**
   * Determine whether a deadlock is detected in the JVM.
   * @return <code>true</code> if a deadlock is detected, <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("determine whether a deadlock is detected in the JVM")
  Boolean hasDeadlock() throws Exception;

  /**
   * Get a summarized snapshot of the JVM health.
   * @return a {@link HealthSnapshot} instance.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get a summarized snapshot of the JVM health")
  HealthSnapshot healthSnapshot() throws Exception;

  /**
   * Get a a string representation of a JVM health snapshot. The returned string contains a set of key / value pairs separated by new lines.
   * As such, it can be loaded directly into a {@link java.util.Properties Properties} or {@link org.jppf.utils.TypedProperties TypedProperties}
   * object via their {@code load(Reader)} method, using a {@link java.io.StringReader StringReader}.
   * @return a {@link java.util.Properties Properties}-compatible string representation of a JVM health snapshot.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get a a string representation of a JVM health snapshot. The returned string contains a set of key / value pairs separated by new lines")
  String healthSnapshotAsString() throws Exception;

  /**
   * Trigger a heap dump of the JVM. This will not work with all JVM implementations.
   * It should work with Oracle standard and JRockit JVMs, along with IBM JVM.
   * @return a message describing the outcome.
   * @throws Exception if any error occurs. 
   */
  @MBeanDescription("trigger a heap dump of the JVM")
  String heapDump() throws Exception;

  /**
   * Get an approximation of the current CPU load. The computed value is equal to
   * <code>sum<sub>i</sub>(thread_used_cpu<sub>i</sub>) / interval</code>, for all the
   * live threads of the JVM at the time of the computation. Thus, errors may occur,
   * since many threads may have been created then died between two computations.
   * However, in most cases this is a reasonable approximation, whose computation does not
   * tax the CPU too heavily.
   * @return the cpu load as a double value in the range <code>[0, 1]</code> (ratio of <code>totalCpuTime / computationInterval</code>),
   * or -1d if CPU time measurement is not available for the JVM.
   */
  @MBeanDescription("get an approximation of the current CPU load")
  Double cpuLoad();

  /**
   * Get the list of properties available as monitoring data.
   * @return a list of the properties described as instances of {@link JPPFProperty}.
   */
  @MBeanDescription("The list of monitoring data properties available in the snapshots")
  @MBeanElementType(type = List.class, parameters = { "org.jppf.utils.configuration.JPPFProperty<?>" })
  List<JPPFProperty<?>> getMonitoringDataProperties();
}
