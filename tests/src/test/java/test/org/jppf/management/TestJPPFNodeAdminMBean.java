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

package test.org.jppf.management;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.classloader.DelegationModel;
import org.jppf.client.JPPFJob;
import org.jppf.management.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link JPPFNodeAdminMBean}.
 * In this class, we test that the functionality of the JPPFNodeAdminMBean from the client point of view.
 * @author Laurent Cohen
 */
public class TestJPPFNodeAdminMBean extends BaseTest {
  /**
   * Connection to the node's JMX server.
   */
  private static JMXNodeConnectionWrapper nodeJmx = null;
  /**
   * Connection to the driver's JMX server.
   */
  private static JMXDriverConnectionWrapper driverJmx = null;

  /**
   * Launches a driver and node and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    client = BaseSetup.setup(1, 1, true, true, BaseSetup.DEFAULT_CONFIG);
    driverJmx = BaseSetup.getJMXConnection(client);
    final Collection<JPPFManagementInfo> coll = driverJmx.nodesInformation();
    final JPPFManagementInfo info = coll.iterator().next();
    nodeJmx = new JMXNodeConnectionWrapper(info.getHost(), info.getPort(), info.isSecure());
    nodeJmx.connectAndWait(5000L);
    if (!nodeJmx.isConnected()) {
      nodeJmx = null;
      throw new Exception("could not connect to the node's JMX server");
    }
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    Exception e = null;
    if (nodeJmx != null) {
      try {
        nodeJmx.close();
      } catch(final Exception e2) {
        e = e2;
      } finally {
        nodeJmx = null;
      }
    }
    BaseSetup.cleanup();
    if (e != null) throw e;
  }

  /**
   * Test getting the node state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testState() throws Exception {
    JPPFNodeState state = nodeJmx.state();
    assertNotNull(state);
    assertEquals(0, state.getNbTasksExecuted());
    assertEquals(1, state.getThreadPoolSize());
    assertEquals(Thread.NORM_PRIORITY, state.getThreadPriority());
    assertEquals(JPPFNodeState.ConnectionState.CONNECTED, state.getConnectionStatus());
    assertEquals(JPPFNodeState.ExecutionState.IDLE, state.getExecutionStatus());
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, LifeCycleTask.class, 2000L);
    client.submitAsync(job);
    Thread.sleep(750L);
    state = nodeJmx.state();
    assertNotNull(state);
    assertEquals(0, state.getNbTasksExecuted());
    assertEquals(JPPFNodeState.ExecutionState.EXECUTING, state.getExecutionStatus());
    job.awaitResults();
    state = nodeJmx.state();
    assertNotNull(state);
    assertEquals(1, state.getNbTasksExecuted());
    assertEquals(JPPFNodeState.ExecutionState.IDLE, state.getExecutionStatus());
    nodeJmx.resetTaskCounter();
  }

  /**
   * Test updating the number of processing threads.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testUpdateThreadPoolSize() throws Exception {
    JPPFNodeState state = nodeJmx.state();
    assertNotNull(state);
    assertEquals(1, state.getThreadPoolSize());
    try {
      nodeJmx.updateThreadPoolSize(3);
      state = nodeJmx.state();
      assertNotNull(state);
      assertEquals(3, state.getThreadPoolSize());
    } finally {
      nodeJmx.updateThreadPoolSize(1);
    }
  }

  /**
   * Test updating the priority of processing threads.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testUpdateThreadPriority() throws Exception {
    JPPFNodeState state = nodeJmx.state();
    assertNotNull(state);
    assertEquals(Thread.NORM_PRIORITY, state.getThreadPriority());
    try {
      nodeJmx.updateThreadsPriority(Thread.MAX_PRIORITY);
      state = nodeJmx.state();
      assertNotNull(state);
      assertEquals(Thread.MAX_PRIORITY, state.getThreadPriority());
    } finally {
      nodeJmx.updateThreadsPriority(Thread.NORM_PRIORITY);
    }
  }

  /**
   * Test resetting the tasks counter.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testResetTaskCounter() throws Exception {
    final int nbTasks = 12;
    JPPFNodeState state = nodeJmx.state();
    assertNotNull(state);
    assertEquals(0, state.getNbTasksExecuted());
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class, 1L);
    client.submit(job);
    state = nodeJmx.state();
    assertNotNull(state);
    assertEquals(nbTasks, state.getNbTasksExecuted());
    nodeJmx.resetTaskCounter();
    state = nodeJmx.state();
    assertNotNull(state);
    assertEquals(0, state.getNbTasksExecuted());
  }

  /**
   * Test setting the tasks counter to a given value.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testSetTaskCounter() throws Exception {
    JPPFNodeState state = nodeJmx.state();
    assertNotNull(state);
    assertEquals(0, state.getNbTasksExecuted());
    nodeJmx.setTaskCounter(12);
    state = nodeJmx.state();
    assertNotNull(state);
    assertEquals(12, state.getNbTasksExecuted());
    nodeJmx.setTaskCounter(0);
    state = nodeJmx.state();
    assertNotNull(state);
    assertEquals(0, state.getNbTasksExecuted());
  }

  /**
   * Test getting the node system information.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testSystemInformation() throws Exception {
    final JPPFSystemInformation info = nodeJmx.systemInformation();
    assertNotNull(info);
    assertNotNull(info.getEnv());
    assertFalse(info.getEnv().isEmpty());
    assertNotNull(info.getJppf());
    assertEquals(1, (int) info.getJppf().get(JPPFProperties.PROCESSING_THREADS));
    assertEquals("n1", info.getJppf().getString("jppf.node.uuid"));
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

  /**
   * Test updating the ndoe's configuration.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testUpdateConfiguration() throws Exception {
    JPPFSystemInformation info = nodeJmx.systemInformation();
    assertNotNull(info);
    final TypedProperties config = info.getJppf();
    assertNotNull(config);
    assertFalse(config.isEmpty());
    assertEquals(1, (int) config.get(JPPFProperties.PROCESSING_THREADS));
    assertEquals("n1", config.getString("jppf.node.uuid"));
    TypedProperties newConfig = new TypedProperties(config).set(JPPFProperties.PROCESSING_THREADS, 8).setString("custom.property", "custom.value");
    nodeJmx.updateConfiguration(newConfig, false);
    info = nodeJmx.systemInformation();
    assertNotNull(info);
    newConfig = info.getJppf();
    assertNotNull(newConfig);
    assertFalse(newConfig.isEmpty());
    assertEquals(8, (int) newConfig.get(JPPFProperties.PROCESSING_THREADS));
    assertEquals("n1", newConfig.getString("jppf.node.uuid"));
    assertEquals("custom.value", newConfig.getString("custom.property"));
    nodeJmx.updateConfiguration(config, false);
  }

  /**
   * Test cancelling a job executing in the node.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testCancelJob() throws Exception {
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, LifeCycleTask.class, 5000L);
    final String uuid = job.getUuid();
    client.submitAsync(job);
    Thread.sleep(750L);
    nodeJmx.cancelJob(uuid, false);
    final List<Task<?>> result = job.awaitResults();
    assertNotNull(result);
    assertEquals(1, result.size());
    final LifeCycleTask task = (LifeCycleTask) result.get(0);
    assertTrue(task.isCancelled());
    assertNull(task.getResult());
    nodeJmx.resetTaskCounter();
  }

  /**
   * Test getting the class loader's delegation model.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testGetDelegationModel() throws Exception {
    final DelegationModel model = nodeJmx.getDelegationModel();
    assertEquals(DelegationModel.PARENT_FIRST, model);
  }

  /**
   * Test setting the class loader's delegation model.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testSetDelegationModel() throws Exception {
    DelegationModel model = nodeJmx.getDelegationModel();
    assertEquals(DelegationModel.PARENT_FIRST, model);
    nodeJmx.setDelegationModel(DelegationModel.URL_FIRST);
    model = nodeJmx.getDelegationModel();
    assertEquals(DelegationModel.URL_FIRST, model);
    nodeJmx.setDelegationModel(DelegationModel.PARENT_FIRST);
    model = nodeJmx.getDelegationModel();
    assertEquals(DelegationModel.PARENT_FIRST, model);
  }
}
