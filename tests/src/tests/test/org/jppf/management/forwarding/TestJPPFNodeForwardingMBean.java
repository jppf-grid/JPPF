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

package test.org.jppf.management.forwarding;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.JPPFException;
import org.jppf.classloader.DelegationModel;
import org.jppf.client.JPPFJob;
import org.jppf.management.*;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.Test;

import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link JPPFDriverAdminMBean}.
 * In this class, we test that the functionality of the DriverJobManagementMBean from the client point of view.
 * @author Laurent Cohen
 */
public class TestJPPFNodeForwardingMBean extends AbstractTestJPPFNodeForwardingMBean {
  /** */
  private final Map<NodeSelector, String[]> selectorMap = new LinkedHashMap<NodeSelector, String[]>() {{
    put(new AllNodesSelector(), new String[] {"n1", "n2"});
    put(new ExecutionPolicySelector(new Equal("jppf.node.uuid", false, "n1")), new String[] {"n1"});
    put(new UuidSelector("n2"), new String[] {"n2"});
    put(new ScriptedNodeSelector("javascript", "\"n1\".equals(nodeInfo.getSystemInfo().getJppf().getString(\"jppf.node.uuid\"))"), new String[] {"n1"});
  }};

  /**
   * Test getting the node state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000)
  public void testState() throws Exception {
    for (final Map.Entry<NodeSelector, String[]> entry: selectorMap.entrySet()) testState(entry.getKey(), entry.getValue());
  }

  /**
   * Execute the tests with the specified node selector.
   * @param selector the selector to apply.
   * @param expectedNodes the set of nodes the selector is expected to resolve to.
   * @throws Exception if any error occurs
   */
  private static void testState(final NodeSelector selector, final String... expectedNodes) throws Exception {
    final String selectorClass = selector.getClass().getSimpleName();
    BaseTestHelper.printToAll(driverJmx, true, true, true, true, false, ">>> testing with %s, expectedNodes=%s", selectorClass, Arrays.toString(expectedNodes));
    final int nbNodes = expectedNodes.length;
    try {
      configureLoadBalancer();
      Map<String, Object> result = nodeForwarder.state(selector);
      checkNodes(result, JPPFNodeState.class, expectedNodes);
      for (final Map.Entry<String, Object> entry : result.entrySet()) {
        final JPPFNodeState state = (JPPFNodeState) entry.getValue();
        assertEquals(1, state.getThreadPoolSize());
        assertEquals(Thread.NORM_PRIORITY, state.getThreadPriority());
        assertEquals(JPPFNodeState.ConnectionState.CONNECTED, state.getConnectionStatus());
        assertEquals(JPPFNodeState.ExecutionState.IDLE, state.getExecutionStatus());
      }
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + "-" + selectorClass, false, nbNodes, LifeCycleTask.class, 2000L);
      job.getSLA().setExecutionPolicy(new OneOf("jppf.node.uuid", false, expectedNodes));
      client.submitAsync(job);
      Thread.sleep(750L);
      result = nodeForwarder.state(selector);
      checkNodes(result, JPPFNodeState.class, expectedNodes);
      for (final Map.Entry<String, Object> entry : result.entrySet()) {
        final JPPFNodeState state = (JPPFNodeState) entry.getValue();
        assertEquals(0, state.getNbTasksExecuted());
        assertEquals(JPPFNodeState.ExecutionState.EXECUTING, state.getExecutionStatus());
      }
      job.awaitResults();
      result = nodeForwarder.state(selector);
      checkNodes(result, JPPFNodeState.class, expectedNodes);
      for (final Map.Entry<String, Object> entry : result.entrySet()) {
        final JPPFNodeState state = (JPPFNodeState) entry.getValue();
        assertEquals(1, state.getNbTasksExecuted());
        assertEquals(JPPFNodeState.ExecutionState.IDLE, state.getExecutionStatus());
      }
    } finally {
      nodeForwarder.resetTaskCounter(selector);
      resetLoadBalancer();
    }
  }

  /**
   * Test updating the number of processing threads.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testUpdateThreadPoolSize() throws Exception {
    try {
      for (final Map.Entry<NodeSelector, String[]> entry: selectorMap.entrySet()) testUpdateThreadPoolSize(entry.getKey(), entry.getValue());
    } catch(final Exception e) {
      throw new JPPFException(e);
    }
  }

  /**
   * Execute the tests with the specified node selector.
   * @param selector the selector to apply.
   * @param expectedNodes the set of nodes the selector is expected to resolve to.
   * @throws Exception if any error occurs.
   */
  private static void testUpdateThreadPoolSize(final NodeSelector selector, final String... expectedNodes) throws Exception {
    final String methodName = ReflectionUtils.getCurrentMethodName();
    BaseTestHelper.printToAll(client, true, true, true, true, false, ">>> start of %s(%s)", methodName, selector.getClass().getSimpleName());
    checkNodes(nodeForwarder.state(selector), JPPFNodeState.class, state -> state.getThreadPoolSize() == 1, expectedNodes);
    BaseTestHelper.printToAll(client, true, true, true, true, false, ">>> setting threadPoolSize to 3");
    checkNoException(nodeForwarder.updateThreadPoolSize(selector, 3), expectedNodes);
    checkNodes(nodeForwarder.state(selector), JPPFNodeState.class, state -> state.getThreadPoolSize() == 3, expectedNodes);
    BaseTestHelper.printToAll(client, true, true, true, true, false, ">>> setting threadPoolSize back to 1");
    checkNoException(nodeForwarder.updateThreadPoolSize(selector, 1), expectedNodes);
    checkNodes(nodeForwarder.state(selector), JPPFNodeState.class, state -> state.getThreadPoolSize() == 1, expectedNodes);
    BaseTestHelper.printToAll(client, true, true, true, true, false, "<<< end of %s(%s)", methodName, selector.getClass().getSimpleName());
  }

  /**
   * Test updating the priority of processing threads.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testUpdateThreadPriority() throws Exception {
    for (final Map.Entry<NodeSelector, String[]> entry: selectorMap.entrySet()) testUpdateThreadPriority(entry.getKey(), entry.getValue());
  }

  /**
   * Execute the tests with the specified node selector.
   * @param selector the selector to apply.
   * @param expectedNodes the set of nodes the selector is expected to resolve to.
   * @throws Exception if any error occurs.
   */
  private static void testUpdateThreadPriority(final NodeSelector selector, final String... expectedNodes) throws Exception {
    final String selectorClass = selector.getClass().getSimpleName();
    BaseTestHelper.printToAll(driverJmx, true, true, true, true, false, ">>> testing with %s, expectedNodes=%s", selectorClass, Arrays.toString(expectedNodes));
    try {
      checkNodes(nodeForwarder.state(selector), JPPFNodeState.class, state -> state.getThreadPriority() == Thread.NORM_PRIORITY, expectedNodes);
      checkNoException(nodeForwarder.updateThreadsPriority(selector, Thread.MAX_PRIORITY), expectedNodes);
      checkNodes(nodeForwarder.state(selector), JPPFNodeState.class, state -> state.getThreadPriority() == Thread.MAX_PRIORITY, expectedNodes);
    } finally {
      checkNoException(nodeForwarder.updateThreadsPriority(selector, Thread.NORM_PRIORITY), expectedNodes);
      checkNodes(nodeForwarder.state(selector), JPPFNodeState.class, state -> state.getThreadPriority() == Thread.NORM_PRIORITY, expectedNodes);
    }
  }

  /**
   * Test resetting the tasks counter.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testResetTaskCounter() throws Exception {
    for (final Map.Entry<NodeSelector, String[]> entry: selectorMap.entrySet()) testResetTaskCounter(entry.getKey(), entry.getValue());
  }

  /**
   * Execute the tests with the specified node selector.
   * @param selector the selector to apply.
   * @param expectedNodes the set of nodes the selector is expected to resolve to.
   * @throws Exception if any error occurs.
   */
  private static void testResetTaskCounter(final NodeSelector selector, final String... expectedNodes) throws Exception {
    final String selectorClass = selector.getClass().getSimpleName();
    BaseTestHelper.printToAll(driverJmx, true, true, true, true, false, ">>> testing with %s, expectedNodes=%s", selectorClass, Arrays.toString(expectedNodes));
    final int nbNodes = expectedNodes.length;
    final int nbTasks = 5 * nbNodes;
    checkNodes(nodeForwarder.state(selector), JPPFNodeState.class, state -> state.getNbTasksExecuted() == 0, expectedNodes);
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + " - " + selector, false, nbTasks, LifeCycleTask.class, 1L);
    job.getSLA().setExecutionPolicy(new OneOf("jppf.node.uuid", false, expectedNodes));
    client.submit(job);
    checkNodes(nodeForwarder.state(selector), JPPFNodeState.class, state -> state.getNbTasksExecuted() == 5, expectedNodes);
    checkNoException(nodeForwarder.resetTaskCounter(selector), expectedNodes);
    checkNodes(nodeForwarder.state(selector), JPPFNodeState.class, state -> state.getNbTasksExecuted() == 0, expectedNodes);
  }

  /**
   * Test setting the tasks counter to a given value.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testSetTaskCounter() throws Exception {
    for (final Map.Entry<NodeSelector, String[]> entry: selectorMap.entrySet()) testSetTaskCounter(entry.getKey(), entry.getValue());
  }

  /**
   * Execute the tests with the specified node selector.
   * @param selector the selector to apply.
   * @param expectedNodes the set of nodes the selector is expected to resolve to.
   * @throws Exception if any error occurs.
   */
  private static void testSetTaskCounter(final NodeSelector selector, final String... expectedNodes) throws Exception {
    final String selectorClass = selector.getClass().getSimpleName();
    BaseTestHelper.printToAll(driverJmx, true, true, true, true, false, ">>> testing with %s, expectedNodes=%s", selectorClass, Arrays.toString(expectedNodes));
    checkNodes(nodeForwarder.state(selector), JPPFNodeState.class, state -> state.getNbTasksExecuted() == 0, expectedNodes);
    checkNullResults(nodeForwarder.setTaskCounter(selector, 12), expectedNodes);
    checkNodes(nodeForwarder.state(selector), JPPFNodeState.class, state -> state.getNbTasksExecuted() == 12, expectedNodes);
    checkNullResults(nodeForwarder.setTaskCounter(selector, 0), expectedNodes);
    checkNodes(nodeForwarder.state(selector), JPPFNodeState.class, state -> state.getNbTasksExecuted() == 0, expectedNodes);
  }

  /**
   * Test getting the node system information.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testSystemInformation() throws Exception {
    for (final Map.Entry<NodeSelector, String[]> entry: selectorMap.entrySet()) testSystemInformation(entry.getKey(), entry.getValue());
  }

  /**
   * Execute the tests with the specified node selector.
   * @param selector the selector to apply.
   * @param expectedNodes the set of nodes the selector is expected to resolve to.
   * @throws Exception if any error occurs.
   */
  private static void testSystemInformation(final NodeSelector selector, final String... expectedNodes) throws Exception {
    final String selectorClass = selector.getClass().getSimpleName();
    BaseTestHelper.printToAll(driverJmx, true, true, true, true, false, ">>> testing with %s, expectedNodes=%s", selectorClass, Arrays.toString(expectedNodes));
    final Map<String, Object> result = nodeForwarder.systemInformation(selector);
    checkNodes(result, JPPFSystemInformation.class, expectedNodes);
    for (final Map.Entry<String, Object> entry : result.entrySet()) {
      final JPPFSystemInformation info = (JPPFSystemInformation) entry.getValue();
      assertNotNull(info);
      assertNotNull(info.getEnv());
      assertFalse(info.getEnv().isEmpty());
      assertNotNull(info.getJppf());
      assertEquals(1, (int) info.getJppf().get(JPPFProperties.PROCESSING_THREADS));
      assertEquals(entry.getKey(), info.getJppf().getString("jppf.node.uuid"));
      assertFalse(info.getJppf().isEmpty());
      assertNotNull(info.getNetwork());
      assertFalse(info.getNetwork().isEmpty());
      assertNotNull(info.getRuntime());
      assertFalse(info.getRuntime().isEmpty());
      assertEquals(Runtime.getRuntime().availableProcessors(), info.getRuntime().getInt("availableProcessors"));
      assertNotNull(info.getStorage());
      assertFalse(info.getStorage().isEmpty());
      assertNotNull(info.getSystem());
      assertFalse(info.getSystem().isEmpty());
    }
  }

  /**
   * Test updating the ndoe's configuration.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testUpdateConfiguration() throws Exception {
    for (final Map.Entry<NodeSelector, String[]> entry: selectorMap.entrySet()) testUpdateConfiguration(entry.getKey(), entry.getValue());
  }

  /**
   * Execute the tests with the specified node selector.
   * @param selector the selector to apply.
   * @param expectedNodes the set of nodes the selector is expected to resolve to.
   * @throws Exception if any error occurs.
   */
  private static void testUpdateConfiguration(final NodeSelector selector, final String... expectedNodes) throws Exception {
    final String selectorClass = selector.getClass().getSimpleName();
    BaseTestHelper.printToAll(driverJmx, true, true, true, true, false, ">>> testing with %s, expectedNodes=%s", selectorClass, Arrays.toString(expectedNodes));
    TypedProperties oldConfig = null, newConfig = null;
    BaseTestHelper.printToAll(driverJmx, true, true, true, true, false, ">>> getting system info");
    Map<String, Object> result = nodeForwarder.systemInformation(selector);
    checkNodes(result, JPPFSystemInformation.class, expectedNodes);
    for (final Map.Entry<String, Object> entry : result.entrySet()) {
      final String uuid = entry.getKey();
      final JPPFSystemInformation info = (JPPFSystemInformation) entry.getValue();
      assertNotNull(info);
      final TypedProperties config = info.getJppf();
      assertNotNull(config);
      assertFalse(config.isEmpty());
      assertEquals(1, (int) config.get(JPPFProperties.PROCESSING_THREADS));
      assertEquals(uuid, config.getString("jppf.node.uuid"));
      if (oldConfig == null) oldConfig = new TypedProperties(config);
      if (newConfig == null) newConfig = new TypedProperties(config).set(JPPFProperties.PROCESSING_THREADS, 8).setString("custom.property", "custom.value");
    }
    BaseTestHelper.printToAll(driverJmx, true, true, true, true, false, ">>> updating config");
    result = nodeForwarder.updateConfiguration(selector, newConfig, false);
    checkNoException(result, expectedNodes);
    BaseTestHelper.printToAll(driverJmx, true, true, true, true, false, ">>> getting new system info");
    result = nodeForwarder.systemInformation(selector);
    checkNodes(result, JPPFSystemInformation.class, expectedNodes);
    for (final Map.Entry<String, Object> entry : result.entrySet()) {
      final String uuid = entry.getKey();
      final JPPFSystemInformation info = (JPPFSystemInformation) entry.getValue();
      assertNotNull(info);
      newConfig = info.getJppf();
      assertNotNull(newConfig);
      assertFalse(newConfig.isEmpty());
      assertEquals(8, (int) newConfig.get(JPPFProperties.PROCESSING_THREADS));
      assertEquals(uuid, newConfig.getString("jppf.node.uuid"));
      assertEquals("custom.value", newConfig.getString("custom.property"));
    }
    BaseTestHelper.printToAll(driverJmx, true, true, true, true, false, ">>> resetting config");
    nodeForwarder.updateConfiguration(selector, oldConfig, false);
  }

  /**
   * Test cancelling a job executing in the nodes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  @Override
  public void testCancelJob() throws Exception {
    for (final Map.Entry<NodeSelector, String[]> entry: selectorMap.entrySet()) testCancelJob(entry.getKey(), entry.getValue());
  }

  /**
   * Execute the tests with the specified node selector.
   * @param selector the selector to apply.
   * @param expectedNodes the set of nodes the selector is expected to resolve to.
   * @throws Exception if any error occurs.
   */
  private static void testCancelJob(final NodeSelector selector, final String... expectedNodes) throws Exception {
    final String methodName = ReflectionUtils.getCurrentMethodName();
    final String selectorClass = selector.getClass().getSimpleName();
    BaseTestHelper.printToAll(client, false, true, true, true, false, ">>> testing with selector=%s,  expectedNodes=%s", selectorClass, Arrays.toString(expectedNodes));
    final int nbNodes = expectedNodes.length;
    final int nbTasks = 5 * nbNodes;
    final JPPFJob job = BaseTestHelper.createJob(methodName + "-" + selectorClass, false, nbTasks, LifeCycleTask.class, 5000L);
    job.getSLA().setExecutionPolicy(new OneOf("jppf.node.uuid", false, expectedNodes));
    final String uuid = job.getUuid();
    client.submitAsync(job);
    Thread.sleep(1000L);
    final Map<String, Object> result = nodeForwarder.cancelJob(selector, uuid, false);
    checkNoException(result, expectedNodes);
    final List<Task<?>> jobResult = job.awaitResults();
    assertNotNull(jobResult);
    assertEquals(nbTasks, jobResult.size());
    int count = 0;
    for (final Task<?> t : jobResult) {
      final LifeCycleTask task = (LifeCycleTask) t;
      if (count == 0) assertTrue(task.isCancelled());
      assertNull(task.getResult());
      count++;
    }
    nodeForwarder.resetTaskCounter(selector);
  }

  /**
   * Test getting the class loader's delegation model.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testGetDelegationModel() throws Exception {
    for (final Map.Entry<NodeSelector, String[]> entry: selectorMap.entrySet()) testGetDelegationModel(entry.getKey(), entry.getValue());
  }

  /**
   * Execute the tests with the specified node selector.
   * @param selector the selector to apply.
   * @param expectedNodes the set of nodes the selector is expected to resolve to.
   * @throws Exception if any error occurs.
   */
  private static void testGetDelegationModel(final NodeSelector selector, final String... expectedNodes) throws Exception {
    checkNodes(nodeForwarder.getDelegationModel(selector), DelegationModel.class, model -> model == DelegationModel.PARENT_FIRST, expectedNodes);
  }

  /**
   * Test setting the class loader's delegation model.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testSetDelegationModel() throws Exception {
    for (final Map.Entry<NodeSelector, String[]> entry: selectorMap.entrySet()) testSetDelegationModel(entry.getKey(), entry.getValue());
  }

  /**
   * Execute the tests with the specified node selector.
   * @param selector the selector to apply.
   * @param expectedNodes the set of nodes the selector is expected to resolve to.
   * @throws Exception if any error occurs.
   */
  private static void testSetDelegationModel(final NodeSelector selector, final String... expectedNodes) throws Exception {
    checkNullResults(nodeForwarder.setDelegationModel(selector, DelegationModel.URL_FIRST), expectedNodes);
    checkNodes(nodeForwarder.getDelegationModel(selector), DelegationModel.class, model -> model == DelegationModel.URL_FIRST, expectedNodes);
    checkNullResults(nodeForwarder.setDelegationModel(selector, DelegationModel.PARENT_FIRST), expectedNodes);
    checkNodes(nodeForwarder.getDelegationModel(selector), DelegationModel.class, model -> model == DelegationModel.PARENT_FIRST, expectedNodes);
  }
}
