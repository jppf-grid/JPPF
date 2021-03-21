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

package test.org.jppf.embedded;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.*;

import org.jppf.client.JPPFClient;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.NodeRunner;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.TypedProperties;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import test.org.jppf.client.CommonClientTests;
import test.org.jppf.management.CommonDriverAdminTests;
import test.org.jppf.test.setup.*;

/**
 * Tests for a simple embedded grid.
 * @author Laurent Cohen
 */
public class TestSimpleEmbeddedGrid extends BaseTest {
  /**
   * Number of drivers and nodes.
   */
  private static final int nbDrivers = 1, nbNodes = 2;
  /**
   * The node runners.
   */
  private static NodeRunner[] runners;
  /**
   * The driver.
   */
  private static JPPFDriver driver;
  /** */
  @Rule
  public TestWatcher setup1D1N1CWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      print(false, false, "start of method %s()", description.getMethodName());
    }
  };

  /**
   * Launches a driver and node and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    print(false, true, "setup with %d drivers and %d nodes", nbDrivers, nbNodes);
    ConfigurationHelper.setLoggerLevels("classes/tests/config/log4j-embedded-grid.properties");
    final Map<String, Object> bindings = new HashMap<>();
    bindings.put("$nbDrivers", nbDrivers);
    bindings.put("$nbNodes", nbNodes);

    bindings.put("$n", 1);
    String path = "classes/tests/config/driver.template.properties";
    Assert.assertTrue(new File(path).exists());
    final TypedProperties driverConfig = ConfigurationHelper.createConfigFromTemplate(path, bindings).set(DEADLOCK_DETECTOR_ENABLED, false);
    print(false, false, ">>> starting the JPPF driver");
    driver = new JPPFDriver(driverConfig);
    print(false, false, ">>> calling JPPFDriver.start()");
    driver.start();

    runners = new NodeRunner[nbNodes];
    path = "classes/tests/config/node.template.properties";
    Assert.assertTrue(new File(path).exists());
    for (int i=0; i<nbNodes; i++) {
      bindings.put("$n", i + 1);
      final TypedProperties nodeConfig = ConfigurationHelper.createConfigFromTemplate(path, bindings).set(DEADLOCK_DETECTOR_ENABLED, false);
      print(false, false, ">>> starting the JPPF node " + (i + 1));
      runners[i] = new NodeRunner(nodeConfig);
      final NodeRunner runner = runners[i];
      new Thread(() -> runner.start(), String.format("[node-%03d]", i + 1)).start();
    }
    final TypedProperties clientConfig = getClientConfig();
    clientConfig.remove("jppf.node.uuid");
    client = new JPPFClient(clientConfig);
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection(client);
    print(false, false, ">>> initializing %s", jmx);
    Assert.assertTrue(jmx.connectAndWait(5000L));
    print(false, false, ">>> JMX connection established");
    assertTrue(ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> jmx.nbIdleNodes() >= nbNodes, 5000L, 250L, false));
    print(false, false, ">>> checked JMX connection");
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    try {
      if (client != null) client.close();
      if (runners != null) {
        int i = 0;
        for (final NodeRunner runner: runners) {
          print(false, false, "<<< stoping the JPPF node " + (++i));
          runner.shutdown();
        }
      }
      if (driver != null) {
        print(false, false, "<<< shutting down driver");
        driver.shutdown();;
      }
    } finally {
      BaseSetup.generateClientThreadDump();
    }
  }

  /**
   * Test the submission of a job.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testSubmit() throws Exception {
    CommonClientTests.testSubmit(client);
  }

  /**
   * Test the cancellation of a job.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testCancelJob() throws Exception {
    CommonClientTests.testCancelJob(client);
  }

  /**
   * Test getting node management information from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testNodesInformation() throws Exception {
    CommonDriverAdminTests.testNodesInformation(client, nbNodes);
  }

  /**
   * Test getting idle nodes information from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testIdleNodesInformation() throws Exception {
    CommonDriverAdminTests.testIdleNodesInformation(client, nbNodes);
  }

  /**
   * Test getting the number of nodes attached to the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testNbNodes() throws Exception {
    CommonDriverAdminTests.testNbNodes(client, nbNodes);
  }

  /**
   * Test getting the number of idle nodes from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testNbIdleNodes() throws Exception {
    CommonDriverAdminTests.testNbIdleNodes(client, nbNodes);
  }

  /**
   * Test getting and setting load-balancer information in the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testGetLoadBalancerInformation() throws Exception {
    CommonDriverAdminTests.testGetLoadBalancerInformation(client);
  }

  /**
   * Test getting and setting load-balancer information in the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testSetLoadBalancerInformation() throws Exception {
    CommonDriverAdminTests.testSetLoadBalancerInformation(client);
  }

  /**
   * Test of matching the nodes against an execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testNodesMatchingExecutionPolicy() throws Exception {
    CommonDriverAdminTests.testNodesMatchingExecutionPolicy(client, nbNodes);
  }

  /**
   * Test activating and deactivating one or more nodes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testToggleActiveState() throws Exception {
    CommonDriverAdminTests.testToggleActiveState(client, nbNodes);
  }

  /**
   * Test activating and deactivating one or more nodes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testGetAndSetActiveState() throws Exception {
    CommonDriverAdminTests.testGetAndSetActiveState(client, nbNodes);
  }

  /**
   * Load a client configuration.
   * @return a fully resolved client configuration.
   * @throws Exception if any error occurs.
   */
  private static TypedProperties getClientConfig() throws Exception {
    final Map<String, Object> bindings = new HashMap<>();
    bindings.put("$nbDrivers", nbDrivers);
    bindings.put("$nbNodes", nbNodes);
    return ConfigurationHelper.createConfigFromTemplate(BaseSetup.DEFAULT_CONFIG.clientConfig, bindings);
  }
}
