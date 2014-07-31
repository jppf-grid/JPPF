/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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


/**
 * Interface for the diagnostics MBean.
 * @author Laurent Cohen
 */
public interface DiagnosticsMBean
{
  /**
   * The name of this MBean in a driver.
   */
  String MBEAN_NAME_DRIVER = "org.jppf:name=diagnostics,type=driver";
  /**
   * The name of this MBean in a node.
   */
  String MBEAN_NAME_NODE = "org.jppf:name=diagnostics,type=node";

  /**
   * Get the diagnostics info for the whole JVM.
   * @return a {@link MemoryInformation} instance.
   * @throws Exception if any error occurs.
   */
  MemoryInformation memoryInformation() throws Exception;

  /**
   * Get the names of all live threads in the current JVM.
   * @return an arrray of thread names as strings.
   * @throws Exception if any error occurs.
   */
  String[] threadNames() throws Exception;

  /**
   * Perform a garbage collection. This method calls <code>System.gc()</code>.
   * @throws Exception if any error occurs.
   */
  void gc() throws Exception;

  /**
   * Get a full thread dump, including detection of deadlocks.
   * @return a {@link ThreadDump} instance.
   * @throws Exception if any error occurs.
   */
  ThreadDump threadDump() throws Exception;

  /**
   * Determine whether a deadlock is detected in the JVM.
   * @return <code>true</code> if a deadlock is detected, <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  Boolean hasDeadlock() throws Exception;

  /**
   * Get a summarized snapshot of the JVM health.
   * @return a {@link HealthSnapshot} instance.
   * @throws Exception if any error occurs.
   */
  HealthSnapshot healthSnapshot() throws Exception;

  /**
   * Trigger a heap dump of the JVM. This will not work with all JVM implementations.
   * It should work with Oracle standard and JRockit JVMs, along with IBM JVM.
   * @return a message describing the outcome.
   * @throws Exception if any error occurs. 
   */
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
  Double cpuLoad();
}
