/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package test.org.jppf.test.setup;

import static org.junit.Assert.*;

import java.io.NotSerializableException;
import java.util.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.Task;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.jppf.utils.ConcurrentUtils.Condition;
import org.jppf.utils.collections.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.AfterClass;

import test.org.jppf.test.setup.BaseSetup.Configuration;
import test.org.jppf.test.setup.common.*;

/**
 * Base test setup for a grid with multiple servers in p2p.
 * @author Laurent Cohen
 */
public class AbstractNonStandardSetup {
  /**
   * The jppf client to use.
   */
  protected static JPPFClient client = null;
  /**
   * 
   */
  protected static Configuration testConfig = null;

  /**
   * Create the drivers and nodes configuration.
   * @param prefix prefix to use to locate the configuration files
   * @return a {@link Configuration} instance.
   * @throws Exception if a process could not be started.
   */
  protected static Configuration createConfig(final String prefix) throws Exception {
    SSLHelper.resetConfig();
    testConfig = new Configuration();
    List<String> commonCP = new ArrayList<>();
    commonCP.add("classes/addons");
    commonCP.add("classes/tests/config");
    commonCP.add("../common/classes");
    commonCP.add("../node/classes");
    commonCP.add("../JPPF/lib/slf4j/slf4j-api-1.6.1.jar");
    commonCP.add("../JPPF/lib/slf4j/slf4j-log4j12-1.6.1.jar");
    commonCP.add("../JPPF/lib/log4j/log4j-1.2.15.jar");
    commonCP.add("../JPPF/lib/LZ4/lz4-1.3.0.jar");
    commonCP.add("../JPPF/lib/jmxremote/" + BaseSetup.JMX_REMOTE_JAR);
    commonCP.add("../JPPF/lib/ApacheCommons/commons-io-2.4.jar");
    List<String> driverCP = new ArrayList<>(commonCP);
    driverCP.add("../server/classes");
    String dir = "classes/tests/config/" + prefix;
    testConfig.driverJppf = dir + "/driver.properties";
    testConfig.driverLog4j = "classes/tests/config/log4j-driver.template.properties";
    testConfig.driverClasspath = driverCP;
    testConfig.driverJvmOptions.add("-Djava.util.logging.testConfig.file=classes/tests/config/logging-driver.properties");
    testConfig.nodeJppf = dir + "/node.properties";
    testConfig.nodeLog4j = "classes/tests/config/log4j-node.template.properties";
    testConfig.nodeClasspath = commonCP;
    testConfig.nodeJvmOptions.add("-Djava.util.logging.testConfig.file=classes/tests/config/logging-node1.properties");
    testConfig.clientConfig = dir + "/client.properties";
    return testConfig;
  }

  /**
   * Stops the driver and nodes and close the client.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    try {
      BaseSetup.cleanup();
    } finally {
      BaseSetup.resetClientConfig();
    }
  }

  /**
   * Test that a simple job is normally executed.
   * @param policy the client execution policy to set onto the job, may be null.
   * @throws Exception if any error occurs
   */
  protected void testSimpleJob(final ExecutionPolicy policy) throws Exception {
    int tasksPerNode = 5;
    int nbNodes = BaseSetup.nbNodes();
    int nbTasks = tasksPerNode * nbNodes;
    String name = ReflectionUtils.getCurrentClassAndMethod();
    JPPFJob job = BaseTestHelper.createJob(name, true, false, nbTasks, LifeCycleTask.class, 250L);
    job.getClientSLA().setExecutionPolicy(policy);
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    CollectionMap<String, Task<?>> map = new ArrayListHashMap<>();
    for (Task<?> t: results) {
      assertTrue("task = " + t, t instanceof LifeCycleTask);
      LifeCycleTask task = (LifeCycleTask) t;
      map.putValue(task.getNodeUuid(), task);
      Throwable throwable = t.getThrowable();
      assertNull("throwable for task '" + t.getId() + "' : " + ExceptionUtils.getStackTrace(throwable), throwable);
      assertNotNull(t.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, t.getResult());
    }
    System.out.println(name + " : map = " + CollectionUtils.prettyPrint(map));
    assertEquals(nbNodes, map.keySet().size());
    for (int i=0; i<nbNodes; i++) {
      String key = "n" + (i+1);
      assertTrue(map.containsKey(key));
      assertEquals(tasksPerNode, map.getValues(key).size());
    }
  }

