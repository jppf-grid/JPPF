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

import static org.jppf.persistence.JPPFDatasourceFactory.Scope.*;
import static org.junit.Assert.*;

import java.util.*;

import javax.sql.DataSource;

import org.jppf.client.JPPFJob;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.*;
import org.jppf.persistence.JPPFDatasourceFactory;
import org.jppf.utils.*;
import org.junit.*;

import com.zaxxer.hikari.HikariDataSource;

import test.org.jppf.test.runner.IgnoreForEmbeddedGrid;
import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * Unit test {@link JPPFDatasourceFactory}.
 * @author Laurent Cohen
 */
@IgnoreForEmbeddedGrid
public class TestJPPFDatasourceFactory extends AbstractDatabaseSetup {
  /**
    * Starts the DB server and create the database with a test table.
    * @throws Exception if any error occurs.
    */
  @BeforeClass
  public static void setup() throws Exception {
    final TestConfiguration config = dbSetup("persistence");
    final StringBuilder sb = new StringBuilder();
    config.driver.classpath.forEach(path -> sb.append("\n").append(path));
    print(false, false, "driver classpath:%s", sb);
    config.driver.log4j = CONFIG_ROOT_DIR + "persistence/log4j-driver.properties";
    config.node.log4j = CONFIG_ROOT_DIR + "persistence/log4j-node.properties";
    client = BaseSetup.setup(1, 2, true, true, config);
  }

   /**
    * Test a single data source definiton via a set of configId-prefixed properties, using internal APIs.
    * @throws Exception if any error occurs
    */
   @Test(timeout = 10000)
   public void testSimpleDataSourcePrefixedDefinition() throws Exception {
     final TypedProperties props = configureDataSource(new TypedProperties(), "testDS", "myConfigId", "local");
     print("datasource config:\n  %s", props.asString("\n  "));
     final JPPFDatasourceFactory factory = JPPFDatasourceFactory.getInstance();
     factory.configure(props, JPPFDatasourceFactory.Scope.LOCAL);
     final DataSource ds = factory.getDataSource("testDS");
     checkHikariProperties("testDS", ds);
     List<String> names = factory.getDataSourceNames();
     print("datasource names: %s", names);
     assertNotNull(names);
     assertEquals(1, names.size());
     assertEquals("testDS", names.get(0));
     assertTrue(factory.removeDataSource("testDS"));
     assertNull(factory.getDataSource("testDS"));
     names = factory.getDataSourceNames();
     assertNotNull(names);
     assertTrue(names.isEmpty());
   }
  
   /**
    * Test a single datasource definiton via a set of properties, using public APIs.
    * @throws Exception if any error occurs
    */
   @Test(timeout = 10000)
   public void testSimpleDataSourcePublicAPI() throws Exception {
     final TypedProperties props = configureDataSource(new TypedProperties(), "testDS", null, "local");
     final JPPFDatasourceFactory factory = JPPFDatasourceFactory.getInstance();
     final DataSource ds = factory.createDataSource("testDS", props);
     checkHikariProperties("testDS", ds);
     List<String> names = factory.getDataSourceNames();
     assertNotNull(names);
     assertEquals(1, names.size());
     assertEquals("testDS", names.get(0));
     assertTrue(factory.removeDataSource("testDS"));
     assertNull(factory.getDataSource("testDS"));
     names = factory.getDataSourceNames();
     assertNotNull(names);
     assertTrue(names.isEmpty());
   }
  
   /**
    * Test a creating multiple datasources at once using {@link JPPFDatasourceFactory#createDataSources(java.util.Properties) createDataSources()}.
    * @throws Exception if any error occurs
    */
   @Test(timeout = 10000)
   public void testMultipleDataSourcesPublicAPI() throws Exception {
     final int nbDS = 3;
     TypedProperties props = new TypedProperties();
     for (int i=1; i<=nbDS; i++) props = configureDataSource(props, "testDS_" + i, "test_" + i, "local");
     final JPPFDatasourceFactory factory = JPPFDatasourceFactory.getInstance();
     final Map<String, DataSource> map = factory.createDataSources(props);
     assertNotNull(map);
     assertEquals(nbDS, map.size());
     List<String> names = factory.getDataSourceNames();
     assertNotNull(names);
     assertEquals(nbDS, names.size());
     for (int i=1; i<=nbDS; i++) {
       final String name = "testDS_" + i;
       assertTrue(map.containsKey(name));
       final DataSource ds = map.get(name);
       checkHikariProperties(name, ds);
       assertTrue(names.contains(name));
     }
     for (int i=1; i<=nbDS; i++) {
       final String name = "testDS_" + i;
       assertTrue(factory.removeDataSource(name));
       assertNull(factory.getDataSource(name));
     }
     names = factory.getDataSourceNames();
     assertNotNull(names);
     assertTrue(names.isEmpty());
   }
  
   /**
    * Test the creation and retrieval of datasources in all possible scopes.
    * @throws Exception if any error occurs
    */
   @Test(timeout = 10000)
   public void testDataSourceScopes() throws Exception {
     final TypedProperties props = new TypedProperties();
     for (final JPPFDatasourceFactory.Scope scope: JPPFDatasourceFactory.Scope.values()) {
       final String scopeName = scope.name();
       configureDataSource(props, scopeName, scopeName + "_id", scopeName);
     }
     final JPPFDatasourceFactory factory = JPPFDatasourceFactory.getInstance();
     factory.configure(props, LOCAL);
     List<String> names = factory.getDataSourceNames();
     assertNotNull(names);
     assertEquals(2, names.size());
     assertTrue(names.contains(LOCAL.name()));
     assertTrue(names.contains(ANY.name()));
     factory.clear();
     names = factory.getDataSourceNames();
     assertNotNull(names);
     assertTrue(names.isEmpty());
     factory.configure(props, REMOTE);
     names = factory.getDataSourceNames();
     assertNotNull(names);
     assertEquals(2, names.size());
     assertTrue(names.contains(REMOTE.name()));
     assertTrue(names.contains(ANY.name()));
     factory.clear();
     names = factory.getDataSourceNames();
     assertNotNull(names);
     assertTrue(names.isEmpty());
   }
  
