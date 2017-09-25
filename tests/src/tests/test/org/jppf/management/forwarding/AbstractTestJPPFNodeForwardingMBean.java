/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.utils.TypedProperties;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * Unit tests for {@link JPPFDriverAdminMBean}.
 * In this class, we test that the functionality of the DriverJobManagementMBean from the client point of view.
 * @author Laurent Cohen
 */
public abstract class AbstractTestJPPFNodeForwardingMBean extends BaseTest {
  /**
   * Connection to the driver's JMX server.
   */
  protected static JMXDriverConnectionWrapper driverJmx = null;
  /**
   * The driver mbean which delegates operations to specified nodes.
   */
  protected static JPPFNodeForwardingMBean nodeForwarder = null;
  /**
   * The uuids of all nodes.
   */
  protected static Set<String> allNodes = new HashSet<>();
  /**
   * 
   */
  protected static LoadBalancingInformation oldLbi = null;
  /** */
  @Rule
  public TestWatcher setup1D2N1CWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      BaseTestHelper.printToServersAndNodes(client, true, true, "start of method %s()", description.getMethodName());
    }
  };

  /**
   * Launches a driver and node and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    int nbNodes = 2;
    client = BaseSetup.setup(2);
    for (int i = 1; i <= nbNodes; i++)
      allNodes.add("n" + i);
    driverJmx = BaseSetup.getJMXConnection(client);
    nodeForwarder = driverJmx.getNodeForwarder();
    boolean ready = false;
    NodeSelector selector = new AllNodesSelector();
    String[] array = new String[nbNodes];
    while (!ready) {
      try {
        Map<String, Object> result = nodeForwarder.state(selector);
        checkNodes(result, JPPFNodeState.class, allNodes.toArray(array));
        ready = true;
      } catch (@SuppressWarnings("unused") Exception e) {
        Thread.sleep(100L);
      } catch (@SuppressWarnings("unused") AssertionError e) {
        Thread.sleep(100L);
      }
    }
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    BaseSetup.cleanup();
  }

  /**
   * Check that there are results for all the expected nodes, and only these ndoes.
   * @param result the result to chek.
   * @param expectedClass the expected class of each result value.
   * @param expectedNodes the list of expectd nodes.
   * @throws Exception if any error occurs or the check fails.
   */
  protected static void checkNodes(final Map<String, Object> result, final Class<?> expectedClass, final String... expectedNodes) throws Exception {
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(expectedNodes.length, result.size());
    for (String uuid : expectedNodes) {
      assertTrue(result.containsKey(uuid));
      Object value = result.get(uuid);
      assertNotNull(value);
      assertEquals(expectedClass, value.getClass());
    }
  }

  /**
   * Check that the specified map is empty.
   * @param result the result map to chek.
   * @throws Exception if any error occurs or the check fails.
   */
  protected static void checkEmpty(final Map<String, Object> result) throws Exception {
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  /**
   * Check that the specified map contains no exception among its values.
   * @param result the result map to chek.
   * @param expectedNodes the list of expectd nodes.
   * @throws Exception if any error occurs or the check fails.
   */
  protected static void checkNoException(final Map<String, Object> result, final String... expectedNodes) throws Exception {
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(expectedNodes.length, result.size());
    for (String uuid : expectedNodes) {
      assertTrue(result.containsKey(uuid));
      Object value = result.get(uuid);
      assertFalse(value instanceof Exception);
    }
  }

  /**
   * Test getting and setting load-balancer information in the server.
   * @throws Exception if any error occurs.
   */
  public static void configureLoadBalancer() throws Exception {
    oldLbi = driverJmx.loadBalancerInformation();
    TypedProperties newConfig = new TypedProperties();
    newConfig.setProperty("size", "1");
    driverJmx.changeLoadBalancerSettings("manual", newConfig);
  }

  /**
   * Test getting and setting load-balancer information in the server.
   * @throws Exception if any error occurs.
   */
  public static void resetLoadBalancer() throws Exception {
    if (oldLbi != null) {
      driverJmx.changeLoadBalancerSettings(oldLbi.getAlgorithm(), oldLbi.getParameters());
      oldLbi = null;
    }
  }
}
