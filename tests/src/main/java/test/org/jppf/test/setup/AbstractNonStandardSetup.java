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

package test.org.jppf.test.setup;

import static org.junit.Assert.*;
import static test.org.jppf.test.setup.TestConfiguration.JARS;
import static test.org.jppf.test.setup.TestConfiguration.getMatches;

import java.io.NotSerializableException;
import java.util.*;

import org.jppf.JPPFTimeoutException;
import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.NodeForwardingMBean;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
import org.junit.AfterClass;

import test.org.jppf.test.setup.common.*;

/**
 * Base test setup for grids with special configuration or topology.
 * @author Laurent Cohen
 */
public class AbstractNonStandardSetup extends BaseTest {
  /** */
  protected static final ExecutionPolicy PEER_POLICY = new Equal("jppf.peer.driver", true);
  /** */
  protected static final NodeSelector NON_PEER_SELECTOR = new ExecutionPolicySelector(PEER_POLICY.not());
  /** */
  protected static final NodeSelector PEER_SELECTOR = new ExecutionPolicySelector(PEER_POLICY);
  /** */
  protected static TestConfiguration testConfig;
  /** */
  protected int nbNodes;

  /**
   * Default constructor.
   */
  public AbstractNonStandardSetup() {
    nbNodes = -1;
  }

  /**
   * Create the drivers and nodes configuration.
   * @param prefix prefix to use to locate the configuration files.
   * @return a {@link TestConfiguration} instance.
   * @throws Exception if a process could not be started.
   */
  protected static TestConfiguration createConfig(final String prefix) throws Exception {
    //SSLHelper.resetConfig();
    testConfig = new TestConfiguration();
    final List<String> commonCP = new ArrayList<>();
    commonCP.add("target/classes");
    commonCP.add("target/test-classes/config");
    commonCP.add("../common/target/classes");
    commonCP.add("../node/target/classes");
    commonCP.add("../jmxremote-nio/target/classes");
    commonCP.addAll(getMatches(JARS, "*slf4j*"));
    commonCP.addAll(getMatches(JARS, "*log4j*"));
    commonCP.addAll(getMatches(JARS, "*lz4*"));
    commonCP.addAll(getMatches(JARS, "*commons-io*"));
    commonCP.addAll(getMatches(JARS, "*jna*"));
    commonCP.addAll(getMatches(JARS, "*oshi*"));
    commonCP.addAll(getMatches(JARS, "*HikariCP*"));
    final List<String> driverCP = new ArrayList<>(commonCP);
    driverCP.add("../server/target/classes");
    //final String dir = "classes/tests/config" + (prefix == null ? "" : "/" + prefix);
    String dir = CONFIG_ROOT_DIR + (prefix == null ? "" : prefix);
    dir = dir.endsWith("/") ? dir : dir + "/";
    testConfig.driver.jppf = FileUtils.getFirstExistingFilePath(dir + "driver.properties", dir + "driver.template.properties", CONFIG_ROOT_DIR + "driver.template.properties");
    testConfig.driver.log4j = FileUtils.getFirstExistingFilePath(dir + "log4j-driver.properties", dir + "log4j-driver.template.properties", CONFIG_ROOT_DIR + "log4j-driver.template.properties");
    testConfig.driver.classpath = driverCP;
    testConfig.driver.jvmOptions.add("-Djava.util.logging.configuration.file="+ CONFIG_ROOT_DIR + "logging-driver.properties");
    testConfig.node.jppf = FileUtils.getFirstExistingFilePath(dir + "node.properties", dir + "node.template.properties", CONFIG_ROOT_DIR + "node.template.properties");
    testConfig.node.log4j = FileUtils.getFirstExistingFilePath(dir + "log4j-node.properties", dir + "log4j-node.template.properties", CONFIG_ROOT_DIR + "log4j-node.template.properties");
    testConfig.node.classpath = commonCP;
    testConfig.node.jvmOptions.add("-Djava.util.logging.configuration.file="+ CONFIG_ROOT_DIR + "logging-node1.properties");
    testConfig.clientConfig = dir + "client.properties";
    return testConfig;
  }

  /**
   * Stops the driver and nodes and close the client.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanupAbstractNonStandardSetup() throws Exception {
    try {
      print("performing cleanup");
      BaseSetup.cleanup();
    } finally {
      print("performing config reset");
      BaseSetup.resetClientConfig();
    }
  }

  /**
   * Test that a simple job is normally executed.
   * @param policy the client execution policy to set onto the job, may be null.
   * @throws Exception if any error occurs
   */
  protected void testSimpleJob(final ExecutionPolicy policy) throws Exception {
    testSimpleJob(policy, "n");
  }

