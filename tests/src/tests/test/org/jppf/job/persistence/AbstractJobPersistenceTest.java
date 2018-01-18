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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.h2.tools.Script;
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.job.*;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.diagnostics.*;
import org.jppf.node.protocol.Task;
import org.jppf.test.addons.common.AddonSimpleTask;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import test.org.jppf.persistence.AbstractDatabaseSetup;
import test.org.jppf.test.setup.BaseSetup;
import test.org.jppf.test.setup.common.*;

/**
 *
 * @author Laurent Cohen
 */
public abstract class AbstractJobPersistenceTest extends AbstractDatabaseSetup {
  /**
   * 
   */
  private static final long WAIT_TIME_EMPTY_UUIDS = 5000L;
  /**
   * 
   */
  private int dumpSequence;

  /** */
  @Rule
  public TestWatcher setup1D1N1CWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      BaseTestHelper.printToAll(client, false, false, true, true, true, "start of method %s()", description.getMethodName());
    }
  };

  /**
   * @throws Exception if any error occurs.
   */
  @After
  public void tearDownInstance() throws Exception {
    try (final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", 11201, false)) {
      jmx.connectAndWait(5_000L);
      final boolean b = jmx.isConnected();
      print(false, false, "tearDownInstance() : jmx connected = %b", b);
      if (b) {
        final JPPFDriverJobPersistence mgr = new JPPFDriverJobPersistence(jmx);
        mgr.deleteJobs(JobSelector.ALL_JOBS);
        final DiagnosticsMBean proxy = jmx.getDiagnosticsProxy();
        if (proxy.hasDeadlock()) {
          final String text = TextThreadDumpWriter.printToString(proxy.threadDump(), "driver thread dump for " + jmx);
          FileUtils.writeTextFile("driver_thread_dump_" + ++dumpSequence  + "_" + jmx.getPort() + ".log", text);
          assertTrue("driver deadlock detected", false);
        }
      }
    }
  }

  /**
   * @throws Exception if any error occurs.
   */
  @AfterClass
  public static void tearAbstractJobPersistenceTest() throws Exception {
    try (final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", 11201, false)) {
      jmx.connectAndWait(5_000L);
      if (jmx.isConnected()) BaseSetup.generateDriverThreadDump(jmx);
    }
  }

  /**
   * Test that a persisted job executes normally and is deleted from persistence after completion as configured.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testSimplePersistedJob() throws Exception {
    final int nbTasks = 10;
    final String method = ReflectionUtils.getCurrentMethodName();
    final JPPFJob job = BaseTestHelper.createJob(method, false, false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().setCancelUponClientDisconnect(false);
    job.getSLA().getPersistenceSpec().setPersistent(true).setAutoExecuteOnRestart(false).setDeleteOnCompletion(true);
    client.submitJob(job);
    final List<Task<?>> results = job.awaitResults();
    checkJobResults(nbTasks, results, false);
    final JMXDriverConnectionWrapper jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    final JPPFDriverJobPersistence mgr = new JPPFDriverJobPersistence(jmx);
    assertTrue(ConcurrentUtils.awaitCondition(new EmptyPersistedUuids(mgr), WAIT_TIME_EMPTY_UUIDS));
    assertFalse(mgr.deleteJob(job.getUuid()));
  }

  /**
   * Test that a persisted job executes normally and can be retrieved from the perisstence store.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testSimplePersistedJobRetrieval() throws Exception {
    final int nbTasks = 10;
    final String method = ReflectionUtils.getCurrentMethodName();
    final JPPFJob job = BaseTestHelper.createJob(method, false, false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().setCancelUponClientDisconnect(false);
    job.getSLA().getPersistenceSpec().setPersistent(true).setAutoExecuteOnRestart(false).setDeleteOnCompletion(false);
    client.submitJob(job);
    final List<Task<?>> results = job.awaitResults();
    if (h2Server != null) Script.main("-url", DB_URL, "-user", DB_USER, "-password", DB_PWD, "-script", "test1h2dump.log");
    checkJobResults(nbTasks, results, false);
    final JMXDriverConnectionWrapper jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    final JPPFDriverJobPersistence mgr = new JPPFDriverJobPersistence(jmx);
    assertTrue(ConcurrentUtils.awaitCondition(new PersistedJobCompletion(mgr, job.getUuid()), 6000L));
    final List<String> persistedUuids = mgr.listJobs(JobSelector.ALL_JOBS);
    assertNotNull(persistedUuids);
    assertEquals(1, persistedUuids.size());
    final JPPFJob job2 = mgr.retrieveJob(job.getUuid());
    compareJobs(job, job2, true);
    checkJobResults(nbTasks, job2.getResults().getAllResults(), false);
    assertEquals(JobStatus.COMPLETE, job2.getStatus());
    assertTrue(mgr.deleteJob(job.getUuid()));
    assertTrue(ConcurrentUtils.awaitCondition(new EmptyPersistedUuids(mgr), WAIT_TIME_EMPTY_UUIDS));
  }

  /**
   * Test that a persisted job executes normally, can be cancelled and is deleted from persistence after completion as configured.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testSimplePersistedJobCancellation() throws Exception {
    final int nbTasks = 40;
    final String method = ReflectionUtils.getCurrentMethodName();
    final JPPFJob job = BaseTestHelper.createJob(method, false, false, nbTasks, LifeCycleTask.class, 100L);
    job.getSLA().setCancelUponClientDisconnect(false);
    job.getSLA().getPersistenceSpec().setPersistent(true).setAutoExecuteOnRestart(false).setDeleteOnCompletion(true);
    client.submitJob(job);
    Thread.sleep(1000L);
    assertTrue(job.cancel());
    final List<Task<?>> results = job.awaitResults();
    checkJobResults(nbTasks, results, true);
    final JMXDriverConnectionWrapper jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    final JPPFDriverJobPersistence mgr = new JPPFDriverJobPersistence(jmx);
    assertTrue(ConcurrentUtils.awaitCondition(new EmptyPersistedUuids(mgr), WAIT_TIME_EMPTY_UUIDS));
    assertFalse(mgr.deleteJob(job.getUuid()));
  }

  /**
   * Test restarting the driver while executing a job.
   * The client should recover gracefully and provide the job results without intervention.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000)
  public void testJobAutoRecoveryOnDriverRestart() throws Exception {
    final int nbTasks = 20;
    final String method = ReflectionUtils.getCurrentMethodName();
    final JPPFJob job = BaseTestHelper.createJob(method, false, false, nbTasks, LifeCycleTask.class, 100L);
    job.getSLA().setCancelUponClientDisconnect(false);
    job.getSLA().getPersistenceSpec().setPersistent(true).setAutoExecuteOnRestart(false).setDeleteOnCompletion(false);
    try (final JMXDriverConnectionWrapper jmx = newJmx(client)) {
      jmx.setReconnectOnError(false);
      final AwaitJobNotificationListener listener = new AwaitJobNotificationListener(jmx, JobEventType.JOB_DISPATCHED);
      client.submitJob(job);
      listener.await();
      Thread.sleep(500L);
      print(false, false, "about to request driver restart");
      jmx.restartShutdown(1L, 1L);
    }
    Thread.sleep(500L);
    print(false, false, "waiting for job results");
    final List<Task<?>> results = job.awaitResults();
    checkJobResults(nbTasks, results, false);
    try (final JMXDriverConnectionWrapper jmx = newJmx(client)) {
      final JPPFDriverJobPersistence mgr = new JPPFDriverJobPersistence(jmx);
      final List<String> persistedUuids = mgr.listJobs(JobSelector.ALL_JOBS);
      assertNotNull(persistedUuids);
      assertEquals(1, persistedUuids.size());
      assertNotNull(persistedUuids.get(0));
      assertEquals(job.getUuid(), persistedUuids.get(0));
      assertTrue(mgr.deleteJob(job.getUuid()));
    }
  }

  /**
   * Test restarting the driver while executing a job.
   * The driver should resubmit the job for execution upon restart, and the client must be able to retrieve the job upon completion.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000)
  public void testJobAutoExecuteOnDriverRestart() throws Exception {
    final int nbTasks = 20;
    final String method = ReflectionUtils.getCurrentMethodName();
    final JPPFJob job = BaseTestHelper.createJob(method, false, false, nbTasks, AddonSimpleTask.class, 100L);
    job.getSLA().setCancelUponClientDisconnect(false);
    job.getSLA().getPersistenceSpec().setPersistent(true).setAutoExecuteOnRestart(true).setDeleteOnCompletion(false);
    try (JMXDriverConnectionWrapper jmx = newJmx(client)) {
      jmx.setReconnectOnError(false);
      final AwaitJobNotificationListener listener = new AwaitJobNotificationListener(jmx, JobEventType.JOB_DISPATCHED);
      client.submitJob(job);
      listener.await();
      client.close();
      jmx.restartShutdown(500L, 1L);
    }
    Thread.sleep(500L);
    client = BaseSetup.createClient(null);
    try (final JMXDriverConnectionWrapper jmx = newJmx(client)) {
      final JPPFDriverJobPersistence mgr = new JPPFDriverJobPersistence(jmx);
      assertTrue(ConcurrentUtils.awaitCondition(new PersistedJobCompletion(mgr, job.getUuid()), 6000L));
      final List<String> persistedUuids = mgr.listJobs(JobSelector.ALL_JOBS);
      assertNotNull(persistedUuids);
      assertEquals(1, persistedUuids.size());
      assertNotNull(persistedUuids.get(0));
      assertEquals(job.getUuid(), persistedUuids.get(0));
      final JPPFJob job2 = mgr.retrieveJob(job.getUuid());
      compareJobs(job, job2, false);
      print(true, false, "job2 results: " + job2.getResults());
      checkJobResults(nbTasks, job2.getResults().getAllResults(), false);
      assertTrue(mgr.deleteJob(job.getUuid()));
    }
  }

  /**
   * Test that a persisted job executes and is persisted normally when submittd over two connections to the same driver.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testJobSubmittedOnTwoChannels() throws Exception {
    final int nbTasks = 2 * 10;
    final String method = ReflectionUtils.getCurrentMethodName();
    final JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
    final LoadBalancingInformation lbi = client.getLoadBalancerSettings();
    try {
      client.setLoadBalancerSettings("manual", new TypedProperties().setInt("size", nbTasks / 2));
      pool.setSize(2);
      pool.awaitActiveConnections(Operator.EQUAL, 2);
      final JPPFJob job = BaseTestHelper.createJob(method, true, false, nbTasks, LifeCycleTask.class, 100L);
      job.getSLA().setCancelUponClientDisconnect(false);
      job.getSLA().getPersistenceSpec().setPersistent(true).setAutoExecuteOnRestart(false).setDeleteOnCompletion(false);
      job.getClientSLA().setMaxChannels(2);
      final List<String> dispatchList = new CopyOnWriteArrayList<>();
      final JobListener listener = new JobListenerAdapter() {
        @Override
        public void jobDispatched(final JobEvent event) {
          dispatchList.add(event.getConnection().getConnectionUuid());
        }
      };
      job.addJobListener(listener);
      final List<Task<?>> results = client.submitJob(job);
      assertEquals(2, dispatchList.size());
      print(false, false, "dispatch list: %s", dispatchList);
      assertNotSame(dispatchList.get(0), dispatchList.get(1));
      checkJobResults(nbTasks, results, false);
      final JMXDriverConnectionWrapper jmx = pool.awaitWorkingJMXConnection();
      final JPPFDriverJobPersistence mgr = new JPPFDriverJobPersistence(jmx);
      assertTrue(ConcurrentUtils.awaitCondition(new PersistedJobCompletion(mgr, job.getUuid()), 6000L));
      final List<String> persistedUuids = mgr.listJobs(JobSelector.ALL_JOBS);
      assertNotNull(persistedUuids);
      assertEquals(1, persistedUuids.size());
      final JPPFJob job2 = mgr.retrieveJob(job.getUuid());
      compareJobs(job, job2, true);
      checkJobResults(nbTasks, job2.getResults().getAllResults(), false);
      assertEquals(JobStatus.COMPLETE, job2.getStatus());
      assertTrue(mgr.deleteJob(job.getUuid()));
      assertTrue(ConcurrentUtils.awaitCondition(new EmptyPersistedUuids(mgr), WAIT_TIME_EMPTY_UUIDS));
    } finally {
      if (lbi != null) client.setLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
      if (pool != null) {
        pool.setSize(1);
        pool.awaitActiveConnections(Operator.EQUAL, 1);
      }
    }
  }

  /** */
  static class EmptyPersistedUuids implements ConcurrentUtils.Condition {
    /** */
    final JPPFDriverJobPersistence mgr;

    /**
     * @param mgr .
     */
    EmptyPersistedUuids(final JPPFDriverJobPersistence mgr) {
      this.mgr = mgr;
    }

    @Override
    public boolean evaluate() {
      try {
        final List<String> persistedUuids = mgr.listJobs(JobSelector.ALL_JOBS);
        return (persistedUuids != null) && persistedUuids.isEmpty();
      } catch (@SuppressWarnings("unused") final Exception e) {
      }
      return false;
    }
  };

  /** */
  static class PersistedJobCompletion implements ConcurrentUtils.Condition {
    /** */
    final JPPFDriverJobPersistence mgr;
    /** */
    final String uuid;

    /**
     * @param mgr .
     * @param uuid .
     */
    PersistedJobCompletion(final JPPFDriverJobPersistence mgr, final String uuid) {
      this.mgr = mgr;
      this.uuid = uuid;
    }

    @Override
    public boolean evaluate() {
      try {
        return mgr.isJobComplete(uuid);
      } catch (@SuppressWarnings("unused") final Exception e) {
      }
      return false;
    }
  };
}
