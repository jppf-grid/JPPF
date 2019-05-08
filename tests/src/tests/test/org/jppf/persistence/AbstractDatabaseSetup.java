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

package test.org.jppf.persistence;

import static org.junit.Assert.*;

import java.io.File;
import java.sql.*;
import java.util.*;

import org.apache.log4j.Level;
import org.h2.tools.*;
import org.jppf.client.*;
import org.jppf.load.balancer.persistence.LoadBalancerPersistenceManagement;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.Task;
import org.jppf.persistence.JPPFDatasourceFactory;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
import org.junit.AfterClass;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * Base test class for unit tests using a database.
 * @author Laurent Cohen
 */
public abstract class AbstractDatabaseSetup extends AbstractNonStandardSetup {
  /**
   * Database name.
   */
  public static final String DB_NAME = "tests_jppf";
  /**
   * Connection URL.
   */
  public static final String DB_URL = "jdbc:h2:tcp://localhost:9092/./root/" + DB_NAME;
  /**
   * Database user.
   */
  public static final String DB_USER = "sa";
  /**
   * Database password.
   */
  public static final String DB_PWD = "";
  /**
   * JDBC driver class name.
   */
  public static final String DB_DRIVER_CLASS = "org.h2.Driver";
  /**
   * Name of the test table.
   */
  public static final String TABLE_NAME = "TEST_TABLE";
  /**
   * The database server.
   */
  protected static Server h2Server;

  /**
   * Create and start a H2 server, and create the configuration for the test.
   * @param prefix prefix to use to locate the configuration files.
   * @return a {@link TestConfiguration} instance.
   * @throws Exception if a process could not be started.
   */
  protected static TestConfiguration dbSetup(final String prefix) throws Exception {
    return dbSetup(prefix, true);
  }