  /**
   * Test that a simple job is normally executed.
   * @param policy the client execution policy to set onto the job, may be null.
   * @param nodePrefix the prefix for the node uuid.
   * @throws Exception if any error occurs
   */
  protected void testSimpleJob(final ExecutionPolicy policy, final String nodePrefix) throws Exception {
    print("driver load balancing config: %s", BaseSetup.getJMXConnection(client).loadBalancerInformation());
    final int tasksPerNode = 5;
    final int nbNodes = getNbNodes();
    final int nbTasks = tasksPerNode * nbNodes;
    final String name = ReflectionUtils.getCurrentClassAndMethod();
    final JPPFJob job = BaseTestHelper.createJob(name, false, nbTasks, LifeCycleTask.class, 250L);
    job.getClientSLA().setExecutionPolicy(policy);
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    final CollectionMap<String, Task<?>> map = new ArrayListHashMap<>();
    for (final Task<?> t: results) {
      assertTrue("task = " + t, t instanceof LifeCycleTask);
      final LifeCycleTask task = (LifeCycleTask) t;
      map.putValue(task.getNodeUuid(), task);
      final Throwable throwable = t.getThrowable();
      assertNull("throwable for task '" + t.getId() + "' : " + ExceptionUtils.getStackTrace(throwable), throwable);
      assertNotNull(t.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, t.getResult());
    }
    print(false, false, "%s : map = %s", name , CollectionUtils.prettyPrint(map));
    assertEquals(nbNodes, map.keySet().size());
    for (int i=0; i<nbNodes; i++) {
      final String key = nodePrefix + (i+1);
      assertTrue(map.containsKey(key));
      assertEquals(tasksPerNode, map.getValues(key).size());
    }
  }

