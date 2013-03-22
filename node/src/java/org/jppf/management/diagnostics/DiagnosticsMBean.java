/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import javax.management.NotificationEmitter;

/**
 * Interface for the diagnostics MBean.
 * @author Laurent Cohen
 */
public interface DiagnosticsMBean extends NotificationEmitter
{
  /**
   * The name of this mbean in a driver.
   */
  String MBEAN_NAME_DRIVER = "org.jppf:name=diagnostics,type=driver";
  /**
   * The name of this mbean in a node.
   */
  String MBEAN_NAME_NODE = "org.jppf:name=diagnostics,type=node";

  /**
   * Get the diagnostifcs info for the whole JVM.
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
   * Trigger a heap dump of the JVM.
   * @return a message describing the outcome.
   * @throws Exception if any error occurs. 
   */
  String heapDump() throws Exception;
}
