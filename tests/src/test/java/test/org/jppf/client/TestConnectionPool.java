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

package test.org.jppf.client;

import static org.jppf.utils.configuration.JPPFProperties.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.discovery.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for <code>JPPFClient</code> using multiple connections to the same server
 * (connection pool size > 1).
 * @author Laurent Cohen
 */
public class TestConnectionPool extends Setup1D1N {
  /**
   * Reset the confiugration.
   * @throws Exception if any error occurs
   */
  @After
  public void reset() throws Exception {
    if (client != null) {
      client.close();
      client = null;
    }
    JPPFConfiguration.reset();
  }

  /**
   * Test job submission with <code>jppf.pool.size = 2</code> and local execution disabled.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testSubmitJobMultipleConnections() throws Exception {
    configure(0);
    client = BaseSetup.createClient(null, false, BaseSetup.DEFAULT_CONFIG);
    final int nbTasks = 100;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class, 0L);
    final List<Task<?>> results = client.submit(job);
    testJobResults(nbTasks, results);
  }

  /**
   * Test job submission with <code>jppf.pool.size = 2</code> and local execution enabled.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testSubmitJobMultipleConnectionsAndLocalExec() throws Exception {
    configure(2);
    client = BaseSetup.createClient(null, false, BaseSetup.DEFAULT_CONFIG);
    final int nbTasks = 100;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class, 0L);
    final List<Task<?>> results = client.submit(job);
    testJobResults(nbTasks, results);
  }

  /**
   * Test job submission with <code>jppf.pool.size = 2</code> and getMachChannels() > 1.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testSubmitJobMultipleRemoteChannels() throws Exception {
    configure(0);
    client = BaseSetup.createClient(null, false, BaseSetup.DEFAULT_CONFIG);
    while (client.getAllConnectionsCount() < 2) Thread.sleep(10L);
    final int nbTasks = 100;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class, 0L);
    // default max channels is 1 for backward compatibility with previous versions of the client.
    job.getClientSLA().setMaxChannels(10);
    final List<Task<?>> results = client.submit(job);
    testJobResults(nbTasks, results);
  }

  /**
   * Check that the expected number of pools have been created.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testNumberOfPools() throws Exception {
    BaseSetup.resetClientConfig();
    client = BaseSetup.createClient(null, false, BaseSetup.DEFAULT_CONFIG);
    BaseSetup.checkDriverAndNodesInitialized(client, 1, 1, false);
    final List<JPPFConnectionPool> pools = client.getConnectionPools();
    assertNotNull(pools);
    assertEquals(1, pools.size());
  }

  /**
   * Test a sequence of {@link JPPFConnectionPool#setSize(int)} calls.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 15000)
  public void testSetPoolSizeByAPI() throws Exception {
    print(false, false, "creating client");
    client = BaseSetup.createClient(null, false, BaseSetup.DEFAULT_CONFIG);
    for (int i=1; i<=10; i++) {
      print(false, false, "iteration #%d awaiting working pool", i);
      final JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
      try {
        print(false, false, "setting pool size to 2, current size is %d", pool.getSize());
        pool.setSize(2);
        print(false, false, "waiting for 2 working connections");
        pool.awaitWorkingConnections(Operator.EQUAL, 2);
        assertEquals(2, pool.getSize());
      } finally {
        print(false, false, "setting pool size to 1, current size is %d", pool.getSize());
        pool.setSize(1);
        print(false, false, "waiting for 1 working connection");
        pool.awaitWorkingConnections(Operator.EQUAL, 1);
        assertEquals(1, pool.getSize());
      }
    }
  }

  /**
   * Configure the client for a connection pool.
   * @param localThreads a value greater than 0 to enable local execution with this number of threads, 0 or less otherwise.
   * @throws Exception if any error occurs
   */
  private static void configure(final int localThreads) throws Exception {
    final TypedProperties config = BaseSetup.resetClientConfig();
    config.set(DISCOVERY_ENABLED, false)
      .set(LOAD_BALANCING_ALGORITHM, "proportional")
      .set(LOAD_BALANCING_PROFILE, "test")
      .setInt(LOAD_BALANCING_PROFILE.getName() + ".test.initialSize", 10)
      .setInt("driver1." + POOL_SIZE.getName(), 2);
    if (localThreads > 0) config.set(LOCAL_EXECUTION_ENABLED, true).set(LOCAL_EXECUTION_THREADS, localThreads);
    else config.set(LOCAL_EXECUTION_ENABLED, false);
  }