  /**
   * Create and start a H2 server, and create the configuration for the test.
   * @param prefix prefix to use to locate the configuration files.
   * @param useDB whether to actuallu start and use a DB server.
   * @return a {@link TestConfiguration} instance.
   * @throws Exception if a process could not be started.
   */
  protected static TestConfiguration dbSetup(final String prefix, final boolean useDB) throws Exception {
    BaseSetup.setLoggerLevel(Level.DEBUG, "org.jppf.persistence");
    if (useDB) {
      print(false, false, "starting H2 server");
      h2Server = Server.createTcpServer().start();
      print(false, false, "H2 server started, creating table");
      // create the test table
      Class.forName(DB_DRIVER_CLASS);
      try (final Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PWD)) {
        final String sql = FileUtils.readTextFile(AbstractDatabaseSetup.class.getPackage().getName().replace('.', '/') + "/create_table.sql");
        try (final PreparedStatement ps = c.prepareStatement(sql)) {
          ps.executeUpdate();
        }
      }
      print(false, false, "table created");
    }
    final TestConfiguration config = createConfig(prefix);
    final String dir = "classes/tests/config" + (prefix == null ? "" : "/" + prefix);
    config.driver.log4j = dir + "/log4j-driver.properties";
    config.node.log4j = dir + "/log4j-node.properties";
    config.driver.classpath.add("lib/h2.jar");
    return config;
  }

  /**
   * Stops the DB server and delete the database.
   * @throws Exception if the server could not be started.
   */
  @AfterClass
  public static void dbTeardown() throws Exception {
    try {
      BaseSetup.generateClientThreadDump();
      BaseSetup.generateDriverThreadDump(client);
      JPPFDatasourceFactory.getInstance().clear();
      if (h2Server != null) {
        // generate sql dump file of the database
        print(false, false, "generating SQL dump");
        dumpDatabase("h2dump.log");
        print(false, false, "stopping H2 server");
        h2Server.stop();
        print(false, false, "H2 server stopped, deleting database");
        // delete the entire database
        FileUtils.deletePath(new File("./root"), true);
        print(false, false, "database deleted");
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Check the results of a job's execution.
   * @param nbTasks the number of tasks in the job.
   * @param results the execution results to check.
   * @param cancelled whether the job was cancelled.
   * @throws Exception if any error occurs.
   */
  protected void checkJobResults(final int nbTasks, final Collection<Task<?>> results, final boolean cancelled) throws Exception {
    checkJobResults(nbTasks, results, cancelled, null);
  }

  /**
   * Check the results of a job's execution.
   * @param nbTasks the number of tasks in the job.
   * @param results the execution results to check.
   * @param cancelled whether the job was cancelled.
   * @param resultFunction a function which computes the expected result for a task, can be {@code null} in which case the result is expected to be {@link BaseTestHelper#EXECUTION_SUCCESSFUL_MESSAGE}.
   * @throws Exception if any error occurs.
   */
  protected void checkJobResults(final int nbTasks, final Collection<Task<?>> results, final boolean cancelled, final ExpectedResultFunction resultFunction) throws Exception {
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (final Task<?> task: results) {
      assertNotNull(task);
      final Throwable t = task.getThrowable();
      assertNull(String.format("task '%s' has a throwable: %s", task.getId(), (t == null) ? "none" : ExceptionUtils.getMessage(t)), t);
      if (!cancelled) {
        final Object expectedResult = (resultFunction == null) ? BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE : resultFunction.getExpectedResult(task);
        assertNotNull(String.format("task %s has a null result", task.getId()), expectedResult);
        assertEquals(expectedResult, task.getResult());
      }
    }
  }

  /**
   * @return whether the persistence being tested is an asynchronous wrapper for laod-balancer persistence, in which case job results checking should wait for a given time before checking,
   * to give persistence enough time to complete.
   */
  protected boolean isAsyncLoadBalancerPersistence() {
    return false;
  }

  /**
   * Create a jmx connection independent of the specified client.
   * @param client .
   * @return a {@link JMXDriverConnectionWrapper}.
   * @throws Exception if any error occurs.
   */
  protected JMXDriverConnectionWrapper newJmx(final JPPFClient client) throws Exception {
    final JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
    final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper(pool.getDriverHost(), pool.getJmxPort(), pool.isSslEnabled());
    jmx.connectAndWait(10_000L);
    return jmx;
  }

  /**
   * @param job .
   * @param job2 .
   * @param checkResults .
   * @throws Exception if any error occurs.
   */
  protected void compareJobs(final JPPFJob job, final JPPFJob job2, final boolean checkResults) throws Exception {
    assertNotNull(job);
    assertNotNull(job2);
    assertEquals(job.getUuid(), job2.getUuid());
    assertEquals(job.getName(), job2.getName());
    assertEquals(job.getTaskCount(), job2.getTaskCount());
    if (checkResults) assertEquals(job.getResults().size(), job2.getResults().size());
  }

  /**
   * Generate a database dump.
   * @param filepath the path to the dump file.
   * @throws Exception if any error occurs.
   */
  protected static void dumpDatabase(final String filepath) throws Exception {
    if (h2Server != null) Script.main("-url", DB_URL, "-user", DB_USER, "-password", DB_PWD, "-script", filepath);
  }

  /**
   * Check whether the list of load-balancer states is empty.
   * @param mgt the load-balancer state manager.
   * @return {@code true} if the list of channel states is empty, {@code false} otherwise.
   */
  protected boolean checkEmptyChannels(final LoadBalancerPersistenceManagement mgt) {
    final ConcurrentUtils.ConditionFalseOnException cond = () -> {
      final List<String> channels = mgt.listAllChannels();
      return (channels != null) && channels.isEmpty();
    };
    return ConcurrentUtils.awaitCondition(cond, 5000L, 500L, false);
  }

  /**
   * Check whether the list of load-balancer states for the specified algorithm is empty.
   * @param mgt the load-balancer state manager.
   * @param algo load-balancer algorithm for which to check.
   * @return {@code true} if the list of channel states is empty, {@code false} otherwise.
   */
  protected boolean checkEmptyChannelsForAlgo(final LoadBalancerPersistenceManagement mgt, final String algo) {
    final ConcurrentUtils.ConditionFalseOnException cond = () -> {
      final List<String> channels = mgt.listAllChannelsWithAlgorithm(algo);
      return (channels != null) && channels.isEmpty();
    };
    return ConcurrentUtils.awaitCondition(cond, 5000L, 500L, false);
  }

  /**
   * Wait until the persistence has no more pending operation, or the timeout expires, whichever happens first.
   * @param mgt the load-balancer state manager.
   */
  protected void awaitNoMorePendingOperations(final LoadBalancerPersistenceManagement mgt) {
    ConcurrentUtils.awaitCondition((ConditionFalseOnException) (() -> mgt.getUncompletedOperations() <= 0), 5000L, 100L, false);
  }

  /** */
  @FunctionalInterface
  public static interface ExpectedResultFunction {
    /**
     * Get the task result.
     * @param task the task from which to get the result.
     * @return the task result.
     */
    Object getExpectedResult(Task<?> task);
  }
}
