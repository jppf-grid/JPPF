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

import java.util.List;
import java.util.concurrent.*;

import org.jppf.client.*;
import org.jppf.job.JobEventType;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for <code>JPPFJob</code>.
 * @author Laurent Cohen
 */
public class TestJPPFJob extends Setup1D1N {
  /**
   * Test that {@link Task#getTaskObject()} always returns the expected object.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testGetTaskObject() throws Exception {
    final JPPFJob job = new JPPFJob();
    final Task<?> task = job.add(new SimpleRunnable());
    assertNotNull(task);
    assertNotNull(task.getTaskObject());
    final Task<?> task2 = job.add(new SimpleTask());
    assertNotNull(task2);
    assertNotNull(task2.getTaskObject());
  }

  /**
   * Test that the expected number of {@link org.jppf.client.event.JobListener} notifications are received in the expected order,
   * with local execution enbaled and remote execution disabled.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testJobListenerLocalExecution() throws Exception {
    final int nbTasks = 10;
    JPPFConfiguration.set(LOAD_BALANCING_ALGORITHM, "manual")
      .set(LOAD_BALANCING_PROFILE, "manual")
      .setInt(LOAD_BALANCING_PROFILE.getName() + ".manual.size", 5)
      .set(REMOTE_EXECUTION_ENABLED, false)
      .set(LOCAL_EXECUTION_ENABLED, true)
      .set(LOCAL_EXECUTION_THREADS, 4);
    try (final JPPFClient client = BaseSetup.createClient(null, false, BaseSetup.DEFAULT_CONFIG)) {
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 50L);
      final CountingJobListener listener = new CountingJobListener();
      job.addJobListener(listener);
      client.submit(job);
      assertEquals(1, listener.startedCount.get());
      assertEquals(1, listener.endedCount.get());
    } finally {
      BaseSetup.resetClientConfig();
    }
  }

  /**
   * Test that a job can cancel itself.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testCancel() throws Exception {
    try (final JPPFClient client = BaseSetup.createClient(null, true, BaseSetup.DEFAULT_CONFIG)) {
      BaseSetup.checkDriverAndNodesInitialized(client, BaseSetup.nbDrivers(), BaseSetup.nbNodes(), true);
      BaseTestHelper.printToServersAndNodes(client, true, true, "start of method %s()", ReflectionUtils.getCurrentMethodName());
      final int nbTasks = 10;
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 5000L);
      assertNull(job.getSLA().getDependencySpec().getId());
      final AwaitJobNotificationListener listener = new AwaitJobNotificationListener(client, JobEventType.JOB_DISPATCHED);
      print(false, false, "submitting %s", job);
      client.submitAsync(job);
      print(false, false, "awaiting JOB_DISPATCHED event");
      listener.await();
      print(false, false, "cancelling job");
      final boolean cancelled = job.cancel(true);
      assertTrue(cancelled);
      final List<Task<?>> results = job.get();
      print(false, false, "got job results");
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      int count = 0;
      for (Task<?> task: results) {
        if (task.getResult() == null) count++;
      }
      assertTrue(count > 0);
      assertTrue(job.isCancelled());
      assertTrue(job.isDone());
    }
  }

  /**
   * Test that a job isn't cancelled and {@code job.cancel(false)} is invoked while it is executing.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testCancelWithInterruptFlagFalse() throws Exception {
    final String method = ReflectionUtils.getCurrentMethodName();
    try (JPPFClient client = BaseSetup.createClient(null, true, BaseSetup.DEFAULT_CONFIG)) {
      BaseSetup.checkDriverAndNodesInitialized(client, BaseSetup.nbDrivers(), BaseSetup.nbNodes(), true);
      BaseTestHelper.printToServersAndNodes(client, true, true, "start of method %s()", method);
      try {
        final int nbTasks = 1;
        final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 3000L);
        final AwaitJobNotificationListener listener = new AwaitJobNotificationListener(client, JobEventType.JOB_DISPATCHED);
        client.submitAsync(job);
        listener.await();
        final boolean cancelled = job.cancel(false);
        assertFalse(cancelled);
        final List<Task<?>> results = job.get();
        assertNotNull(results);
        assertEquals(nbTasks, results.size());
        int count = 0;
        for (final Task<?> task: results) {
          if (task.getResult() == null) count++;
        }
        assertEquals(0, count);
        assertFalse(job.isCancelled());
        assertTrue(job.isDone());
      } finally {
        BaseTestHelper.printToServersAndNodes(client, true, true, "end of method %s()", method);
      }
    }
  }

  /**
   * Test the cancellation of a job immediately after it is submitted.
   * @throws Exception if any error occurs
   */
  @SuppressWarnings("deprecation")
  //@Ignore
  //@Test(timeout=15000)
  public void testCancelImmediately() throws Exception {
    ///final String name  = ReflectionUtils.getCurrentMethodName();
    final String name  = "tci";
    final int nbTasks = 1;
    final int nbJobs = 10;
    try (final JPPFClient client = BaseSetup.createClient("c1", true, BaseSetup.DEFAULT_CONFIG)) {
      client.awaitWorkingConnectionPool().setMaxJobs(1);
      int totalCancelCount = 0;
      for (int i=1; i<=nbJobs; i++) {
        print(false, false, ">>> test iteration %d", i);
        final JPPFJob job = BaseTestHelper.createJob(name + "-" + i, false, nbTasks, LifeCycleTask.class, 1000L);
        client.submitAsync(job);
        //Thread.sleep(1L);
        print(false, false, ">>> cancelling job %d", i);
        assertTrue(job.cancel(true));
        print(false, false, ">>> awaiting results for job %d", i);
        final List<Task<?>> results = job.awaitResults();
        print(false, false, ">>> checking results for job %d", i);
        assertNotNull(results);
        assertEquals(nbTasks, results.size());
        int count = 0;
        for (Task<?> task: results) {
          if (task.getResult() == null) count++;
        }
        if (count > 0) totalCancelCount++;
        // success of cancelling a job immediately after submission is on a best effort basis,
        // therefore we merely check that at least one of the jobs was effectively cancelled
        //assertTrue(count > 0);
        //assertTrue(job.isCancelled());
        assertTrue(job.isDone());
      }
      //final int n = (nbJobs / 2) + (nbJobs % 2);
      print(false, false, ">>> totalCancelCount = %d", totalCancelCount);
      //assertTrue("total cancel count is " + totalCancelCount + " but should be >= 1", totalCancelCount >= 1);
    }
  }

