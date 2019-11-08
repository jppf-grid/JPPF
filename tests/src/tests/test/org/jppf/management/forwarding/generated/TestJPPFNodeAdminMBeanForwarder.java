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

import java.util.Map;
import org.jppf.classloader.DelegationModel;
import org.jppf.management.JPPFNodeAdminMBean;
import org.jppf.management.JPPFNodeState;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.management.NodePendingAction;
import org.jppf.management.NodeSelector;
import org.jppf.management.forwarding.generated.JPPFNodeAdminMBeanForwarder;
import org.jppf.utils.ResultsMap;
import org.junit.Before;
import org.junit.Test;
import test.org.jppf.management.forwarding.AbstractTestForwarderProxy;

/**
 * Test of the forwarding proxy for the {@link JPPFNodeAdminMBean} MBean.
 * MBean description: management and monitoring of a JPPF node.
 * @since 6.2
 */
public class TestJPPFNodeAdminMBeanForwarder extends AbstractTestForwarderProxy {
  /**
   * Reference to the forwarding proxy.
   */
  private JPPFNodeAdminMBeanForwarder proxy;

  /**
   * Initial setup.
   * @throws Exception if any error occurs.
   */
  @Before
  public void setupInstance() throws Exception {
    if (proxy == null) proxy = (JPPFNodeAdminMBeanForwarder) getForwardingProxy(JPPFNodeAdminMBean.class);
  }

  /**
   * Test getting the value of the {@code DelegationModel} attribute for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testgetDelegationModel() throws Exception {
    final ResultsMap<String, DelegationModel> results = proxy.getDelegationModel(NodeSelector.ALL_NODES);
    checkResults(results, DelegationModel.class);
  }

  /**
   * Test setting the value of the {@code DelegationModel} attribute on all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testSetDelegationModel() throws Exception {
    // nothing yet
    final ResultsMap<String, Void> results = proxy.setDelegationModel(NodeSelector.ALL_NODES, DelegationModel.PARENT_FIRST);
    checkResults(results, DelegationModel.class);
  }

  /**
   * Test setting the value of the {@code TaskCounter} attribute on all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testSetTaskCounter() throws Exception {
    // nothing yet
    final ResultsMap<String, Void> results = proxy.setTaskCounter(NodeSelector.ALL_NODES, Integer.valueOf(0));
    checkResults(results, Integer.class);
  }

  /**
   * Test invoking the {@code state} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testState() throws Exception {
    final ResultsMap<String, JPPFNodeState> results = proxy.state(NodeSelector.ALL_NODES);
    checkResults(results, JPPFNodeState.class);
  }

  /**
   * Test invoking the {@code cancelJob} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testCancelJob() throws Exception {
    final ResultsMap<String, Void> results = proxy.cancelJob(NodeSelector.ALL_NODES, "some_string", Boolean.FALSE);
    checkResults(results, void.class);
  }

  /**
   * Test invoking the {@code updateThreadsPriority} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testUpdateThreadsPriority() throws Exception {
    final ResultsMap<String, Void> results = proxy.updateThreadsPriority(NodeSelector.ALL_NODES, Integer.valueOf(0));
    checkResults(results, void.class);
  }

  /**
   * Test invoking the {@code updateThreadPoolSize} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testUpdateThreadPoolSize() throws Exception {
    final ResultsMap<String, Void> results = proxy.updateThreadPoolSize(NodeSelector.ALL_NODES, Integer.valueOf(0));
    checkResults(results, void.class);
  }

  /**
   * Test invoking the {@code updateConfiguration} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testUpdateConfiguration() throws Exception {
    final ResultsMap<String, Void> results = proxy.updateConfiguration(NodeSelector.ALL_NODES, (Map<Object, Object>) null, Boolean.FALSE);
    checkResults(results, void.class);
  }

  /**
   * Test invoking the {@code updateConfiguration} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testUpdateConfiguration2() throws Exception {
    final ResultsMap<String, Void> results = proxy.updateConfiguration(NodeSelector.ALL_NODES, (Map<Object, Object>) null, Boolean.FALSE, Boolean.FALSE);
    checkResults(results, void.class);
  }

  /**
   * Test invoking the {@code cancelPendingAction} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testCancelPendingAction() throws Exception {
    final ResultsMap<String, Boolean> results = proxy.cancelPendingAction(NodeSelector.ALL_NODES);
    checkResults(results, boolean.class);
  }

  /**
   * Test invoking the {@code pendingAction} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testPendingAction() throws Exception {
    final ResultsMap<String, NodePendingAction> results = proxy.pendingAction(NodeSelector.ALL_NODES);
    checkResults(results, NodePendingAction.class);
  }

  /**
   * Test invoking the {@code resetTaskCounter} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testResetTaskCounter() throws Exception {
    final ResultsMap<String, Void> results = proxy.resetTaskCounter(NodeSelector.ALL_NODES);
    checkResults(results, void.class);
  }

  /**
   * Test invoking the {@code systemInformation} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testSystemInformation() throws Exception {
    final ResultsMap<String, JPPFSystemInformation> results = proxy.systemInformation(NodeSelector.ALL_NODES);
    checkResults(results, JPPFSystemInformation.class);
  }
}
