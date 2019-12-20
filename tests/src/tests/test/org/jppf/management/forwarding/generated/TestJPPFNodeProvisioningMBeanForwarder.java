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
import org.jppf.management.forwarding.generated.JPPFNodeProvisioningMBeanForwarder;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.utils.ResultsMap;
import org.jppf.utils.TypedProperties;
import org.junit.Before;
import org.junit.Test;
import test.org.jppf.management.forwarding.AbstractTestForwarderProxy;

/**
 * Test of the forwarding proxy for the {@link JPPFNodeProvisioningMBean} MBean.
 * MBean description: interface for provisioning, managing and monitoring slave nodes.
 * @since 6.2
 */
public class TestJPPFNodeProvisioningMBeanForwarder extends AbstractTestForwarderProxy {
  /**
   * Reference to the forwarding proxy.
   */
  private JPPFNodeProvisioningMBeanForwarder proxy;

  /**
   * Initial setup.
   * @throws Exception if any error occurs.
   */
  @Before
  public void setupInstance() throws Exception {
    if (proxy == null) proxy = (JPPFNodeProvisioningMBeanForwarder) getForwardingProxy(JPPFNodeProvisioningMBean.class);
  }

  /**
   * Test getting the value of the {@code NbSlaves} attribute for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testgetNbSlaves() throws Exception {
    final ResultsMap<String, Integer> results = proxy.getNbSlaves(NodeSelector.ALL_NODES);
    checkResults(results, int.class);
  }

  /**
   * Test invoking the {@code provisionSlaveNodes} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testProvisionSlaveNodes() throws Exception {
    final ResultsMap<String, Void> results = proxy.provisionSlaveNodes(NodeSelector.ALL_NODES, 0, (TypedProperties) null);
    checkResults(results, void.class);
  }

  /**
   * Test invoking the {@code provisionSlaveNodes} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testProvisionSlaveNodes2() throws Exception {
    final ResultsMap<String, Void> results = proxy.provisionSlaveNodes(NodeSelector.ALL_NODES, 0, false, (TypedProperties) null);
    checkResults(results, void.class);
  }

  /**
   * Test invoking the {@code provisionSlaveNodes} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testProvisionSlaveNodes3() throws Exception {
    final ResultsMap<String, Void> results = proxy.provisionSlaveNodes(NodeSelector.ALL_NODES, 0);
    checkResults(results, void.class);
  }

  /**
   * Test invoking the {@code provisionSlaveNodes} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testProvisionSlaveNodes4() throws Exception {
    final ResultsMap<String, Void> results = proxy.provisionSlaveNodes(NodeSelector.ALL_NODES, 0, false);
    checkResults(results, void.class);
  }
}
