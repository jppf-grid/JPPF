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

package test.org.jppf.server.protocol;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.execute.async.AbstractAsyncExecutionManager;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.*;
import org.jppf.management.forwarding.NodeForwardingMBean;
import org.jppf.node.NodeInternal;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;
import test.org.jppf.test.setup.common.LifeCycleTask.JobInfo;

/**
 * Unit tests for {@link org.jppf.node.protocol.JobSLA JobSLA}.
 * In this class, we test that the behavior is the expected one, from the client point of view,
 * as specified in the job SLA.
 * @author Laurent Cohen
 */
public class TestJPPFJobSLA4 extends BaseTest {
  /**
   * Launches a driver and 1 node and start the client.
   * The node has the classes from server and common mpdules in its classpath.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final TestConfiguration config = TestConfiguration.newDefault();
    config.driver.jppf = CONFIG_ROOT_DIR + "driver.TestJPPFJobSLA4.properties";
    client = BaseSetup.setup(1, 2, true, true, config);
  }

  /**
   * Stops the driver and nodes and close the client.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    try {
      BaseSetup.cleanup();
    } finally {
      BaseSetup.resetClientConfig();
    }
  }

  /**
   * Test that the size of a server-side dispatch does not exceed the maximum specified in the SLA.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testMaxDispatchSize() throws Exception {
    checkNodes();
    final int nbTasks = 50, maxDispatchSize = 6;
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection();
    final LoadBalancingInformation clientLBI = client.getLoadBalancerSettings();
    final LoadBalancingInformation driverLBI = jmx.loadBalancerInformation();
    try {
      client.setLoadBalancerSettings("manual", new TypedProperties().setInt("size", 100));
      jmx.changeLoadBalancerSettings("manual", new TypedProperties().setInt("size", 15));
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class, 10L);
      job.getSLA().setMaxDispatchSize(maxDispatchSize);
      final List<Task<?>> results = client.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      for (final Task<?> result: results) {
        assertNotNull(result.getResult());
        assertNull(result.getThrowable());
        assertTrue(result instanceof LifeCycleTask);
        final LifeCycleTask task = (LifeCycleTask) result;
        final JobInfo info = task.getJobInfo();
        print(false, false, "%s ==> %d tasks in dispatch", task.getId(), info.taskCount);
        assertCompare(Operator.AT_MOST, maxDispatchSize, info.taskCount);
      }
    } finally {
      client.setLoadBalancerSettings(clientLBI.getAlgorithm(), clientLBI.getParameters());
      jmx.changeLoadBalancerSettings(driverLBI.getAlgorithm(), driverLBI.getParameters());
    }
  }

  /**
   * Test that a job with {@code allowMultipleDispatchesToSameChannel = true} is dispatched concurrently to the same node.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testAllowMultipleDispatchesToSameChannel() throws Exception {
    checkNodes();
    final int nbTasks = 50;
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection();
    final LoadBalancingInformation clientLBI = client.getLoadBalancerSettings();
    final LoadBalancingInformation driverLBI = jmx.loadBalancerInformation();
    try {
      client.setLoadBalancerSettings("manual", new TypedProperties().setInt("size", 100));
      jmx.changeLoadBalancerSettings("manual", new TypedProperties().setInt("size", 15));
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, DispatchCountingTask.class);
      job.getSLA().setMaxDispatchSize(6).setAllowMultipleDispatchesToSameChannel(true);
      final List<Task<?>> results = client.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      int maxConcurrentDispatches = 0;
      for (final Task<?> result: results) {
        assertNotNull(result.getResult());
        assertNull(result.getThrowable());
        assertTrue(result instanceof DispatchCountingTask);
        final DispatchCountingTask task = (DispatchCountingTask) result;
        if (task.nbDispatches > maxConcurrentDispatches) maxConcurrentDispatches = task.nbDispatches;
      }
      print(false, false, "max concurrent dispatches: %d", maxConcurrentDispatches);
      assertCompare(Operator.MORE_THAN, 1, maxConcurrentDispatches);
    } finally {
      client.setLoadBalancerSettings(clientLBI.getAlgorithm(), clientLBI.getParameters());
      jmx.changeLoadBalancerSettings(driverLBI.getAlgorithm(), driverLBI.getParameters());
    }
  }

  /**
   * Test that a job with {@code allowMultipleDispatchesToSameChannel = false} is not dispatched concurrently to the same node.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testPreventMultipleDispatchesToSameChannel() throws Exception {
    checkNodes();
    final int nbTasks = 50;
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection();
    final LoadBalancingInformation clientLBI = client.getLoadBalancerSettings();
    final LoadBalancingInformation driverLBI = jmx.loadBalancerInformation();
    try {
      client.setLoadBalancerSettings("manual", new TypedProperties().setInt("size", 100));
      jmx.changeLoadBalancerSettings("manual", new TypedProperties().setInt("size", 15));
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, DispatchCountingTask.class);
      job.getSLA().setMaxDispatchSize(6).setAllowMultipleDispatchesToSameChannel(false);
      final List<Task<?>> results = client.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      int maxConcurrentDispatches = 0;
      for (final Task<?> result: results) {
        assertNotNull(result.getResult());
        assertNull(result.getThrowable());
        assertTrue(result instanceof DispatchCountingTask);
        final DispatchCountingTask task = (DispatchCountingTask) result;
        if (task.nbDispatches > maxConcurrentDispatches) maxConcurrentDispatches = task.nbDispatches;
      }
      print(false, false, "max concurrent dispatches: %d", maxConcurrentDispatches);
      assertCompare(Operator.EQUAL, 1, maxConcurrentDispatches);
    } finally {
      client.setLoadBalancerSettings(clientLBI.getAlgorithm(), clientLBI.getParameters());
      jmx.changeLoadBalancerSettings(driverLBI.getAlgorithm(), driverLBI.getParameters());
    }
  }

  /**
   * Wait until all nodes are connected to the driver via JMX.
   * @throws Exception if any error occurs.
   */
  private static void checkNodes() throws Exception {
    final int nbNodes = BaseSetup.nbNodes();
    final JMXDriverConnectionWrapper driverJmx = BaseSetup.getJMXConnection(client);
    final NodeForwardingMBean nodeForwarder = driverJmx.getForwarder();
    while (true) {
      final ResultsMap<String, JPPFNodeState> result = nodeForwarder.state(NodeSelector.ALL_NODES);
      if (result.size() == nbNodes) {
        int count = 0;
        for (final Map.Entry<String, InvocationResult<JPPFNodeState>>entry: result.entrySet()) {
          if (entry.getValue().result() != null) count++;
          else break;
        }
        if (count == nbNodes) break;
      }
      Thread.sleep(100L);
    }
  }

  /**
   * 
   */
  public static class DispatchCountingTask extends AbstractTask<String> {
    /**
     * Number of dispatches for the job this task belongs to in the node that executes it.
     */
    int nbDispatches;

    @Override
    public void run() {
      try {
        final AbstractAsyncExecutionManager exec = (AbstractAsyncExecutionManager) ((NodeInternal) getNode()).getExecutionManager();
        nbDispatches = exec.getNbBundles(getJob().getUuid());
        Thread.sleep(10L);
        setResult("success");
      } catch (final Exception e) {
        setThrowable(e);
      }
    }
  }
}
