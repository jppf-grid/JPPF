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

package test.org.jppf.job.persistence;

import static org.junit.Assert.*;

import java.io.File;
import java.util.*;

import org.jppf.client.*;
import org.jppf.job.JobSelector;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ReflectionUtils;
import org.junit.*;

import test.org.jppf.persistence.AbstractDatabaseSetup;
import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Test database job persistence. 
 * @author Laurent Cohen
 */
public class TestDefaultFilePersistenceP2P extends AbstractDatabaseSetup {
  /**
   * Starts the DB server and create the database with a test table.
   * @throws Exception if any error occurs.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final String prefix = "job_persistence_p2p";
    final TestConfiguration config = createConfig(prefix);
    config.driverLog4j = "classes/tests/config/" + prefix + "/log4j-driver.properties";
    client = BaseSetup.setup(2, 2, true, true, config);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @After
  public void tearDownInstance() throws Exception {
    for (int i=1; i<=BaseSetup.nbDrivers(); i++) {
      try (final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", 11200 + i, false)) {
        jmx.connectAndWait(5_000L);
        final boolean b = jmx.isConnected();
        print(false, false, "tearDownInstance() for driver %d : jmx connected = %b", i, b);
        if (b) {
          final JPPFDriverJobPersistence mgr = new JPPFDriverJobPersistence(jmx);
          mgr.deleteJobs(JobSelector.ALL_JOBS);
        }
      }
    }
  }

  /**
   * Test that a persisted job is only persisted in the driver to which it is directly submitted, and not in any peer driver.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testJobNotPersistedInPeer() throws Exception {
    final int nbTasks = 10;
    final String method = ReflectionUtils.getCurrentMethodName();
    final JPPFJob job = BaseTestHelper.createJob(method, false, false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().getPersistenceSpec().setPersistent(true).setAutoExecuteOnRestart(false).setDeleteOnCompletion(false);
    job.getClientSLA().setExecutionPolicy(new Equal("jppf.server.port", 11101));
    client.submitJob(job);
    final List<Task<?>> results = job.awaitResults();
    showDirContent("persistence1/" + job.getUuid());
    showDirContent("persistence2/" + job.getUuid());
    checkJobResults(nbTasks, results, false);
    // check that tasks were dispatched to both drivers and attached nodes
    final Set<String> set = new HashSet<>();
    for (final Task<?> task: results) {
      assertTrue(task instanceof LifeCycleTask);
      final LifeCycleTask lct = (LifeCycleTask) task;
      if (!set.contains(lct.getNodeUuid())) set.add(lct.getNodeUuid());
    }
    assertEquals(2, set.size());
    try (final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", 11201)) {
      jmx.connectAndWait(5000L);
      assertTrue(jmx.isConnected());
      final JPPFDriverJobPersistence mgr = new JPPFDriverJobPersistence(jmx);
      final List<String> uuids = mgr.listJobs(JobSelector.ALL_JOBS);
      assertNotNull(uuids);
      assertEquals(1, uuids.size());
      assertEquals(job.getUuid(), uuids.get(0));
      assertTrue(mgr.isJobComplete(job.getUuid()));
      final JPPFJob job2 = mgr.retrieveJob(job.getUuid());
      compareJobs(job, job2, true);
      checkJobResults(nbTasks, job2.getResults().getAllResults(), false);
      assertTrue(mgr.deleteJob(job.getUuid()));
    }
    try (final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", 11202)) {
      jmx.connectAndWait(5000L);
      assertTrue(jmx.isConnected());
      final JPPFDriverJobPersistence mgr = new JPPFDriverJobPersistence(jmx);
      final List<String> uuids = mgr.listJobs(JobSelector.ALL_JOBS);
      assertNotNull(uuids);
      assertTrue(uuids.isEmpty());
    }
  }

  /**
   * @param path .
   * @throws Exception if any error occurs.
   */
  private static void showDirContent(final String path) throws Exception {
    final File dir = new File(path);
    final List<String> list = new ArrayList<>();
    if (dir.exists()) {
      final File[] files = dir.listFiles();
      for (final File file: files) list.add(file.getName());
    }
    print(true, false, "content of dir '%s': %s", path, list);
  }
}
