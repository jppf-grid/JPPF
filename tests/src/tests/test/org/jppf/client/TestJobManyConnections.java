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

package test.org.jppf.client;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-533">JPPF-533</a>.
 * @author Laurent Cohen
 */
public class TestJobManyConnections extends AbstractNonStandardSetup {
  /** */
  @Rule
  public TestWatcher testJobManyConnections = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      BaseTestHelper.printToAll(client, false, false, true, true, true, "start of method %s()", description.getMethodName());
    }
  };

  /**
   * Launches 3 drivers connected to each other,  with 1 node attached to each and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final int nbDrivers = 3;
    final TestConfiguration config = createConfig("p2p");
    config.driver.jppf = "classes/tests/config/p2p/driver2.properties";
    config.driver.log4j = "classes/tests/config/p2p/log4j-driver2.properties";
    client = BaseSetup.setup(nbDrivers, nbDrivers, true, false, config);
    print(false, false, "client configuration:\n%s", client.getConfig().asString());
    checkPeers(nbDrivers, 10_000L, false, true);
    client.setLoadBalancerSettings("proportional", new TypedProperties().setInt("InitialSize", 10));
  }

  /**
   * Stops the driver and nodes and close the client.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanupTestJobManyConnections() throws Exception {
    for (int i=1; i<=BaseSetup.nbDrivers(); i++) {
      try (final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", 11100 + i)) {
        assertTrue(jmx.connectAndWait(5000L));
        if (i > 2) BaseSetup.generateDriverThreadDump(jmx);
      }
    }
  }

  /**
   * Test job submission with pool size = 10 and getMachChannels() = 10.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testSubmitJobManyRemoteChannels() throws Exception {
    final int nbTasks = 100;
    for (int i=1; i<=3; i++) {
      BaseTestHelper.printToAll(client, true, true, true, true, false, ">>> creating job %d", i);
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + "-" + i, false, nbTasks, LifeCycleTask.class, 1L);
      final Set<Integer> set = new HashSet<>();
      for (final Task<?> task: job.getJobTasks()) {
        final int pos = task.getPosition();
        assertFalse("position " + pos + " duplicated!!!!", set.contains(pos));
        set.add(pos);
      }
      BaseTestHelper.printToAll(client, true, true, true, true, false, ">>> submitting job %d", i);
      final List<Task<?>> results = client.submitAsync(job).get();
      //final List<Task<?>> results = client.submit(job);
      BaseTestHelper.printToAll(client, true, true, true, true, false, ">>> checking job %d results", i);
      testJobResults(nbTasks, results);
    }
  }

  /**
   * Test that a job with maxDriverDepth = 1 is only processed by a single driver (i.e. never sent to a peer).
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testMaxDriverDepth() throws Exception {
    final int nbTasks = 100;
    BaseTestHelper.printToAll(client, true, true, true, true, false, ">>> creating job");
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class);
    job.getClientSLA().setExecutionPolicy(new Equal("jppf.server.port", 11101));
    job.getSLA().setMaxDriverDepth(1);
    BaseTestHelper.printToAll(client, true, true, true, true, false, ">>> submitting job %s", job.getName());
    final List<Task<?>> results = client.submit(job);
    BaseTestHelper.printToAll(client, true, true, true, true, false, ">>> checking results for job %s", job.getName());
    testJobResults(nbTasks, results);
    BaseTestHelper.printToAll(client, true, true, true, true, false, ">>> checking single node 'n1' for job %s", job.getName());
    for (final Task<?> tsk: results) {
      assertTrue(tsk instanceof LifeCycleTask);
      final LifeCycleTask task = (LifeCycleTask) tsk;
      assertEquals("n1", task.getUuidFromNode());
    }
  }

  /**
   * Test the results of a job execution.
   * @param nbTasks the expected number of tasks in the results.
   * @param results the results.
   * @throws Exception if any error occurs.
   */
  private static void testJobResults(final int nbTasks, final List<Task<?>> results) throws Exception {
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    final int count = 0;
    for (final Task<?> task : results) {
      final String prefix = "task " + count + " ";
      final Throwable t = task.getThrowable();
      assertNull(prefix + "has an exception", t);
      assertNotNull(prefix + "result is null", task.getResult());
    }
  }
}
