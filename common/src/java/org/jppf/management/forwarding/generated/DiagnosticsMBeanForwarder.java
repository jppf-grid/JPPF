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

package org.jppf.management.forwarding.generated;

import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.NodeSelector;
import org.jppf.management.diagnostics.DiagnosticsMBean;
import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.management.diagnostics.MemoryInformation;
import org.jppf.management.diagnostics.ThreadDump;
import org.jppf.management.forwarding.AbstractNodeForwardingProxy;
import org.jppf.utils.ResultsMap;

/**
 * Forwarding proxy for the {@link DiagnosticsMBean} MBean.
 * MBean description: management and monitoring of the JVM health.
 * @since 6.2
 */
public class DiagnosticsMBeanForwarder extends AbstractNodeForwardingProxy {
  /**
   * Initialize this proxy.
   * @param jmx a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs..
   */
  public DiagnosticsMBeanForwarder(final JMXDriverConnectionWrapper jmx) throws Exception {
    super(jmx, "org.jppf:name=diagnostics,type=node");
  }

  /**
   * Invoke the {@code gc} operation for all selected nodes (perform a full garbage collection by calling System.gc()).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> gc(final NodeSelector selector) throws Exception {
    return invoke(selector, "gc");
  }

  /**
   * Invoke the {@code healthSnapshotAsString} operation for all selected nodes (get a a string representation of a JVM health snapshot. The returned string contains a set of key / value pairs separated by new lines).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either a {@link String} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, String> healthSnapshotAsString(final NodeSelector selector) throws Exception {
    return invoke(selector, "healthSnapshotAsString");
  }

  /**
   * Invoke the {@code memoryInformation} operation for all selected nodes (get the current state of the heap and non-heap memory for the JVM).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either a {@link MemoryInformation} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, MemoryInformation> memoryInformation(final NodeSelector selector) throws Exception {
    return invoke(selector, "memoryInformation");
  }

  /**
   * Invoke the {@code threadNames} operation for all selected nodes (get the names of all live threads in the current JVM).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either a {@link String[]} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, String[]> threadNames(final NodeSelector selector) throws Exception {
    return invoke(selector, "threadNames");
  }

  /**
   * Invoke the {@code threadDump} operation for all selected nodes (get a full thread dump, including detection of deadlocks).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either a {@link ThreadDump} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, ThreadDump> threadDump(final NodeSelector selector) throws Exception {
    return invoke(selector, "threadDump");
  }

  /**
   * Invoke the {@code hasDeadlock} operation for all selected nodes (determine whether a deadlock is detected in the JVM).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either a {@link Boolean} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Boolean> hasDeadlock(final NodeSelector selector) throws Exception {
    return invoke(selector, "hasDeadlock");
  }

  /**
   * Invoke the {@code healthSnapshot} operation for all selected nodes (get a summarized snapshot of the JVM health).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either a {@link HealthSnapshot} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, HealthSnapshot> healthSnapshot(final NodeSelector selector) throws Exception {
    return invoke(selector, "healthSnapshot");
  }

  /**
   * Invoke the {@code heapDump} operation for all selected nodes (trigger a heap dump of the JVM).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either a {@link String} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, String> heapDump(final NodeSelector selector) throws Exception {
    return invoke(selector, "heapDump");
  }

  /**
   * Invoke the {@code cpuLoad} operation for all selected nodes (get an approximation of the current CPU load).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either a {@link Double} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Double> cpuLoad(final NodeSelector selector) throws Exception {
    return invoke(selector, "cpuLoad");
  }
}
