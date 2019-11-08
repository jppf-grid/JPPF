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

package test.org.jppf.management.forwarding.generated;

import org.jppf.management.NodeSelector;
import org.jppf.management.diagnostics.DiagnosticsMBean;
import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.management.diagnostics.MemoryInformation;
import org.jppf.management.diagnostics.ThreadDump;
import org.jppf.management.forwarding.generated.DiagnosticsMBeanForwarder;
import org.jppf.utils.ResultsMap;
import org.junit.Before;
import org.junit.Test;
import test.org.jppf.management.forwarding.AbstractTestForwarderProxy;

/**
 * Test of the forwarding proxy for the {@link DiagnosticsMBean} MBean.
 * MBean description: management and monitoring of the JVM health.
 * @since 6.2
 */
public class TestDiagnosticsMBeanForwarder extends AbstractTestForwarderProxy {
  /**
   * Reference to the forwarding proxy.
   */
  private DiagnosticsMBeanForwarder proxy;

  /**
   * Initial setup.
   * @throws Exception if any error occurs.
   */
  @Before
  public void setupInstance() throws Exception {
    if (proxy == null) proxy = (DiagnosticsMBeanForwarder) getForwardingProxy(DiagnosticsMBean.class);
  }

  /**
   * Test invoking the {@code gc} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testGc() throws Exception {
    final ResultsMap<String, Void> results = proxy.gc(NodeSelector.ALL_NODES);
    checkResults(results, void.class);
  }

  /**
   * Test invoking the {@code healthSnapshotAsString} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testHealthSnapshotAsString() throws Exception {
    final ResultsMap<String, String> results = proxy.healthSnapshotAsString(NodeSelector.ALL_NODES);
    checkResults(results, String.class);
  }

  /**
   * Test invoking the {@code memoryInformation} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testMemoryInformation() throws Exception {
    final ResultsMap<String, MemoryInformation> results = proxy.memoryInformation(NodeSelector.ALL_NODES);
    checkResults(results, MemoryInformation.class);
  }

  /**
   * Test invoking the {@code threadNames} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testThreadNames() throws Exception {
    final ResultsMap<String, String[]> results = proxy.threadNames(NodeSelector.ALL_NODES);
    checkResults(results, String[].class);
  }

  /**
   * Test invoking the {@code threadDump} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testThreadDump() throws Exception {
    final ResultsMap<String, ThreadDump> results = proxy.threadDump(NodeSelector.ALL_NODES);
    checkResults(results, ThreadDump.class);
  }

  /**
   * Test invoking the {@code hasDeadlock} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testHasDeadlock() throws Exception {
    final ResultsMap<String, Boolean> results = proxy.hasDeadlock(NodeSelector.ALL_NODES);
    checkResults(results, Boolean.class);
  }

  /**
   * Test invoking the {@code healthSnapshot} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testHealthSnapshot() throws Exception {
    final ResultsMap<String, HealthSnapshot> results = proxy.healthSnapshot(NodeSelector.ALL_NODES);
    checkResults(results, HealthSnapshot.class);
  }

  /**
   * Test invoking the {@code cpuLoad} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testCpuLoad() throws Exception {
    final ResultsMap<String, Double> results = proxy.cpuLoad(NodeSelector.ALL_NODES);
    checkResults(results, Double.class);
  }
}
