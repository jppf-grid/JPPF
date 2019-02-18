/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package test.org.jppf.node;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.location.*;
import org.jppf.management.*;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit test for nodes in offline mode.
 * @author Laurent Cohen
 */
public class TestOfflineNode extends AbstractNonStandardSetup {
  /** */
  @Rule
  public TestWatcher setup1D1N1CWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      BaseTestHelper.printToAll(client, false, false, true, true, true, "start of method %s()", description.getMethodName());
    }
  };

  /**
   * Launches a driver and 2 nodes and starts the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final TestConfiguration testConfig = createConfig("offline_node");
    final List<String> cp = testConfig.node.classpath;
    for (int i=0; i<cp.size(); i++) {
      if (cp.get(i).replace("\\", "/").contains("tests/classes/framework")) {
        cp.remove(i);
        break;
      }
    }
    print(false, "setup node classpath: %s", cp);
    cp.add("../server/classes");
    testConfig.driver.log4j = "classes/tests/config/offline_node/log4j-driver.properties";
    testConfig.node.log4j = "classes/tests/config/offline_node/log4j-node.properties";
    client = BaseSetup.setup(1, 2, true, false, testConfig);
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection(client);
    print(false, false, "waiting for at least %d nodes", BaseSetup.nbNodes());
    ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> jmx.nbNodes() >= BaseSetup.nbNodes(), 5_000L, 500L, true);
    print(false, false, "got %d nodes", jmx.nbNodes());
  }

  /**
   * Test that a simple job triggers a deserialization error in the node (because the task class is not found in the classpath),
   * and that this error is handled properly.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testSimpleJobDeserializationError() throws Exception {
    final int nbTasks = 5;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 10L);
    job.getSLA().getClassPath().setForceClassLoaderReset(true);
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (final Task<?> task : results) {
      assertTrue("task = " + task, task instanceof LifeCycleTask);
      final Throwable t = task.getThrowable();
      assertNotNull("throwable for task '" + task.getId() + "' is null", t);
      assertNull(task.getResult());
    }
  }

  /**
   * Test that a simple job is normally executed.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testSimpleJob() throws Exception {
    final int nbTasks = 5;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 10L);
    final Location<?> loc = new MemoryLocation(new FileLocation("build/jppf-test-framework.jar").toByteArray());
    job.getSLA().getClassPath().add(loc);
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (final Task<?> task : results) {
      assertTrue("task = " + task, task instanceof LifeCycleTask);
      final Throwable t = task.getThrowable();
      assertNull("throwable for task '" + task.getId() + "' : " + ExceptionUtils.getStackTrace(t), t);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
  }

  /**
   * Test that a simple job expires and is cancelled upon first dispatch.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testJobDispatchExpiration() throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 2500L);
    final Location<?> loc = new MemoryLocation(new FileLocation("build/jppf-test-framework.jar").toByteArray());
    job.getSLA().getClassPath().add(loc);
    job.getSLA().setDispatchExpirationSchedule(new JPPFSchedule(1000L));
    job.getSLA().setExecutionPolicy(new Equal("jppf.node.uuid", false, "n2"));
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (final Task<?> task : results) {
      assertTrue("task = " + task, task instanceof LifeCycleTask);
      final Throwable t = task.getThrowable();
      assertNull("throwable for task '" + task.getId() + "' : " + ExceptionUtils.getStackTrace(t), t);
      assertNull(task.getResult());
    }
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection(client);
    assertTrue(ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> jmx.nbIdleNodes() == BaseSetup.nbNodes(), 5000L, 500L, true));
  }

  /**
   * Test that the JMDDriverConnectionWrapper returns correct values for nbNodes() and nbIdleNodes() methods,
   * even though management is disabled in the nodes (but not in the driver).
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testNumberOfNodes() throws Exception {
    BaseSetup.checkDriverAndNodesInitialized(client, 1, 2);
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection(client);
    final NodeSelector node1Selector = new ExecutionPolicySelector(new Equal("jppf.node.uuid", false, "n1"));
    assertEquals(BaseSetup.nbNodes(), (int) jmx.nbNodes());
    assertEquals(1, (int) jmx.nbNodes(node1Selector));
    assertEquals(BaseSetup.nbNodes(), (int) jmx.nbIdleNodes());
    assertEquals(1, (int) jmx.nbIdleNodes(node1Selector));
  }

  /**
   * Test that the JMDDriverConnectionWrapper returns correct values for nodesInformation() and idleNodesInformation() methods,
   * even though management is disabled in the nodes (but not in the driver).
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testNodesInformation() throws Exception {
    BaseSetup.checkDriverAndNodesInitialized(client, 1, 2);
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection(client);
    final NodeSelector node1Selector = new ExecutionPolicySelector(new Equal("jppf.node.uuid", false, "n1"));
    checkNodesInfo(jmx.nodesInformation(), "n1", "n2");
    checkNodesInfo(jmx.nodesInformation(node1Selector), "n1");
    checkNodesInfo(jmx.idleNodesInformation(), "n1", "n2");
    checkNodesInfo(jmx.idleNodesInformation(node1Selector), "n1");
  }

  /**
   * Check that the nodes information retrieved from the server is correct according to the expected node uuids.
   * @param nodesInfo the nodes information from the server.
   * @param expectedUuids the expected uuids of the nodes.
   * @throws Exception if any error occurs.
   */
  private static void checkNodesInfo(final Collection<JPPFManagementInfo> nodesInfo, final String...expectedUuids) throws Exception {
    assertNotNull(nodesInfo);
    assertNotNull(expectedUuids);
    assertEquals(expectedUuids.length, nodesInfo.size());
    final List<JPPFManagementInfo> list = new ArrayList<>(nodesInfo);
    Collections.sort(list, new Comparator<JPPFManagementInfo>() {
      @Override
      public int compare(final JPPFManagementInfo o1, final JPPFManagementInfo o2) {
        return o1.getUuid().compareTo(o2.getUuid());
      }
    });
    Arrays.sort(expectedUuids);
    for (int i=0; i<expectedUuids.length; i++) {
      final JPPFManagementInfo info = list.get(i);
      assertEquals(expectedUuids[i], info.getUuid());
    }
  }
}
