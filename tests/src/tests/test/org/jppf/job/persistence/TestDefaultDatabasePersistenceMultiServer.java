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
    String prefix = "job_persistence_p2p_db";
    TestConfiguration config = dbSetup(prefix);
    TypedProperties props = new TypedProperties().setString("scope", "local").setString("driverClassName", DB_DRIVER_CLASS)
      .setString("jdbcUrl", DB_URL).setString("username", DB_USER).setString("password", DB_PWD)
      .setInt("minimumIdle", 1).setInt("maximumPoolSize", 5);
    datasource = JPPFDatasourceFactory.getInstance().createDataSource("jobDS", props);
    String path = JPPFConfiguration.get(JPPFProperties.JOB_PERSISTENCE_DDL_LOCATION);
    String ddl = FileUtils.readTextFile(path).replace("${table}", "TEST1");
    try (Connection c = datasource.getConnection()) {
      try (PreparedStatement ps = c.prepareStatement(ddl)) {
        ps.executeUpdate();
      }
    }
    config.driverLog4j = "classes/tests/config/" + prefix + "/log4j-driver.properties";
    client = BaseSetup.setup(2, 2, true, true, config);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @After
  public void tearDownInstance() throws Exception {
    JPPFDatasourceFactory.getInstance().clear();
    for (int i=1; i<=BaseSetup.nbDrivers(); i++) {
      try (JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", 11200 + i, false)) {
        jmx.connectAndWait(5_000L);
        boolean b = jmx.isConnected();
        print(false, false, "tearDownInstance() for driver %d : jmx connected = %b", i, b);
        if (b) {
          JPPFDriverJobPersistence mgr = new JPPFDriverJobPersistence(jmx);
          mgr.deleteJobs(JobSelector.ALL_JOBS);
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
    final int nbTasks = 10;
    String method = ReflectionUtils.getCurrentMethodName();
    final JPPFJob job = BaseTestHelper.createJob(method, true, false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().getPersistenceSpec().setPersistent(true).setAutoExecuteOnRestart(false).setDeleteOnCompletion(false);
    job.getClientSLA().setMaxChannels(2);
    List<Task<?>> results = client.submitJob(job);
    checkJobResults(nbTasks, results, false);
    // check that tasks were dispatched to both drivers and attached nodes
    Set<String> set = new HashSet<>();
    for (Task<?> task: results) {
      assertTrue(task instanceof LifeCycleTask);
      LifeCycleTask lct = (LifeCycleTask) task;
      if (!set.contains(lct.getNodeUuid())) set.add(lct.getNodeUuid());
    }
    assertEquals(2, set.size());
    ConcurrentUtils.Condition cond = new ConcurrentUtils.Condition() {
      @Override
      public boolean evaluate() {
        try {
          return nbTasks == queryNbResults(job.getUuid());
        } catch (@SuppressWarnings("unused") Exception e) {
          return false;
        }
      }
    };
    ConcurrentUtils.awaitInterruptibleCondition(cond, 5000L, true);
    print(false, false, "before job check, number of results = %d", queryNbResults(job.getUuid()));
    for (int i=1; i<=2; i++) {
      try (JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", 11200 + i)) {
        print(false, false, "testing driver %d", i);
        jmx.connectAndWait(5000L);
        assertTrue(jmx.isConnected());
        JPPFDriverJobPersistence mgr = new JPPFDriverJobPersistence(jmx);
        List<String> uuids = mgr.listJobs(JobSelector.ALL_JOBS);
        assertNotNull(uuids);
        assertEquals(1, uuids.size());
        assertEquals(job.getUuid(), uuids.get(0));
        assertTrue(mgr.isJobComplete(job.getUuid()));
        JPPFJob job2 = mgr.retrieveJob(job.getUuid());
        compareJobs(job, job2, true);
        checkJobResults(nbTasks, job2.getResults().getAllResults(), false);
        if (i == 2) {
          print(false, false, "after job check, number of results = %d", queryNbResults(job.getUuid()));
          assertTrue(mgr.deleteJob(job.getUuid()));
        }
      }
    }
  }

  /**
   * 
   * @param uuid the job uuid.
   * @return the numvber of task results for the specified job.
   * @throws Exception if any error occurs.
   */
  private static int queryNbResults(final String uuid) throws Exception {
    try (Connection c = datasource.getConnection()) {
      String sql = "SELECT COUNT(*) FROM TEST1 WHERE UUID = ? AND TYPE = ?";
      try (PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, uuid);
        ps.setString(2, PersistenceObjectType.TASK_RESULT.name());
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) return rs.getInt(1);
          throw new IllegalStateException("count of " + PersistenceObjectType.TASK_RESULT + " returned no result");
        }
      }
    }
  }
}