   /**
    * Test performing various SQL statements against a defined datasource.
    * @throws Exception if any error occurs.
    */
   @Test(timeout = 10000)
   public void testSQLStatements() throws Exception {
     final String dsName = "testDS";
     final TypedProperties props = configureDataSource(new TypedProperties(), dsName, null, "local");
     final JPPFDatasourceFactory factory = JPPFDatasourceFactory.getInstance();
     final DataSource ds = factory.createDataSource(dsName, props);
     checkHikariProperties("testDS", ds);
     final DBTask task = new DBTask(dsName, "h2dump_" + ReflectionUtils.getCurrentMethodName() + ".log");
     task.run();
     assertNull(task.getThrowable());
     assertNotNull(task.getResult());
     assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
     assertTrue(factory.removeDataSource(dsName));
     assertTrue(factory.getDataSourceNames().isEmpty());
   }
  
   /**
    * Test performing various SQL statements against a defined datasource.
    * @throws Exception if any error occurs.
    */
   @Test(timeout = 10000)
   public void testSQLStatementsFromTask() throws Exception {
     final String method = ReflectionUtils.getCurrentMethodName();
     final String[] dsNames = { "commonDS", "nodeDS" };
     for (final String dsName: dsNames) {
       final String name = method + "_" + dsName;
       final JPPFJob job = BaseTestHelper.createJob(name, false, 1, DBTask.class, dsName, "h2dump_" + name + ".log");
       final List<Task<?>> results = client.submit(job);
       for (final Task<?> task: results) {
         throwUnknown(task.getThrowable());
         assertNull(task.getThrowable());
         assertNotNull(task.getResult());
         assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
       }
     }
   }
  
   /**
    * Test performing various SQL statements against a defined datasource.
    * @throws Exception if any error occurs.
    */
   @Test(timeout = 10000)
   public void testDatasourceExecutionPolicy() throws Exception {
     final String method = ReflectionUtils.getCurrentMethodName();
     for (int i = 1; i <= BaseSetup.nbNodes(); i++) {
       final JPPFJob job = BaseTestHelper.createJob(method + "_" + i, false, 1, DSInfoTask.class);
       final String uuid = "n" + i;
       job.getSLA().setExecutionPolicy(new Equal("jppf.uuid", false, uuid));
       final List<Task<?>> results = client.submit(job);
       assertNotNull(results);
       assertEquals(1, results.size());
       final DSInfoTask task = (DSInfoTask) results.get(0);
       assertNull(task.getThrowable());
       assertNotNull(task.nodeUuid);
       assertEquals(uuid, task.nodeUuid);
       assertNotNull(task.dsNames);
       assertTrue(task.dsNames.contains("node" + i + "DS"));
       assertTrue(task.dsNames.contains("nodeDS"));
       assertTrue(task.dsNames.contains("commonDS"));
     }
   }
  
   /**
    * Check the provided datasource is a HikariDataSource and that it has the proper configuration properties.
    * @param name the name of the datasource.
    * @param ds the datasource to check.
    * @throws Exception if any error occurs.
    */
   static void checkHikariProperties(final String name, final DataSource ds) throws Exception {
     assertNotNull(ds);
     assertTrue(ds instanceof HikariDataSource);
     final HikariDataSource hds = (HikariDataSource) ds;
     assertEquals(name, hds.getPoolName());
     assertEquals(DataSourceConstants.DB_DRIVER_CLASS, hds.getDriverClassName());
     assertEquals(DataSourceConstants.DB_URL, hds.getJdbcUrl());
     assertEquals(DataSourceConstants.DB_USER, hds.getUsername());
     assertEquals(DataSourceConstants.DB_PWD, hds.getPassword());
   }
  
   /**
    * Configure the specified datasource in the specified properties.
    * @param props the container for the properties to set.
    * @param name the name of the datasource to define.
    * @param configId the configId of the datasource to define.
    * @param scope the scope of the datasource ddefinition, if {@code null} then "local" is used..
    * @return the provideed properties.
    * @throws Exception if any error occurs.
    */
   private static TypedProperties configureDataSource(final TypedProperties props, final String name, final String configId, final String scope) throws Exception {
     final String prefix = configId == null ? "" : "jppf.datasource." + configId + ".";
     props.setString(prefix + "name", name).setString(prefix + "scope", (scope == null) ? "local" : scope)
       .setString(prefix + "driverClassName", DataSourceConstants.DB_DRIVER_CLASS).setString(prefix + "jdbcUrl", DataSourceConstants.DB_URL)
       .setString(prefix + "username", DataSourceConstants.DB_USER).setString(prefix + "password", DataSourceConstants.DB_PWD);
     return props;
   }
  
   /**
    * A simple task that captures the names of available datasources on the node on which it executes.
    */
   public static class DSInfoTask extends AbstractTask<String> {
     /**
      * Explicit serialVersionUID.
      */
     private static final long serialVersionUID = 1L;
     /** */
     List<String> dsNames;
     /** */
     String nodeUuid;

     @Override
     public void run() {
       dsNames = JPPFDatasourceFactory.getInstance().getDataSourceNames();
       nodeUuid = getNode().getUuid();
     }
   }
 }