  /**
   * Test multiple non-blocking jobs can be sent asynchronously.
   * @throws Exception if any error occurs
   */
  protected void testMultipleJobs() throws Exception {
    int tasksPerNode = 5;
    int nbNodes = BaseSetup.nbNodes();
    int nbTasks = tasksPerNode * nbNodes;
    int nbJobs = 3;
    try {
      if (client != null) client.close();
      JPPFConfiguration.set(JPPFProperties.POOL_SIZE, 2);
      client = BaseSetup.createClient(null, false);
      String name = getClass().getSimpleName() + '.' + ReflectionUtils.getCurrentMethodName();
      List<JPPFJob> jobs = new ArrayList<>(nbJobs);
      for (int i=1; i<=nbJobs; i++) jobs.add(BaseTestHelper.createJob(name + '-' + i, false, false, nbTasks, LifeCycleTask.class, 10L));
      for (JPPFJob job: jobs) client.submitJob(job);
      for (JPPFJob job: jobs) {
        List<Task<?>> results = job.awaitResults();
        assertNotNull(results);
        assertEquals(nbTasks, results.size());
        for (Task<?> task: results) {
          assertTrue("task = " + task, task instanceof LifeCycleTask);
          Throwable t = task.getThrowable();
          assertNull("throwable for task '" + task.getId() + "' : " + ExceptionUtils.getStackTrace(t), t);
          assertNotNull(task.getResult());
          assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
        }
      }
    } finally {
      if (client != null) client.close();
      client = BaseSetup.createClient(null, true, testConfig);
    }
  }

  /**
   * Test the cancellation of a job.
   * @throws Exception if any error occurs
   */
  protected void testCancelJob() throws Exception {
    int tasksPerNode = 5;
    int nbNodes = BaseSetup.nbNodes();
    int nbTasks = tasksPerNode * nbNodes;
    JPPFJob job = BaseTestHelper.createJob("TestJPPFClientCancelJob", false, false, nbTasks, LifeCycleTask.class, 1000L);
    client.submitJob(job);
    Thread.sleep(1500L);
    client.cancelJob(job.getUuid());
    List<Task<?>> results = job.awaitResults();
    assertNotNull(results);
    assertEquals("results size should be " + nbTasks + " but is " + results.size(), nbTasks, results.size());
    int count = 0;
    for (Task<?> task: results) {
      Throwable t = task.getThrowable();
      assertNull("throwable for task '" + task.getId() + "' : " + ExceptionUtils.getStackTrace(t), t);
      if (task.getResult() == null) count++;
    }
    assertTrue(count > 0);
  }

  /**
   * Test that a {@link java.io.NotSerializableException} occurring when a node returns execution results is properly handled.
   * @throws Exception if any error occurs
   */
  protected void testNotSerializableExceptionFromNode() throws Exception {
    int tasksPerNode = 5;
    int nbNodes = BaseSetup.nbNodes();
    int nbTasks = tasksPerNode * nbNodes;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, NotSerializableTask.class, false);
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (Task<?> task: results) {
      assertTrue(task instanceof NotSerializableTask);
      assertNull(task.getResult());
      assertNotNull(task.getThrowable());
      assertTrue(task.getThrowable() instanceof NotSerializableException);
    }
  }

  /**
   * Test that a task that is not Serializable still works with a custom serialization scheme (DefaultJPPFSerialization or KryoSerialization).
   * @throws Exception if any error occurs
   */
  protected void testNotSerializableWorkingInNode() throws Exception {
    int tasksPerNode = 5;
    int nbNodes = BaseSetup.nbNodes();
    int nbTasks = tasksPerNode * nbNodes;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, NotSerializableTask.class, false);
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (Task<?> task: results) {
      assertTrue(task instanceof NotSerializableTask);
      assertNotNull(task.getResult());
      assertEquals("success", task.getResult());
      assertNull(task.getThrowable());
    }
  }

  /**
   * Test that we can obtain the state of a node via the node forwarder mbean.
   * @throws Exception if any error occurs.
   */
  protected void testForwardingMBean() throws Exception {
    JMXDriverConnectionWrapper driverJmx = BaseSetup.getJMXConnection(client);
    JPPFNodeForwardingMBean nodeForwarder = driverJmx.getNodeForwarder();
    boolean ready = false;
    long elapsed = 0L;
    long start = System.nanoTime();
    while (!ready) {
      elapsed = DateTimeUtils.elapsedFrom(start);
      assertTrue((elapsed < 20_000L));
      try {
        Map<String, Object> result = nodeForwarder.state(NodeSelector.ALL_NODES);
        assertNotNull(result);
        assertEquals(BaseSetup.nbNodes(), result.size());
        for (Map.Entry<String, Object> entry: result.entrySet()) assertTrue(entry.getValue() instanceof JPPFNodeState);
        ready = true;
      } catch (Exception|AssertionError e) {
        Thread.sleep(100L);
      }
    }
    assertTrue(ready);
  }

  /**
   * Wait for the server with port = 11101 to be initialized.
   * @throws Exception if any error occurs.
   */
  protected static void awaitPeersInitialized() throws Exception {
    List<JPPFConnectionPool> pools = client.awaitConnectionPools(Operator.AT_LEAST, 2, 5000L, JPPFClientConnectionStatus.workingStatuses());
    for (JPPFConnectionPool pool: pools) {
      final JMXDriverConnectionWrapper jmx = pool.awaitWorkingJMXConnection();
      long timeout = 5000L;
      long time = System.nanoTime();
      ConcurrentUtils.awaitCondition(new Condition() {
        @Override
        public boolean evaluate() {
          try {
            return jmx.nbNodes() < 2;
          } catch (Exception e) {
            return false;
          }
        }
      }, timeout, true);
    }
  }
}
