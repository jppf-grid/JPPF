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

package test.org.jppf.test.setup;

import static org.junit.Assert.*;

import java.io.NotSerializableException;
import java.util.*;

import org.jppf.JPPFTimeoutException;
import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.Task;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.jppf.utils.concurrent.*;
import org.junit.AfterClass;

import test.org.jppf.test.setup.common.*;

/**
 * Base test setup for a grid with multiple servers in p2p.
 * @author Laurent Cohen
 */
public class AbstractNonStandardSetup extends BaseTest {
  /** */
  protected static final NodeSelector NON_PEER_SELECTOR = new ExecutionPolicySelector(new Equal("jppf.peer.driver", false));
  /** */
  protected static final NodeSelector PEER_SELECTOR = new ExecutionPolicySelector(new Equal("jppf.peer.driver", true));
  /**
   * 
   */
  protected static TestConfiguration testConfig = null;

  /**
   * Create the drivers and nodes configuration.
   * @param prefix prefix to use to locate the configuration files.
   * @return a {@link TestConfiguration} instance.
   * @throws Exception if a process could not be started.
   */
  protected static TestConfiguration createConfig(final String prefix) throws Exception {
    SSLHelper.resetConfig();
    testConfig = new TestConfiguration();
    final List<String> commonCP = new ArrayList<>();
    commonCP.add("classes/addons");
    commonCP.add("classes/tests/config");
    commonCP.add("../common/classes");
    commonCP.add("../node/classes");
    commonCP.add("../JPPF/lib/slf4j/slf4j-api-1.6.1.jar");
    commonCP.add("../JPPF/lib/slf4j/slf4j-log4j12-1.6.1.jar");
    commonCP.add("../JPPF/lib/log4j/log4j-1.2.15.jar");
    commonCP.add("../JPPF/lib/LZ4/lz4-1.3.0.jar");
    commonCP.add("../jmxremote/classes");
    commonCP.add("../jmxremote-nio/classes");
    commonCP.add("../JPPF/lib/ApacheCommons/commons-io-2.4.jar");
    final List<String> driverCP = new ArrayList<>(commonCP);
    driverCP.add("../server/classes");
    final String dir = "classes/tests/config" + (prefix == null ? "" : "/" + prefix);
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
    testSimpleJob(policy, "n");
  }