  /**
   * Test that multiple non-blocking jobs can be sent asynchronously.
   * @throws Exception if any error occurs
   */
  protected void testMultipleJobs() throws Exception {
    final int tasksPerNode = 5;
    final int nbNodes = getNbNodes();
    final int nbTasks = tasksPerNode * nbNodes;
    final int nbJobs = 3;
    print("waiting for workking connection pool");
    final JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
    try {
      print("settting pool size to 2");
      pool.setSize(2);
      print("waiting for 2 working connections in pool");
      pool.awaitConnections(Operator.EQUAL, 2, 5000L, JPPFClientConnectionStatus.workingStatuses());
      assertEquals(2, pool.getConnections(JPPFClientConnectionStatus.workingStatuses()).size()); 
      final String name = getClass().getSimpleName() + '.' + ReflectionUtils.getCurrentMethodName();
      final List<JPPFJob> jobs = new ArrayList<>(nbJobs);
      for (int i=1; i<=nbJobs; i++) jobs.add(BaseTestHelper.createJob(name + '-' + i, false, nbTasks, LifeCycleTask.class, 10L));
      print("submitting %d jobs", nbJobs);
      for (final JPPFJob job: jobs) client.submitAsync(job);
      for (final JPPFJob job: jobs) {
        print("getitng results for job %s", job.getName());
        final List<Task<?>> results = job.awaitResults();
        print("got results for job %s", job.getName());
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
    } finally {
      print("settting pool size to 2");
      pool.setSize(1);
      print("waiting for 2 working connection in pool");
      pool.awaitConnections(Operator.EQUAL, 1, 5000L, JPPFClientConnectionStatus.workingStatuses());
      final int n = pool.getConnections(JPPFClientConnectionStatus.workingStatuses()).size();
      print("got %d working connection(s) in pool", n);
      assertEquals(1, n); 
    }
  }

  /**
   * Test the cancellation of a job.
   * @throws Exception if any error occurs
   */
  protected void testCancelJob() throws Exception {
    final int tasksPerNode = 5;
    final int nbNodes = getNbNodes();
    final int nbTasks = tasksPerNode * nbNodes;
    final JPPFJob job = BaseTestHelper.createJob("TestJPPFClientCancelJob", false, nbTasks, LifeCycleTask.class, 1000L);
    client.submitAsync(job);
    Thread.sleep(1500L);
    client.cancelJob(job.getUuid());
    final List<Task<?>> results = job.awaitResults();
    assertNotNull(results);
    assertEquals("results size should be " + nbTasks + " but is " + results.size(), nbTasks, results.size());
    int count = 0;
    for (final Task<?> task: results) {
      final Throwable t = task.getThrowable();
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
    final int tasksPerNode = 5;
    final int nbNodes = getNbNodes();
    final int nbTasks = tasksPerNode * nbNodes;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, NotSerializableTask.class, false);
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (final Task<?> task: results) {
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
    final int tasksPerNode = 5;
    final int nbNodes = getNbNodes();
    final int nbTasks = tasksPerNode * nbNodes;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, NotSerializableTask.class, false);
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (final Task<?> task: results) {
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
    final JMXDriverConnectionWrapper driverJmx = BaseSetup.getJMXConnection(client);
    final NodeForwardingMBean nodeForwarder = driverJmx.getForwarder();
    assertTrue(ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> {
      final ResultsMap<String, JPPFNodeState> result = nodeForwarder.state(NodeSelector.ALL_NODES);
      return (result != null) && (result.size() == getNbNodes());
    }, 5_000L, 100L, false));
  }

  /**
   * Wait for 2 servers with port = 11101 and 11102 to be initialized with at least one idle node attached.
   * @param maxWait the maximum time to wait for completion of this method.
   * @throws Exception if any error occurs.
   */
  protected static void awaitPeersInitialized(final long maxWait) throws Exception {
    awaitPeersInitialized(maxWait, 2);
  }

  /**
   * Wait for 2 servers with port = 11101 and 11102 to be initialized with at least one idle node attached.
   * @param maxWait the maximum time to wait for completion of this method.
   * @param nbDrivers number of drivers to expect.
   * @throws Exception if any error occurs.
   */
  protected static void awaitPeersInitialized(final long maxWait, final int nbDrivers) throws Exception {
    final long start = System.currentTimeMillis();
    long timeout = maxWait;
    print(false, false, ">>> awaiting %d pools", nbDrivers);
    final List<JPPFConnectionPool> pools = client.awaitConnectionPools(Operator.AT_LEAST, nbDrivers, Operator.AT_LEAST, 1, timeout, JPPFClientConnectionStatus.workingStatuses());
    if (pools.size() < nbDrivers) fail("timeout of " + timeout + " ms waiting for " + nbDrivers + " pools expired");
    final List<JMXDriverConnectionWrapper> jmxList = new ArrayList<>(nbDrivers);
    for (final JPPFConnectionPool pool: pools) {
      print(false, false, ">>> awaiting JMX connection for %s", pool);
      final MutableReference<JMXDriverConnectionWrapper> jmx = new MutableReference<>();
      timeout = maxWait - (System.currentTimeMillis() - start);
      if (timeout < 0L) throw new JPPFTimeoutException("execeeded maxWait timeout of " + maxWait + " ms");
      ConcurrentUtils.awaitCondition(() -> {
        final JMXDriverConnectionWrapper driver = pool.getJmxConnection(true);
        if (driver == null) return false;
        jmx.set(driver);
        return true;
      }, timeout, 500L, true);
      print(false, false, ">>> got JMX connection %s", jmx.get());
      jmxList.add(jmx.get());
    }
    for (final JMXDriverConnectionWrapper jmx: jmxList) {
      print(false, false, ">>> awaiting 1 idle node for %s", jmx);
      timeout = maxWait - (System.currentTimeMillis() - start);
      if (timeout < 0L) throw new JPPFTimeoutException("execeeded maxWait timeout of " + maxWait + " ms");
      awaitNbIdleNodes(jmx, Operator.EQUAL, 1, timeout);
      print(false, false, ">>> got 1 idle node for %s", jmx);
    }
  }

  /**
   * Wait for 2 servers with port = 11101 and 11102 to be initialized with at least one idle node attached.
   * @param maxWait the maximum time to wait for completion of this method.
   * @param secure whether to use SSL connections.
   * @throws Exception if any error occurs.
   */
  protected static void checkPeers(final long maxWait, final boolean secure) throws Exception {
    checkPeers(maxWait, secure, false);
  }

  /**
   * Wait for 2 servers with port = 11101 and 11102 to be initialized with at least one idle node attached.
   * @param maxWait the maximum time to wait for completion of this method.
   * @param secure whether to use SSL connections.
   * @param checkPeers whether to check peer driver connections.
   * @throws Exception if any error occurs.
   */
  protected static void checkPeers(final long maxWait, final boolean secure, final boolean checkPeers) throws Exception {
    checkPeers(2, maxWait, secure, checkPeers);
  }

  /**
   * Wait for 2 servers with port = 11101 and 11102 to be initialized with at least one idle node attached.
   * @param nbDrivers the number of peer drivers to check.
   * @param maxWait the maximum time to wait for completion of this method.
   * @param secure whether to use SSL connections.
   * @param checkPeers whether to check peer driver connections.
   * @throws Exception if any error occurs.
   */
  protected static void checkPeers(final int nbDrivers, final long maxWait, final boolean secure, final boolean checkPeers) throws Exception {
    final long start = System.currentTimeMillis();
    long timeout = maxWait;
    print(false, false, "$$ creating %d JMX connections", nbDrivers);
    final JMXDriverConnectionWrapper[] jmxArray = new JMXDriverConnectionWrapper[nbDrivers];
    final int base = secure ? SSL_DRIVER_MANAGEMENT_PORT_BASE : DRIVER_MANAGEMENT_PORT_BASE;
    for (int i=0; i<nbDrivers; i++) jmxArray[i] = new JMXDriverConnectionWrapper("localhost", base + i + 1, secure);
    try {
      for (final JMXDriverConnectionWrapper jmx: jmxArray) jmx.connect();
      for (final JMXDriverConnectionWrapper jmx: jmxArray) {
        print(false, false, "$$ awaiting JMX connection for %s", jmx);
        timeout = maxWait - (System.currentTimeMillis() - start);
        if (timeout <= 0L) throw new JPPFTimeoutException("exceeded timeout of " + maxWait + " ms");
        ConcurrentUtils.awaitCondition(() -> jmx.isConnected(), timeout, 500L, true);
        print(false, false, "$$ got JMX connection %s", jmx);
      }
      for (final JMXDriverConnectionWrapper jmx: jmxArray) {
        print(false, false, "$$ awaiting 1 idle node for %s", jmx);
        timeout = maxWait - (System.currentTimeMillis() - start);
        if (timeout <= 0L) throw new JPPFTimeoutException("exceeded timeout of " + maxWait + " ms");
        awaitNbIdleNodes(jmx, Operator.EQUAL, 1, timeout);
        print(false, false, "$$ got 1 idle node for %s", jmx);
      }
      if (checkPeers) {
        for (final JMXDriverConnectionWrapper jmx: jmxArray) {
          print(false, false, "$$ awaiting %d peer drivers for %s", nbDrivers - 1, jmx);
          timeout = maxWait - (System.currentTimeMillis() - start);
          if (timeout <= 0L) throw new JPPFTimeoutException("exceeded timeout of " + maxWait + " ms");
          awaitNbIdleNodes(jmx, PEER_SELECTOR, checkPeers, Operator.EQUAL, nbDrivers - 1, timeout);
          print(false, false, "$$ got %d peer driver connections for %s", nbDrivers - 1, jmx);
        }
      }
    } finally {
      //BaseSetup.generateDriverThreadDump(jmxArray);
      for (final JMXDriverConnectionWrapper jmx: jmxArray) {
        if (jmx.isConnected()) jmx.close();
      }
    }
  }

  /**
   * Wait for the specified driver to have a number of idle nodes that satisfy the specified condition.
   * @param jmx the JMX connection to the driver.
   * @param operator the comparison operator that defines the condition to evaluate.
   * @param nbNodes the expected number of idle nodes to satisfy the comparison.
   * @param timeout how long to wait for the condtion to be {@link true}.
   * @throws Exception if any error occurs or the tiemout expires.
   */
  protected static void awaitNbIdleNodes(final JMXDriverConnectionWrapper jmx, final ComparisonOperator operator, final int nbNodes, final long timeout) throws Exception {
    awaitNbIdleNodes(jmx, NON_PEER_SELECTOR, false, operator, nbNodes, timeout);
  }

  /**
   * Wait for the specified driver to have a number of idle peer drivers that satisfy the specified condition.
   * @param jmx the JMX connection to the driver.
   * @param selector the node selector to use.
   * @param includePeers whether to include peers in the query.
   * @param operator the comparison operator that defines the condition to evaluate.
   * @param nbNodes the expected number of idle nodes to satisfy the comparison.
   * @param timeout how long to wait for the condtion to be {@link true}.
   * @throws Exception if any error occurs or the tiemout expires.
   */
  protected static void awaitNbIdleNodes(final JMXDriverConnectionWrapper jmx, final NodeSelector selector, final boolean includePeers,
    final ComparisonOperator operator, final int nbNodes, final long timeout) throws Exception {
    ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> operator.evaluate(jmx.nbIdleNodes(selector, includePeers), nbNodes), timeout, 500L, true);
  }

  /**
   * @return the number of nodes in the topology.
   */
  protected int getNbNodes() {
    return (nbNodes >= 0) ? nbNodes : BaseSetup.nbNodes();
  }
}
