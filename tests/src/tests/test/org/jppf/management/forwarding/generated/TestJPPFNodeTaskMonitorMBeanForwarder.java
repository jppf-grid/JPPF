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

import org.jppf.management.JPPFNodeTaskMonitorMBean;
import org.jppf.management.NodeSelector;
import org.jppf.management.forwarding.generated.JPPFNodeTaskMonitorMBeanForwarder;
import org.jppf.utils.ResultsMap;
import org.junit.Before;
import org.junit.Test;
import test.org.jppf.management.forwarding.AbstractTestForwarderProxy;

/**
 * Test of the forwarding proxy for the {@link JPPFNodeTaskMonitorMBean} MBean.
 * MBean description: monitoring of the tasks processing in a node.
 * @since 6.2
 */
public class TestJPPFNodeTaskMonitorMBeanForwarder extends AbstractTestForwarderProxy {
  /**
   * Reference to the forwarding proxy.
   */
  private JPPFNodeTaskMonitorMBeanForwarder proxy;

  /**
   * Initial setup.
   * @throws Exception if any error occurs.
   */
  @Before
  public void setupInstance() throws Exception {
    if (proxy == null) proxy = (JPPFNodeTaskMonitorMBeanForwarder) getForwardingProxy(JPPFNodeTaskMonitorMBean.class);
  }

  /**
   * Test getting the value of the {@code TotalTasksSucessfull} attribute for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testgetTotalTasksSucessfull() throws Exception {
    final ResultsMap<String, Integer> results = proxy.getTotalTasksSucessfull(NodeSelector.ALL_NODES);
    checkResults(results, Integer.class);
  }

  /**
   * Test getting the value of the {@code TotalTaskCpuTime} attribute for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testgetTotalTaskCpuTime() throws Exception {
    final ResultsMap<String, Long> results = proxy.getTotalTaskCpuTime(NodeSelector.ALL_NODES);
    checkResults(results, Long.class);
  }

  /**
   * Test getting the value of the {@code TotalTasksExecuted} attribute for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testgetTotalTasksExecuted() throws Exception {
    final ResultsMap<String, Integer> results = proxy.getTotalTasksExecuted(NodeSelector.ALL_NODES);
    checkResults(results, Integer.class);
  }

  /**
   * Test getting the value of the {@code TotalTasksInError} attribute for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testgetTotalTasksInError() throws Exception {
    final ResultsMap<String, Integer> results = proxy.getTotalTasksInError(NodeSelector.ALL_NODES);
    checkResults(results, Integer.class);
  }

  /**
   * Test getting the value of the {@code TotalTaskElapsedTime} attribute for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testgetTotalTaskElapsedTime() throws Exception {
    final ResultsMap<String, Long> results = proxy.getTotalTaskElapsedTime(NodeSelector.ALL_NODES);
    checkResults(results, Long.class);
  }

  /**
   * Test invoking the {@code reset} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testReset() throws Exception {
    final ResultsMap<String, Void> results = proxy.reset(NodeSelector.ALL_NODES);
    checkResults(results, void.class);
  }
}