  /**
   * Test a sequence of {@link JPPFConnectionPool#setSize(int)} calls.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testPoolPriority() throws Exception {
    JPPFConfiguration.set(DISCOVERY_ENABLED, false).set(REMOTE_EXECUTION_ENABLED, false).set(LOCAL_EXECUTION_ENABLED, false);
    final String methodName = ReflectionUtils.getCurrentMethodName();
    try (final JPPFClient client = new JPPFClient()) {
      BaseTestHelper.printToServersAndNodes(client, true, true, "start of method %s()", methodName);
      final SimpleDiscovery discovery = new SimpleDiscovery();
      client.addDriverDiscovery(discovery);
      discovery.emitPool("pool1", 10);
      discovery.emitPool("pool2", 1);
      awaitConnections(client, null, Operator.AT_LEAST, 2);
      awaitIdleConnections(client, null, Operator.AT_LEAST, 2);
      print(false, false, ">>> testing jobs with pool1");
      testJobsInPool(client, "pool1", methodName);
      while (client.awaitConnectionPools(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE).size() < 2) Thread.sleep(10L);
      final JMXDriverConnectionWrapper jmx = client.findConnectionPool("pool2").awaitWorkingJMXConnection();
      final JPPFConnectionPool pool = client.findConnectionPool("pool1");
      assertNotNull(pool);
      BaseTestHelper.printToAll(jmx, true, true, true, false, false, ">>> closing pool1");
      pool.close();
      awaitConnections(client, jmx, Operator.AT_MOST, 1);
      awaitIdleConnections(client, jmx, Operator.AT_MOST, 1);
      print(false, false, ">>> testing jobs with pool2");
      testJobsInPool(client, "pool2", methodName);
      while (client.awaitConnectionPools(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE).size() < 1) Thread.sleep(10L);
      discovery.emitPool("pool1", 10);
      awaitConnections(client, jmx, Operator.AT_LEAST, 2);
      awaitIdleConnections(client, null, Operator.AT_LEAST, 2);
      print(false, false, ">>> testing jobs with pool1 (again)");
      testJobsInPool(client, "pool1", methodName);
    }
  }

  /**
   * Test a sequence of {@link JPPFConnectionPool#setSize(int)} calls.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 50000)
  public void testPoolPriority2() throws Exception {
    final String methodName = ReflectionUtils.getCurrentMethodName();
    BaseTestHelper.printToServersAndNodes(client, true, true, "start of method %s()", methodName);
    JPPFConfiguration.set(DISCOVERY_ENABLED, false).set(REMOTE_EXECUTION_ENABLED, false).set(LOCAL_EXECUTION_ENABLED, false);
    try (final JPPFClient client = new JPPFClient()) {
      final SimpleDiscovery discovery = new SimpleDiscovery();
      client.addDriverDiscovery(discovery);
      discovery.emitPool("pool1", 10);
      discovery.emitPool("pool2", 1);
      awaitConnections(client, null, Operator.AT_LEAST, 2);
      final JMXDriverConnectionWrapper jmx = client.findConnectionPool("pool2").awaitWorkingJMXConnection();
      for (int i=1; i<=50; i++) {
        BaseTestHelper.printToAll(jmx, true, true, true, false, false, ">>> start of iteration #%d", i);
        while (client.awaitConnectionPools(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE).size() < 2) Thread.sleep(10L);
        final JPPFConnectionPool pool = client.findConnectionPool("pool1");
        assertNotNull(pool);
        BaseTestHelper.printToAll(jmx, true, true, true, false, false, ">>> closing pool1");
        pool.close();
        awaitConnections(client, jmx, Operator.AT_MOST, 1);
        while (client.awaitConnectionPools(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE).size() < 1) Thread.sleep(10L);
        discovery.emitPool("pool1", 10);
        awaitConnections(client, jmx, Operator.AT_LEAST, 2);
      }
    }
  }

  /**
   * @param client .
   * @param poolName .
   * @param prefix .
   * @throws Exception if any error occurs
   */
  private static void testJobsInPool(final JPPFClient client, final String poolName, final String prefix) throws Exception {
    final int nbJobs = 5;
    final List<JPPFJob> jobs = new ArrayList<>(nbJobs);
    final MyJobListener listener = new MyJobListener();
    for (int i=1; i<=nbJobs; i++) {
      final JPPFJob job = BaseTestHelper.createJob(prefix + "-" + i, false, 1, LifeCycleTask.class, 0L);
      job.addJobListener(listener);
      jobs.add(job);
    }
    for (final JPPFJob job: jobs) {
      print(false, false, "submitting job %s", job);
      client.submitAsync(job);
    }
    for (final JPPFJob job: jobs) {
      final List<Task<?>> result = job.awaitResults();
      print(false, false, "testing results for job %s", job);
      testJobResults(1, result);
      final String name = listener.jobToPool.get(job.getUuid());
      assertNotNull(name);
      assertEquals(poolName, name);
    }
  }

