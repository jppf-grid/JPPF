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

package test.org.jppf.client;

import static org.jppf.utils.configuration.JPPFProperties.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.*;
import org.jppf.client.balancer.JobManagerClient;
import org.jppf.client.event.*;
import org.jppf.discovery.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
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
   * The JPPF client.
   */
  private static JPPFClient client = null;

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
    client = BaseSetup.createClient(null, false);
    int nbTasks = 100;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 0L);
    List<Task<?>> results = client.submitJob(job);
    testJobResults(nbTasks, results);
  }

  /**
   * Test job submission with <code>jppf.pool.size = 2</code> and local execution enabled.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testSubmitJobMultipleConnectionsAndLocalExec() throws Exception {
    configure(2);
    client = BaseSetup.createClient(null, false);
    int nbTasks = 100;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 0L);
    List<Task<?>> results = client.submitJob(job);
    testJobResults(nbTasks, results);
  }

  /**
   * Test job submission with <code>jppf.pool.size = 2</code> and getMachChannels() > 1.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testSubmitJobMultipleRemoteChannels() throws Exception {
    configure(0);
    client = BaseSetup.createClient(null, false);
    while (client.getAllConnectionsCount() < 2) Thread.sleep(10L);
    int nbTasks = 100;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 0L);
    // default max channels is 1 for backward compatibility with previous versions of the client.
    job.getClientSLA().setMaxChannels(10);
    List<Task<?>> results = client.submitJob(job);
    testJobResults(nbTasks, results);
  }

  /**
   * Check that the expected number of pools have been created.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testNumberOfPools() throws Exception {
    client = BaseSetup.createClient(null, false);
    BaseSetup.checkDriverAndNodesInitialized(client, 1, 1);
    List<JPPFConnectionPool> pools = client.getConnectionPools();
    assertNotNull(pools);
    assertEquals(1, pools.size());
  }

  /**
   * Test a sequence of {@link JPPFConnectionPool#setSize(int)} calls.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testSetPoolSizeByAPI() throws Exception {
    client = BaseSetup.createClient(null, false);
    for (int i=1; i<=10; i++) {
      JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
      try {
        pool.setSize(2);
        pool.awaitWorkingConnections(Operator.EQUAL, 2);
        assertEquals(2, pool.getSize());
      } finally {
        pool.setSize(1);
        pool.awaitWorkingConnections(Operator.EQUAL, 1);
        assertEquals(1, pool.getSize());
      }
    }
  }

  /**
   * Configure the client for a connection pool.
   * @param localThreads a value greater than 0 to enable local execution with this number of threads, 0 or less otherwise.
   */
  private void configure(final int localThreads) {
    JPPFConfiguration
      .set(LOAD_BALANCING_ALGORITHM, "proportional")
      .set(LOAD_BALANCING_PROFILE, "test")
      .setInt(LOAD_BALANCING_PROFILE.getName() + ".test.initialSize", 10)
      .setInt("driver1." + POOL_SIZE.getName(), 2);
    if (localThreads > 0) JPPFConfiguration.set(LOCAL_EXECUTION_ENABLED, true).set(LOCAL_EXECUTION_THREADS, localThreads);
  }

  /**
   * Test a sequence of {@link JPPFConnectionPool#setSize(int)} calls.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testPoolPriority() throws Exception {
    JPPFConfiguration.set(DISCOVERY_ENABLED, false).set(REMOTE_EXECUTION_ENABLED, false).set(LOCAL_EXECUTION_ENABLED, false);
    String methodName = ReflectionUtils.getCurrentMethodName();
    try (JPPFClient client = new JPPFClient()) {
      BaseTestHelper.printToServersAndNodes(client, true, true, "start of method %s()", methodName);
      SimpleDiscovery discovery = new SimpleDiscovery();
      client.addDriverDiscovery(discovery);
      discovery.emitPool("pool1", 10);
      discovery.emitPool("pool2", 1);
      awaitConnections(client, Operator.AT_LEAST, 2);
      testJobsInPool(client, "pool1", methodName);
      // trigger close of pool1
      JPPFConnectionPool pool = client.findConnectionPool("pool1");
      assertNotNull(pool);
      JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) pool.awaitWorkingConnection();
      AbstractClassServerDelegate csd = (AbstractClassServerDelegate) c.getDelegate();
      csd.getSocketInitializer().close();
      csd.getSocketClient().close();
      awaitConnections(client, Operator.AT_MOST, 1);
      testJobsInPool(client, "pool2", methodName);
      discovery.emitPool("pool1", 10);
      awaitConnections(client, Operator.AT_LEAST, 2);
      testJobsInPool(client, "pool1", methodName);
    }
  }

  /**
   *
   * @param client .
   * @param poolName .
   * @param prefix .
   * @throws Exception if any error occurs
   */
  private void testJobsInPool(final JPPFClient client, final String poolName, final String prefix) throws Exception {
    int nbJobs = 5;
    List<JPPFJob> jobs = new ArrayList<>(nbJobs);
    MyJobListener listener = new MyJobListener();
    for (int i=1; i<=nbJobs; i++) {
      JPPFJob job = BaseTestHelper.createJob(prefix + i, false, false, 1, LifeCycleTask.class, 0L);
      job.addJobListener(listener);
      jobs.add(job);
    }
    for (JPPFJob job: jobs) client.submitJob(job);
    for (JPPFJob job: jobs) {
      List<Task<?>> result = job.awaitResults();
      testJobResults(1, result);
      String name = listener.jobToPool.get(job.getUuid());
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
  private void testJobResults(final int nbTasks, final List<Task<?>> results) throws Exception {
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    int count = 0;
    for (Task<?> task : results) {
      String prefix = "task " + count + " ";
      Throwable t = task.getThrowable();
      assertNull(prefix + "has an exception", t);
      assertNotNull(prefix + "result is null", task.getResult());
    }
  }

  /**
   * Await for the specified number of pools to be working and avaialble.
   * @param client the client used to lookup the pools.
   * @param operator a condition on the number of pools to wait for.
   * @param nbPools the number of pools on which to apply the condition.
   * @throws Exception if any error occurs.
   */
  private void awaitConnections(final JPPFClient client, final Operator operator, final int nbPools) throws Exception {
    while (!operator.evaluate(client.awaitWorkingConnectionPools().size(), nbPools)) Thread.sleep(10L);
    JobManagerClient mgr = (JobManagerClient) client.getJobManager();
    while (!operator.evaluate(mgr.nbAvailableConnections(), nbPools)) Thread.sleep(10L);
  }

  /** */
  static class MyJobListener extends JobListenerAdapter {
    /**
     * Mapping of job uuid to the name of the pool to which it is dispatched.
     */
    public Map<String, String> jobToPool = new ConcurrentHashMap<>();

    @Override
    public void jobDispatched(final JobEvent event) {
      jobToPool.put(event.getJob().getUuid(), event.getConnection().getConnectionPool().getName());
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
          BaseTest.print(true, false, "found new connection pool %s", info);
          newConnection(info);
        }
        BaseTest.print(true, false, "SimpleDiscovery  about to wait in discover()");
        wait();
      }
    }

    /**
     * "Discover" the pool with the specified name and priority.
     * @param name the connection pool name.
     * @param priority the connection pool priority.
     */
    public synchronized void emitPool(final String name, final int priority) {
      BaseTest.print(true, false, "emitting %s with priority %d", name, priority);
      queue.offer(new ClientConnectionPoolInfo(name, false, "localhost", 11101, priority, 1, 1));
      notifyAll();
    }

    @Override
    public synchronized void shutdown() {
      stopped = true;
      notifyAll();
    }
  }
}
