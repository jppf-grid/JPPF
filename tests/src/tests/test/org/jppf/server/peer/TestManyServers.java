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

package test.org.jppf.server.peer;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Test a multi-server topology with 2 servers, 1 node attached to each server and 1 client.
 * @author Laurent Cohen
 */
public class TestManyServers extends AbstractNonStandardSetup {
  /** */
  private static final long TIMEOUT = 20_000L;
  /** */
  @Rule
  public TestWatcher testMultiServerWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      BaseTestHelper.printToAll(client, false, false, true, true, true, "start of method %s()", description.getMethodName());
    }
  };

  /**
   * Launches 5 drivers with 1 node attached to each and start the client.
   * @throws Exception if a process could not be started.
   */
  public void setup() throws Exception {
    try {
      final int nbDrivers = 5;
      print(false, false, ">>> creating test configuration");
      final TestConfiguration config = createConfig("p2p_many");
      config.driver.log4j = "classes/tests/config/p2p_many/log4j-driver.properties";
      config.node.log4j = "classes/tests/config/p2p_many/log4j-node.properties";
      print(false, false, ">>> starting grid");
      BaseSetup.setup(nbDrivers, nbDrivers, false, false, config);
      print(false, false, ">>> updating  client configuration");
      final String[] drivers = new String[nbDrivers];
      for (int i=0; i<nbDrivers; i++) {
        drivers[i] = "driver" + (i + 1);
        JPPFConfiguration
          .set(JPPFProperties.PARAM_SERVER_HOST, "localhost", drivers[i])
          .set(JPPFProperties.PARAM_SERVER_PORT,   11101 + i, drivers[i])
          .set(JPPFProperties.PARAM_MAX_JOBS,              i, drivers[i]);
      }
      JPPFConfiguration.set(JPPFProperties.DRIVERS, drivers);
      print(false, false, ">>> creating client");
      client = BaseSetup.createClient(null, false, config);
      print(false, false, ">>> client config:\n%s", client.getConfig());
      print(false, false, ">>> checking drivers and nodes");
      awaitPeersInitialized(15_000L, nbDrivers);
      print(false, false, ">>> checking peers");
      checkPeers(nbDrivers, 15_000L, false, true);
    } catch (final Exception|Error e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * @throws Exception if any error occurs.
   */
  //@Test(timeout = TIMEOUT)
  @Override
  public void testMultipleJobs() throws Exception {
    setup();
    final int nbTasks = 15;
    final int nbDrivers = BaseSetup.nbDrivers();
    final int nbJobs = 10;
    client.awaitConnectionPools(Operator.AT_LEAST, nbDrivers, Operator.AT_LEAST, 1, TIMEOUT, JPPFClientConnectionStatus.workingStatuses());
    final String name = ReflectionUtils.getCurrentClassAndMethod();
    final List<JPPFJob> jobs = new ArrayList<>(nbJobs);
    for (int i=1; i<=nbJobs; i++) {
      final JPPFJob job = BaseTestHelper.createJob(name + '-' + i, false, false, nbTasks, LifeCycleTask.class, 1L);
      //job.getClientSLA().setMaxChannels(BaseSetup.nbDrivers());
      jobs.add(job);
    }
    for (final JPPFJob job: jobs) client.submitJob(job);
    for (final JPPFJob job: jobs) {
      final List<Task<?>> results = job.awaitResults();
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      for (final Task<?> task: results) {
        assertTrue("task = " + task, task instanceof LifeCycleTask);
        final Throwable t = task.getThrowable();
        assertNull("throwable for task '" + task.getId() + "' : " + ExceptionUtils.getStackTrace(t), t);
        assertNotNull(task.getResult());
        assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
      }
    }
  }
}