  /**
   * Test that a simple job is normally executed.
   * @param policy the client execution policy to set onto the job, may be null.
   * @param nodePrefix the prefix for the node uuid.
   * @throws Exception if any error occurs
   */
  protected void testSimpleJob(final ExecutionPolicy policy, final String nodePrefix) throws Exception {
    System.out.printf("driver load balancing config: %s%n", BaseSetup.getJMXConnection(client).loadBalancerInformation());
    final int tasksPerNode = 5;
    final int nbNodes = getNbNodes();
    final int nbTasks = tasksPerNode * nbNodes;
    final String name = ReflectionUtils.getCurrentClassAndMethod();
    final JPPFJob job = BaseTestHelper.createJob(name, true, false, nbTasks, LifeCycleTask.class, 250L);
    job.getClientSLA().setExecutionPolicy(policy);
    final List<Task<?>> results = client.submitJob(job);
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
    BaseTest.printOut("%s : map = %s", name , CollectionUtils.prettyPrint(map));
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
    final JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
    try {
      pool.setSize(2);
      pool.awaitConnections(Operator.EQUAL, 2, 5000L, JPPFClientConnectionStatus.workingStatuses());
      assertEquals(2, pool.getConnections(JPPFClientConnectionStatus.workingStatuses()).size()); 
      final String name = getClass().getSimpleName() + '.' + ReflectionUtils.getCurrentMethodName();
      final List<JPPFJob> jobs = new ArrayList<>(nbJobs);
      for (int i=1; i<=nbJobs; i++) jobs.add(BaseTestHelper.createJob(name + '-' + i, false, false, nbTasks, LifeCycleTask.class, 10L));
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
    } finally {
      pool.setSize(1);
      pool.awaitConnections(Operator.EQUAL, 1, 5000L, JPPFClientConnectionStatus.workingStatuses());
      assertEquals(1, pool.getConnections(JPPFClientConnectionStatus.workingStatuses()).size()); 
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
    final JPPFJob job = BaseTestHelper.createJob("TestJPPFClientCancelJob", false, false, nbTasks, LifeCycleTask.class, 1000L);
    client.submitJob(job);
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
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, NotSerializableTask.class, false);
    final List<Task<?>> results = client.submitJob(job);
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
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, NotSerializableTask.class, false);
    final List<Task<?>> results = client.submitJob(job);
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
    final JPPFNodeForwardingMBean nodeForwarder = driverJmx.getNodeForwarder();
    boolean ready = false;
    long elapsed = 0L;
    final long start = System.nanoTime();
    while (!ready) {
      elapsed = DateTimeUtils.elapsedFrom(start);
      assertTrue((elapsed < 20_000L));
      try {
        final Map<String, Object> result = nodeForwarder.state(NodeSelector.ALL_NODES);
        assertNotNull(result);
        assertEquals(getNbNodes(), result.size());
        for (final Map.Entry<String, Object> entry: result.entrySet()) assertTrue(entry.getValue() instanceof JPPFNodeState);
        ready = true;
      } catch (@SuppressWarnings("unused") final Exception|AssertionError e) {
        Thread.sleep(100L);
      }
    }
    assertTrue(ready);
  }

  /**
   * Wait for 2 servers with port = 11101 and 11102 to be initialized with at least one idle node attached.
   * @param maxWait the maximum time to wait for completion of this method.
   * @throws Exception if any error occurs.
   */
  protected static void awaitPeersInitialized(final long maxWait) throws Exception {
    final long start = System.currentTimeMillis();
    long timeout = maxWait;
    print(false, false, ">>> awaiting 2 pools");
    final List<JPPFConnectionPool> pools = client.awaitConnectionPools(Operator.AT_LEAST, 2, Operator.AT_LEAST, 1, timeout, JPPFClientConnectionStatus.workingStatuses());
    if (pools.size() < 2) fail("timeout of " + timeout + " ms waiting for 2 pools expired");
    final List<JMXDriverConnectionWrapper> jmxList = new ArrayList<>(2);
    for (final JPPFConnectionPool pool: pools) {
      print(false, false, ">>> awaiting JMX connection for %s", pool);
      final MutableReference<JMXDriverConnectionWrapper> jmx = new MutableReference<>();
      timeout = maxWait - (System.currentTimeMillis() - start);
      if (timeout < 0L) throw new JPPFTimeoutException("execeeded maxWait timeout of " + maxWait + " ms");
      ConcurrentUtils.awaitCondition(new ConcurrentUtils.Condition() {
        @Override
        public boolean evaluate() {
          final JMXDriverConnectionWrapper driver = pool.getJmxConnection(true);
          if (driver == null) return false;
          jmx.set(driver);
          return true;
        }
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
  }

  /**
   * Wait for 2 servers with port = 11101 and 11102 to be initialized with at least one idle node attached.
   * @param maxWait the maximum time to wait for completion of this method.
   * @param secure whether to use SSL connections.
   * @param checkPeers whether to check peer driver connections.
   * @throws Exception if any error occurs.
   */
  protected static void checkPeers(final long maxWait, final boolean secure, final boolean checkPeers) throws Exception {
    final long start = System.currentTimeMillis();
    long timeout = maxWait;
    print(false, false, "$$ creating 2 JMX connections");
    final JMXDriverConnectionWrapper[] jmxArray = new JMXDriverConnectionWrapper[2];
    final int base = secure ? SSL_DRIVER_MANAGEMENT_PORT_BASE : DRIVER_MANAGEMENT_PORT_BASE;
    for (int i=0; i<2; i++) jmxArray[i] = new JMXDriverConnectionWrapper("localhost", base + i + 1, secure);
    try {
      for (final JMXDriverConnectionWrapper jmx: jmxArray) jmx.connect();
      for (final JMXDriverConnectionWrapper jmx: jmxArray) {
        print(false, false, "$$ awaiting JMX connection for %s", jmx);
        timeout = maxWait - (System.currentTimeMillis() - start);
        if (timeout <= 0L) throw new JPPFTimeoutException("execeeded maxWait timeout of " + maxWait + " ms");
        ConcurrentUtils.awaitCondition(new ConcurrentUtils.Condition() {
          @Override
          public boolean evaluate() {
            final boolean b = jmx.isConnected();
            return b;
          }
        }, timeout, 500L, true);
        print(false, false, "$$ got JMX connection %s", jmx);
      }
      for (final JMXDriverConnectionWrapper jmx: jmxArray) {
        print(false, false, "$$ awaiting 1 idle node for %s", jmx);
        timeout = maxWait - (System.currentTimeMillis() - start);
        if (timeout <= 0L) throw new JPPFTimeoutException("execeeded maxWait timeout of " + maxWait + " ms");
        awaitNbIdleNodes(jmx, Operator.EQUAL, 1, timeout);
        print(false, false, "$$ got 1 idle node for %s", jmx);
      }
      if (checkPeers) {
        for (final JMXDriverConnectionWrapper jmx: jmxArray) {
          print(false, false, ">>> awaiting 1 peer driver for %s", jmx);
          timeout = maxWait - (System.currentTimeMillis() - start);
          if (timeout <= 0L) throw new JPPFTimeoutException("execeeded maxWait timeout of " + maxWait + " ms");
          awaitNbIdleNodes(jmx, PEER_SELECTOR, checkPeers, Operator.EQUAL, 1, timeout);
          print(false, false, "$$ got peer driver connection for %s", jmx);
        }
      }
    } finally {
      BaseSetup.generateDriverThreadDump(jmxArray);
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
  protected static void awaitNbIdleNodes(final JMXDriverConnectionWrapper jmx, final Operator operator, final int nbNodes, final long timeout) throws Exception {
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
    final Operator operator, final int nbNodes, final long timeout) throws Exception {
    ConcurrentUtils.awaitCondition(new ConcurrentUtils.Condition() {
        @Override
        public boolean evaluate() {
          try {
            return operator.evaluate(jmx.nbIdleNodes(selector, includePeers), nbNodes);
          } catch (@SuppressWarnings("unused") final Exception e) {
            return false;
          }
        }
      }, timeout, 500L, true);
  }
  /**
   * @return the number of nodes in the topology.
   */
  protected int getNbNodes() {
    return BaseSetup.nbNodes();
  }
}
