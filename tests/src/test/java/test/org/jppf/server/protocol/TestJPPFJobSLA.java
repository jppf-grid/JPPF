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

package test.org.jppf.server.protocol;

import static org.junit.Assert.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link org.jppf.node.protocol.JobSLA JobSLA}.
 * In this class, we test that the behavior is the expected one, from the client point of view,
 * as specified in the job SLA.
 * @author Laurent Cohen
 */
public class TestJPPFJobSLA extends Setup1D2N1C {
  /**
   * A "short" duration for this test.
   */
  private static final long TIME_SHORT = 750L;
  /**
   * A "long" duration for this test.
   */
  private static final long TIME_LONG = 3000L;
  /**
   * A the date format used in the tests.
   */
  private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

  /**
   * @throws Exception if any error occurs.
   */
  @After
  public void instanceCleanup() throws Exception {
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection();
    final String driverState = (String) jmx.invoke("org.jppf:name=debug,type=driver", "dumpQueueDetails");
    if ((driverState !=  null) && !driverState.trim().isEmpty()) {
      print(false, false, "-------------------- driver state --------------------");
      print(false, false, driverState);
    }
  }

  /**
   * Simply test that a job does expires at a specified date.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAtDate() throws Exception {
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, SimpleTask.class, TIME_LONG);
    final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    final Date date = new Date(System.currentTimeMillis() + TIME_SHORT);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(sdf.format(date), DATE_FORMAT));
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), 1);
    final Task<?> task = results.get(0);
    assertNull(task.getResult());
  }

  /**
   * Test that a job does not expires at a specified date, because it completes before that date.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAtDateTooLate() throws Exception {
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, SimpleTask.class, TIME_SHORT);
    final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    final Date date = new Date(System.currentTimeMillis() + TIME_LONG);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(sdf.format(date), DATE_FORMAT));
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), 1);
    final Task<?> task = results.get(0);
    assertNotNull(task.getResult());
    assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
  }

  /**
   * Simply test that a job does expires after a specified delay.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAfterDelay() throws Exception {
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, SimpleTask.class, TIME_LONG);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_SHORT));
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), 1);
    final Task<?> task = results.get(0);
    assertNull(task.getResult());
  }

  /**
   * Test that a job does not expire after a specified delay, because it completes before that.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAfterDelayTooLate() throws Exception {
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, SimpleTask.class, TIME_SHORT);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_LONG));
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), 1);
    final Task<?> task = results.get(0);
    assertNotNull(task.getResult());
    assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
  }

  /**
   * Simply test that a suspended job does expires after a specified delay.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testSuspendedJobExpiration() throws Exception {
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, SimpleTask.class, TIME_LONG);
    job.getSLA().setSuspended(true);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_SHORT));
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), 1);
    final Task<?> task = results.get(0);
    assertNull(task.getResult());
  }

  /**
   * Test that a job queued in the client does not expire there.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testMultipleJobsExpiration() throws Exception {
    final JPPFJob job1 = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + "-1", false, 1, SimpleTask.class, TIME_LONG);
    job1.getSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_SHORT));
    final JPPFJob job2 = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + "-2", false, 1, SimpleTask.class, TIME_SHORT);
    job2.getSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_LONG));
    client.submitAsync(job1);
    client.submitAsync(job2);
    List<Task<?>> results = job1.awaitResults();
    assertNotNull(results);
    assertEquals(results.size(), 1);
    Task<?> task = results.get(0);
    assertNull(task.getResult());
    results = job2.awaitResults();
    assertNotNull(results);
    assertEquals(results.size(), 1);
    task = results.get(0);
    assertNotNull(task.getResult());
    assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
  }

  /**
   * Test that a job is not cancelled when the client connection is closed
   * and <code>JPPFJob.getSLA().setCancelUponClientDisconnect(false)</code> has been set.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testCancelJobUponClientDisconnect() throws Exception {
    print(false, false, "%s() configuration: %s", ReflectionUtils.getCurrentMethodName(), JPPFConfiguration.getProperties());
    final String fileName = "testCancelJobUponClientDisconnect";
    final File f = new File(fileName + ".tmp");
    f.deleteOnExit();
    try {
      assertFalse(f.exists());
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, FileTask.class, fileName, false);
      job.getSLA().setCancelUponClientDisconnect(false);
      client.submitAsync(job);
      Thread.sleep(1000L);
      client.close();
      Thread.sleep(2000L);
      assertTrue(f.exists());
    } catch(final Exception e) {
      e.printStackTrace();
      throw e;
    } finally {
      f.delete();
      client = BaseSetup.createClient(null, true, BaseSetup.DEFAULT_CONFIG);
    }
  }

  /**
   * Test that a job with a higher priority is executed before a job with a smaller priority.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testJobPriority() throws Exception {
    final int nbJobs = 3;
    JPPFConnectionPool pool = null;
    try {
      while ((pool = client.getConnectionPool()) == null) Thread.sleep(10L);
      pool.setJMXPoolSize(nbJobs);
      final JPPFJob[] jobs = new JPPFJob[nbJobs];
      final ExecutionPolicy policy = new Equal("jppf.node.uuid", false, "n1");
      for (int i=0; i<nbJobs; i++) {
        jobs[i] = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + i, false, 1, LifeCycleTask.class, 750L);
        jobs[i].getSLA().setPriority(i);
        jobs[i].getSLA().setExecutionPolicy(policy);
      }
      for (int i=0; i<nbJobs; i++) {
        client.submitAsync(jobs[i]);
        if (i == 0) Thread.sleep(500L);
      }
      final List<List<Task<?>>> results = new ArrayList<>(nbJobs);
      for (int i=0; i<nbJobs; i++) results.add(jobs[i].awaitResults());
      final LifeCycleTask t1 = (LifeCycleTask) results.get(1).get(0);
      assertNotNull(t1);
      final LifeCycleTask t2 = (LifeCycleTask) results.get(2).get(0);
      assertNotNull(t2);
      assertTrue("3rd job (start=" + t2.getStart() + ") should have started before the 2nd (start=" + t1.getStart() + ")", t2.getStart() < t1.getStart());
    } finally {
      if (pool != null) pool.setJMXPoolSize(1);
    }
  }

  /**
   * Test that a job is only sent to the server according to its execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExecutionPolicy() throws Exception {
    final int nbTasks = 10;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class);
    job.getSLA().setExecutionPolicy(new Equal("jppf.node.uuid", false, "n2"));
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (final Task<?> t: results) {
      final LifeCycleTask task = (LifeCycleTask) t;
      assertTrue("n2".equals(task.getNodeUuid()));
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
  }

  /**
   * Test that a broadcast job is executed on all nodes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testBroadcastJob() throws Exception {
    final String suffix = "node-";
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, 1, FileTask.class, suffix, true);
    job.getSLA().setMaxNodes(2);
    final List<Task<?>> result = client.submit(job);
    print(false, false, "initial tasks:        %s", job.getJobTasks());
    print(false, false, "broacast job results: %s", result);
    for (int i=1; i<=2; i++) {
      final File file = new File("node-n" + i + ".tmp");
      try {
        assertTrue("file '" + file + "' does not exist", file.exists());
      } finally {
        if (file.exists()) file.delete();
      }
    }
  }

  /**
   * Test that a broadcast job is executed on all nodes,
   * even though another job is already executing at the time it is submitted.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testBroadcastJob2() throws Exception {
    final JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
    try {
      pool.setSize(2);
      pool.awaitConnections(Operator.EQUAL, 2, 5000L, JPPFClientConnectionStatus.workingStatuses());
      final String methodName = ReflectionUtils.getCurrentMethodName();
      final JPPFJob job1 = BaseTestHelper.createJob(methodName + "-normal", false, 10, LifeCycleTask.class, 500L);
      job1.getSLA().setPriority(1000);
      final String suffix = "broadcast-node-";
      final JPPFJob job2 = BaseTestHelper.createJob(methodName + "-broadcast", true, 1, FileTask.class, suffix, true);
      job2.getSLA().setPriority(-1000);
      BaseTestHelper.printToAll(client, true, true, true, false, ">>> submitting job1");
      client.submitAsync(job1);
      Thread.sleep(500L);
      BaseTestHelper.printToAll(client, true, true, true, false, ">>> submitting job2");
      client.submitAsync(job2);
      job1.awaitResults();
      BaseTestHelper.printToAll(client, true, true, true, false, ">>> got job1 results");
      job2.awaitResults();
      BaseTestHelper.printToAll(client, true, true, true, false, ">>> got job2 results");
      for (int i=1; i<=2; i++) {
        final File file = new File(suffix + "n" + i + ".tmp");
        final boolean exists = file.exists();
        try {
          assertTrue("file '" + file + "' does not exist", exists);
        } finally {
          if (exists) file.delete();
        }
      }
    } finally {
      pool.setSize(1);
      pool.awaitConnections(Operator.EQUAL, 1, 5000L, JPPFClientConnectionStatus.workingStatuses());
    }
  }

  /**
   * Test that a broadcast job is not executed when no node is available.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testBroadcastJobNoNodeAvailable() throws Exception {
    final String suffix = "node-";
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, 1, FileTask.class, suffix, true);
    job.getSLA().setMaxNodes(2);
    job.getSLA().setExecutionPolicy(new Equal("jppf.uuid", false, "no node has this as uuid!"));
    client.submit(job);
    for (int i=1; i<=2; i++) {
      final File file = new File("node-n" + i + ".tmp");
      final boolean exists = file.exists();
      try {
        assertFalse("file '" + file + "' exists but shouldn't", exists);
      } finally {
        if (exists) file.delete();
      }
    }
  }

  /**
   * Test that results are returned according to the SendNodeResultsStrategy specified in the SLA.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testSendNodeResultsStrategy() throws Exception {
    checkSendResultsStrategy(ReflectionUtils.getCurrentMethodName(), SendResultsStrategyConstants.NODE_RESULTS, 4);
  }

  /**
   * Test that results are returned according to the SendAllResultsStrategy specified in the SLA.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testSendAllResultsStrategy() throws Exception {
    checkSendResultsStrategy(ReflectionUtils.getCurrentMethodName(), SendResultsStrategyConstants.ALL_RESULTS, 1);
  }

  /**
   * Test that results are returned according to the default strategy (SendNodeResultsStrategy).
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testDefaultSendResultsStrategy() throws Exception {
    checkSendResultsStrategy(ReflectionUtils.getCurrentMethodName(), null, 4);
  }

  /**
   * Test that results are returned according to the specified strategy.
   * @param jobName the name the job to execute.
   * @param strategyName the name of the strategy to test.
   * @param expectedReturnedCount the expected number of 'job returned' notifications.
   * @throws Exception if any error occurs.
   */
  private static void checkSendResultsStrategy(final String jobName, final String strategyName, final int expectedReturnedCount) throws Exception {
    final int nbTasks = 20;
    final JPPFJob job = BaseTestHelper.createJob(jobName, false, nbTasks, LifeCycleTask.class, 1L);
    final AtomicInteger returnedCount = new AtomicInteger(0);
    final JobListener listener = new JobListenerAdapter() {
      @Override
      public synchronized void jobReturned(final JobEvent event) {
        returnedCount.incrementAndGet();
      }
    };
    job.addJobListener(listener);
    job.getSLA().setResultsStrategy(strategyName);
    client.submit(job);
    assertEquals(expectedReturnedCount, returnedCount.get());
  }
}
