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

package test.org.jppf.client;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-533">JPPF-533</a>.
 * @author Laurent Cohen
 */
public class TestJobManyConnections extends AbstractNonStandardSetup {
  /**
   * Launches 2 drivers connected to each other,  with 1 node attached to each and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final int nbDrivers = 3;
    final TestConfiguration config = createConfig("p2p");
    config.driver.jppf = "classes/tests/config/p2p/driver2.properties";
    config.driver.log4j = "classes/tests/config/p2p/log4j-driver2.properties";
    client = BaseSetup.setup(nbDrivers, nbDrivers, true, false, config);
    checkPeers(nbDrivers, 10_000L, false, true);
    client.setLoadBalancerSettings("proportional", new TypedProperties().setInt("InitialiSize", 10));
  }

  /**
   * Test job submission with pool size = 10 and getMachChannels() = 10.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testSubmitJobManyRemoteChannels() throws Exception {
    final int nbTasks = 100;
    for (int i=1; i<=3; i++) {
      BaseTestHelper.printToAll(client, true, true, true, true, false, ">>> creating job %d", i);
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + "-" + i, true, false, nbTasks, LifeCycleTask.class, 1L);
      BaseTestHelper.printToAll(client, true, true, true, true, false, ">>> submitting job %d", i);
      final List<Task<?>> results = client.submitJob(job);
      BaseTestHelper.printToAll(client, true, true, true, true, false, ">>> checking job %d results", i);
      testJobResults(nbTasks, results);
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
