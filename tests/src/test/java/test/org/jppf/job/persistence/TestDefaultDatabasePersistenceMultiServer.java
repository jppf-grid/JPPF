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

package test.org.jppf.job.persistence;

import static org.junit.Assert.*;

import java.sql.*;
import java.util.*;

import javax.sql.DataSource;

import org.jppf.client.*;
import org.jppf.job.JobSelector;
import org.jppf.job.persistence.PersistenceObjectType;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.Task;
import org.jppf.persistence.JPPFDatasourceFactory;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.*;

import test.org.jppf.persistence.AbstractDatabaseSetup;
import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Test database job persistenc in a multi server topology where drivers do not comunnicate with each other
 * and all drivers point to the same database for jobs persistence. 
 * @author Laurent Cohen
 */
public class TestDefaultDatabasePersistenceMultiServer extends AbstractDatabaseSetup {
  /**
   * 
   */
  protected static DataSource datasource = null;

  /**
   * Starts the DB server and create the database with a test table.
   * @throws Exception if any error occurs.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final String prefix = "job_persistence_p2p_db";
    final TestConfiguration config = dbSetup(prefix);
    final TypedProperties props = new TypedProperties().setString("scope", "local").setString("driverClassName", DB_DRIVER_CLASS)
      .setString("jdbcUrl", DB_URL).setString("username", DB_USER).setString("password", DB_PWD)
      .setInt("minimumIdle", 1).setInt("maximumPoolSize", 5);
    datasource = JPPFDatasourceFactory.getInstance().createDataSource("jobDS", props);
    final String path = JPPFConfiguration.get(JPPFProperties.JOB_PERSISTENCE_DDL_LOCATION);
    final String ddl = FileUtils.readTextFile(path).replace("${table}", "TEST1");
    try (final Connection c = datasource.getConnection()) {
      try (final PreparedStatement ps = c.prepareStatement(ddl)) {
        ps.executeUpdate();
      }
    }
    client = BaseSetup.setup(2, 2, true, true, config);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @After
  public void tearDownInstance() throws Exception {
    JPPFDatasourceFactory.getInstance().clear();
    for (int i=1; i<=BaseSetup.nbDrivers(); i++) {
      try (final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", DRIVER_MANAGEMENT_PORT_BASE + i, false)) {
        jmx.connectAndWait(5_000L);
        final boolean b = jmx.isConnected();
        print(false, false, "tearDownInstance() for driver %d : jmx connected = %b", i, b);
        if (b) {
          try {
            final JPPFDriverJobPersistence mgr = new JPPFDriverJobPersistence(jmx);
            mgr.deleteJobs(JobSelector.ALL_JOBS);
          } catch (final Exception e) {
            if (!BaseSetup.isTestWithEmbeddedGrid()) {
              print(false, false, "--- error in test:\n%s", ExceptionUtils.getStackTrace(e));
              throw e;
            } else {
              print(false, false, "--- expected error in embedded grid: %s", ExceptionUtils.getMessage(e));
            }
          }
        }
      }
    }
  }

  /**
   * Test that a job persisted partially by each of 2 drivers can still be retrieved fully after execution.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testJobPersistedInAllDrivers() throws Exception {
    print("waiting for 2 pools with 1 connection each");
    final List<JPPFConnectionPool> pools = client.awaitConnectionPools(Operator.AT_LEAST, 2, Operator.AT_LEAST, 1, 5000L, JPPFClientConnectionStatus.workingStatuses());
    final List<Integer> maxJobs = new ArrayList<>(pools.size()); 
    try {
      pools.forEach(pool -> {
        maxJobs.add(pool.getMaxJobs());
        pool.setMaxJobs(1);
      });
      final int nbTasks = 10;
      final String method = ReflectionUtils.getCurrentMethodName();
      final JPPFJob job = BaseTestHelper.createJob(method, false, nbTasks, LifeCycleTask.class, 0L);
      job.getSLA().getPersistenceSpec().setPersistent(true).setAutoExecuteOnRestart(false).setDeleteOnCompletion(false);
      job.getClientSLA().setMaxChannels(2);
      print("submitting job");
      final List<Task<?>> results = client.submit(job);
      print("checking job results");
      checkJobResults(nbTasks, results, false);
      // check that tasks were dispatched to both drivers and attached nodes
      final Set<String> set = new HashSet<>();
      for (final Task<?> task: results) {
        assertTrue(task instanceof LifeCycleTask);
        final LifeCycleTask lct = (LifeCycleTask) task;
        if (!set.contains(lct.getNodeUuid())) set.add(lct.getNodeUuid());
      }
      assertEquals(2, set.size());
      print("checking number of persisted jobs in the DB");
      final ConcurrentUtils.Condition cond = (ConcurrentUtils.ConditionFalseOnException) () -> nbTasks == queryNbResults(job.getUuid());
      ConcurrentUtils.awaitCondition(cond, 5000L, 500L, true);
      print("before job check, number of results = %d", queryNbResults(job.getUuid()));
      for (int i=1; i<=2; i++) {
        try (final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", DRIVER_MANAGEMENT_PORT_BASE + i)) {
          print("testing driver %d", i);
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
          if (i == 2) {
            print("after job check, number of results = %d", queryNbResults(job.getUuid()));
            assertTrue(mgr.deleteJob(job.getUuid()));
          }
        }
      }
    } catch(final Exception e) {
      print("error in test:\n%s", ExceptionUtils.getStackTrace(e));
      throw e;
    } finally {
      for (int i=0; i<pools.size(); i++) pools.get(i).setMaxJobs(maxJobs.get(i));
    }
  }

  /**
   * 
   * @param uuid the job uuid.
   * @return the numvber of task results for the specified job.
   * @throws Exception if any error occurs.
   */
  private static int queryNbResults(final String uuid) throws Exception {
    try (final Connection c = datasource.getConnection()) {
      final String sql = "SELECT COUNT(*) FROM TEST1 WHERE UUID = ? AND TYPE = ?";
      try (final PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, uuid);
        ps.setString(2, PersistenceObjectType.TASK_RESULT.name());
        try (final ResultSet rs = ps.executeQuery()) {
          if (rs.next()) return rs.getInt(1);
          throw new IllegalStateException("count of " + PersistenceObjectType.TASK_RESULT + " returned no result");
        }
      }
    }
  }
}
