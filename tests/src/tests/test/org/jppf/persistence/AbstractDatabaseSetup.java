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

package test.org.jppf.persistence;

import static org.junit.Assert.*;

import java.io.File;
import java.sql.*;
import java.util.Collection;

import org.apache.log4j.Level;
import org.h2.tools.*;
import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.Task;
import org.jppf.persistence.JPPFDatasourceFactory;
import org.jppf.utils.*;
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
    config.driverClasspath.add("lib/h2.jar");
    return config;
  }

  /**
   * Stops the DB server and delete the database.
   * @throws Exception if the server could not be started.
   */
  @AfterClass
  public static void dbTeardown() throws Exception {
    BaseSetup.generateClientThreadDump();
    BaseSetup.generateDriverThreadDump(BaseSetup.getClient());
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
  }

  /**
   * Check the results of a job's execution.
   * @param nbTasks the number of tasks in the job.
   * @param results the execution results to check.
   * @param cancelled whether the job was cancelled.
   * @throws Exception if any error occurs.
   */
  protected void checkJobResults(final int nbTasks, final Collection<Task<?>> results, final boolean cancelled) throws Exception {
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (final Task<?> task: results) {
      assertNotNull(task);
      final Throwable t = task.getThrowable();
      assertNull(String.format("task '%s' has a throwable: %s", task.getId(), (t == null) ? "none" : ExceptionUtils.getMessage(t)), t);
      if (!cancelled) {
        assertNotNull(String.format("task %s has a null result", task.getId()), task.getResult());
        assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
      }
    }
  }

  /**
   * Create a jmx connection not independent of the specified client.
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
}