  /**
   * Test the results of a job execution.
   * @param nbTasks the expected number of tasks in the results.
   * @param results the results.
   * @throws Exception if any error occurs.
   */
  private static void testJobResults(final int nbTasks, final List<Task<?>> results) throws Exception {
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    final int count = 0;
    for (final Task<?> task : results) {
      final String prefix = "task " + count + " ";
      final Throwable t = task.getThrowable();
      assertNull(prefix + "has an exception", t);
      assertNotNull(prefix + "result is null", task.getResult());
    }
  }

  /**
   * Await for the specified number of pools to be working and available.
   * @param client the client used to lookup the pools.
   * @param jmx jmx connection to the driver.
   * @param operator a condition on the number of pools to wait for.
   * @param nbPools the number of pools on which to apply the condition.
   * @throws Exception if any error occurs.
   */
  private static void awaitConnections(final JPPFClient client, final JMXDriverConnectionWrapper jmx, final ComparisonOperator operator, final int nbPools) throws Exception {
    if (jmx == null) print(false, false, "waiting for %s %d working pools", operator, nbPools);
    else BaseTestHelper.printToAll(jmx, true, true, true, false, false, "waiting for %s %d working pools", operator, nbPools);
    final List<JPPFConnectionPool> list = client.awaitConnectionPools(operator, nbPools, Operator.EQUAL, 1, 5000L, JPPFClientConnectionStatus.workingStatuses());
    if (!operator.evaluate(list.size(), nbPools)) throw new IllegalStateException(String.format("failed to obtain %s %d pools (got %d)", operator, nbPools, list.size()));
  }

  /**
   * Await for the specified number of connections to be idle.
   * @param client the client used to lookup the pools.
   * @param jmx jmx connection to the driver.
   * @param operator a condition on the number of pools to wait for.
   * @param expected the expected number of idle connections on which to apply the condition.
   * @throws Exception if any error occurs.
   */
  private static void awaitIdleConnections(final JPPFClient client, final JMXDriverConnectionWrapper jmx, final ComparisonOperator operator, final int expected) throws Exception {
    if (jmx == null) print(false, false, "waiting for %s %d idle connections", operator, expected);
    else BaseTestHelper.printToAll(jmx, true, true, true, false, false, "waiting for %s %d idle connections", operator, expected);
    ConcurrentUtils.awaitCondition(new ConcurrentUtils.Condition() {
      @Override
      public boolean evaluate() {
        return operator.evaluate(client.nbIdleCOnnections(), expected);
      }
    }, 5000L, 100L, true);
  }

  /** */
  static class MyJobListener extends JobListenerAdapter {
    /**
     * Mapping of job uuid to the name of the pool to which it is dispatched.
     */
    public Map<String, String> jobToPool = new ConcurrentHashMap<>();

    @Override
    public void jobDispatched(final JobEvent event) {
      final JPPFJob job = event.getJob();
      final JPPFConnectionPool pool = event.getConnection().getConnectionPool();
      print(false, false, "job %s disptached to %s", job.getName(), pool);
      jobToPool.put(job.getUuid(), pool.getName());
    }
  }

  /** */
  public class SimpleDiscovery extends ClientDriverDiscovery {
    /** */
    private final LinkedBlockingQueue<ClientConnectionPoolInfo> queue = new LinkedBlockingQueue<>();
    /** */
    private boolean stopped = false;

    @Override
    public synchronized void discover() throws InterruptedException {
      while (!stopped) {
        ClientConnectionPoolInfo info;
        while ((info = queue.poll()) != null) {
          BaseTest.print(false, false, "found new connection pool %s", info);
          newConnection(info);
        }
        BaseTest.print(false, false, "SimpleDiscovery  about to wait in discover()");
        wait();
      }
    }

    /**
     * "Discover" the pool with the specified name and priority.
     * @param name the connection pool name.
     * @param priority the connection pool priority.
     */
    public synchronized void emitPool(final String name, final int priority) {
      print(false, false, ">>> emitting %s with priority %d", name, priority);
      queue.offer(new ClientConnectionPoolInfo(name, false, "localhost", 11101, priority, 1, 1, false, 1));
      notifyAll();
    }

    @Override
    public synchronized void shutdown() {
      stopped = true;
      notifyAll();
    }
  }
}