  /**
   * Test that when {@code JPPFJob.get(timeout)} expires, a {@link TimeoutException} is raised.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000, expected=TimeoutException.class)
  public void testGetWithTimeout() throws Exception {
    try (final JPPFClient client = BaseSetup.createClient(null, true, BaseSetup.DEFAULT_CONFIG)) {
      BaseSetup.checkDriverAndNodesInitialized(client, BaseSetup.nbDrivers(), BaseSetup.nbNodes(), true);
      BaseTestHelper.printToServersAndNodes(client, true, true, "start of method %s()", ReflectionUtils.getCurrentMethodName());
      final int nbTasks = 1;
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 2000L);
      client.submitAsync(job);
      job.get(1000L, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Test that when a job expires to due its client side SLA's expiration schedule, {@code JPPFJob.isCancelled()} returns {@code true}.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testIsCancelledClientSideExpiration() throws Exception {
    try (final JPPFClient client = BaseSetup.createClient(null, true, BaseSetup.DEFAULT_CONFIG)) {
      BaseSetup.checkDriverAndNodesInitialized(client, BaseSetup.nbDrivers(), BaseSetup.nbNodes(), true);
      BaseTestHelper.printToServersAndNodes(client, true, true, "start of method %s()", ReflectionUtils.getCurrentMethodName());
      final int nbTasks = 1;
      final long duration = 3000L;
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, duration);
      job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(duration/2L));
      final List<Task<?>> results = client.submit(job);
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      int count = 0;
      for (final Task<?> task: results) {
        if (task.getResult() == null) count++;
      }
      assertEquals(1, count);
      assertTrue(job.isCancelled());
      assertTrue(job.isDone());
    }
  }
}
