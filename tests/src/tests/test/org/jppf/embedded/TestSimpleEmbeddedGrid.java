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

import static org.junit.Assert.*;

import java.io.File;
import java.util.*;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.NodeRunner;
import org.jppf.node.protocol.Task;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Tests for a simple embedded grid.
 * @author Laurent Cohen
 */
public class TestSimpleEmbeddedGrid extends BaseTest {
  /**
   * Number of drivers and nodes.
   */
  private static final int nbDrivers = 1, nbNodes = 1;
  /**
   * The node runners.
   */
  private static NodeRunner[] runners;
  /**
   * The driver (used via reflection).
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
    try (final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", DRIVER_MANAGEMENT_PORT_BASE + 1)) {
      print(false, false, ">>> initializing %s", jmx);
      Assert.assertTrue(jmx.connectAndWait(5000L));
      print(false, false, ">>> JMX connection established");
      assertTrue(ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> jmx.nbNodes() >= 1, 5000L, 250L, false));
      print(false, false, ">>> checked JMX connection");
    }
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
   * 
   * @throws Exception .
   */
  //@Test(timeout = 10_000)
  public void test() throws Exception {
    client = BaseSetup.createClient(null, true, BaseSetup.DEFAULT_CONFIG);
    assertNotNull(client.awaitConnectionPool(5000L, JPPFClientConnectionStatus.workingStatuses()));
  }

  /**
   * Test the submission of a job.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testSubmit() throws Exception {
    final Map<String, Object> bindings = new HashMap<>();
    bindings.put("$nbDrivers", nbDrivers);
    bindings.put("$nbNodes", nbNodes);
    final TypedProperties clientConfig = ConfigurationHelper.createConfigFromTemplate(BaseSetup.DEFAULT_CONFIG.clientConfig, bindings);
    try (final JPPFClient client = new JPPFClient(clientConfig)) {
      print(false, false, "waiting for working connection");
      assertNotNull(client.awaitConnectionPool(5000L, JPPFClientConnectionStatus.workingStatuses()));
      print(false, false, "got working connection");
      final int nbTasks = 50;
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 0L);
      int i = 0;
      for (final Task<?> task: job.getJobTasks()) task.setId("" + i++);
      print(false, false, "submitting job");
      final List<Task<?>> results = client.submit(job);
      print(false, false, "got job results");
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      final String msg = BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE;
      for (i=0; i<nbTasks; i++) {
        final Task<?> task = results.get(i);
        final Throwable t = task.getThrowable();
        assertNull("task " + i +" has an exception " + t, t);
        assertEquals("result of task " + i + " should be " + msg + " but is " + task.getResult(), msg, task.getResult());
        assertEquals(job.getJobTasks().get(i).getId(), task.getId());
        assertEquals(i, task.getPosition());
      }
    }
  }
}
