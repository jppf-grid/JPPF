/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

import static org.jppf.utils.configuration.JPPFProperties.*;
import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link org.jppf.node.protocol.JobClientSLA JobClientSLA}.
 * In this class, we test that the behavior is the expected one, from the client point of view,
 * as specified in the job SLA.
 * @author Laurent Cohen
 */
public class TestJPPFJobClientSLA extends Setup1D1N {
  /**
   * A "short" duration for this test.
   */
  private static final long TIME_SHORT = 1000L;
  /**
   * A "long" duration for this test.
   */
  private static final long TIME_LONG = 5000L;
  /**
   * A the date format used in the tests.
   */
  private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
  /**
   * The JPPF client to use.
   */
  private JPPFClient jppfClient;

  /**
   * Simply test that a job does expires at a specified date.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAtDateClient() throws Exception {
    try {
      configure(false, true, 1);
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, SimpleTask.class, TIME_LONG);
      final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
      final Date date = new Date(System.currentTimeMillis() + TIME_SHORT);
      job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(sdf.format(date), DATE_FORMAT));
      final List<Task<?>> results = jppfClient.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), 1);
      final Task<?> task = results.get(0);
      assertNull(task.getResult());
    } finally {
      reset();
    }
  }

  /**
   * Test that a job does not expire at a specified date, because it completes before that date.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAtDateTooLateClient() throws Exception {
    try {
      configure(false, true, 1);
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, SimpleTask.class, TIME_SHORT);
      final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
      final Date date = new Date(System.currentTimeMillis() + TIME_LONG);
      job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(sdf.format(date), DATE_FORMAT));
      final List<Task<?>> results = jppfClient.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), 1);
      final Task<?> task = results.get(0);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    } finally {
      reset();
    }
  }

  /**
   * Simply test that a job does expires after a specified delay.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAfterDelayClient() throws Exception {
    try {
      configure(false, true, 1);
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, SimpleTask.class, TIME_LONG);
      job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_SHORT));
      final List<Task<?>> results = jppfClient.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), 1);
      final Task<?> task = results.get(0);
      assertNull(task.getResult());
    } finally {
      reset();
    }
  }

  /**
   * Test that a job does not expire after a specified delay, because it completes before that.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAfterDelayTooLateClient() throws Exception {
    try {
      configure(false, true, 1);
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, SimpleTask.class, TIME_SHORT);
      job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_LONG));
      final List<Task<?>> results = jppfClient.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), 1);
      final Task<?> task = results.get(0);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    } finally {
      reset();
    }
  }

  /**
   * Test that a job queued in the client does not expire there.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testMultipleJobsExpirationClient() throws Exception {
    try {
      configure(false, true, 1);
      final String methodName = ReflectionUtils.getCurrentMethodName();
      final JPPFJob job1 = BaseTestHelper.createJob(methodName + "-1", false, 1, SimpleTask.class, TIME_LONG);
      job1.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_SHORT));
      final JPPFJob job2 = BaseTestHelper.createJob(methodName + "-2", false, 1, SimpleTask.class, TIME_SHORT);
      job2.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_LONG));
      jppfClient.submitAsync(job1);
      jppfClient.submitAsync(job2);
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
    } finally {
      reset();
    }
  }

  /**
   * Test that a job is only sent to the server according to its execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testJobInNodeExecutionPolicyClient() throws Exception {
    try {
      configure(true, true, 1);
      BaseSetup.checkDriverAndNodesInitialized(jppfClient, 1, 1);
      final int nbTasks = 10;
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class);
      job.getClientSLA().setExecutionPolicy(new Equal("jppf.channel.local", false));
      final List<Task<?>> results = jppfClient.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      for (final Task<?> t: results) {
        final LifeCycleTask task = (LifeCycleTask) t;
        assertTrue(task.isExecutedInNode());
        assertNotNull(task.getResult());
        assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
      }
    } finally {
      reset();
    }
  }

  /**
   * Test that a job is only executed locally in the client according to its execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testJobLocalExecutionPolicyClient() throws Exception {
    try {
      configure(true, true, 1);
      BaseSetup.checkDriverAndNodesInitialized(jppfClient, 1, 1);
      final int nbTasks = 10;
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class);
      job.getClientSLA().setExecutionPolicy(new Equal("jppf.channel.local", true));
      final List<Task<?>> results = jppfClient.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      for (final Task<?> t: results) {
        final LifeCycleTask task = (LifeCycleTask) t;
        assertFalse(task.isExecutedInNode());
        assertNotNull(task.getResult());
        assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
      }
    } finally {
      reset();
    }
  }

  /**
   * Test that a job is only executed on one channel at a time, either local or remote.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobMaxChannelsClient() throws Exception {
    try {
      configure(true, true, 1);
      BaseSetup.checkDriverAndNodesInitialized(jppfClient, 1, 1);
      final int nbTasks = 10;
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class, 100L);
      job.getClientSLA().setMaxChannels(1);
      final List<Task<?>> results = jppfClient.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      // check that no 2 tasks were executing at the same time on different channels
      for (int i=0; i<results.size()-1; i++) {
        final LifeCycleTask t1 = (LifeCycleTask) results.get(i);
        final Range<Double> r1 = new Range<>(t1.getStart(), t1.getStart() + t1.getElapsed());
        for (int j=i+1; j<results.size(); j++) {
          final LifeCycleTask t2 = (LifeCycleTask) results.get(j);
          final Range<Double> r2 = new Range<>(t2.getStart(), t2.getStart() + t2.getElapsed());
          assertFalse("r1=" + r1 + ", r2=" + r2 + ", uuid1=" + t1.getNodeUuid() + ", uuid2=" + t2.getNodeUuid(), 
            r1.intersects(r2, false) && !t1.getNodeUuid().equals(t2.getNodeUuid()));
        }
      }
    } finally {
      reset();
    }
  }

  /**
   * Test that a job is executed on both local and remote channels,
   * with at least 2 tasks executing at the same time on each channel.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testJobMaxChannels2Client() throws Exception {
    try {
      configure(true, true, 1);
      BaseSetup.checkDriverAndNodesInitialized(jppfClient, 1, 1);
      jppfClient.awaitActiveConnectionPool().setMaxJobs(1);
      final int nbTasks = Math.max(2*Runtime.getRuntime().availableProcessors(), 10);
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class, 100L);
      job.getClientSLA().setMaxChannels(2);
      final List<Task<?>> results = jppfClient.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      boolean found = false;
      // check that 2 tasks were executing at the same time on different channels
      for (int i=0; i<results.size()-1; i++) {
        final LifeCycleTask t1 = (LifeCycleTask) results.get(i);
        final Range<Double> r1 = new Range<>(t1.getStart(), t1.getStart() + t1.getElapsed());
        for (int j=i+1; j<results.size(); j++) {
          final LifeCycleTask t2 = (LifeCycleTask) results.get(j);
          final Range<Double> r2 = new Range<>(t2.getStart(), t2.getStart() + t2.getElapsed());
          if ((r1.intersects(r2) || r2.intersects(r1)) && !t1.getNodeUuid().equals(t2.getNodeUuid())) {
            found = true;
            break;
          }
        }
        if (found) break;
      }
      assertTrue(found);
    } catch(final Exception e) {
      e.printStackTrace();
      throw e;
    } finally {
      reset();
    }
  }

  /**
   * Test that the size of a client-side dispatch does not exceed the maximum specified in the SLA.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testMaxDispatchSizeClient() throws Exception {
    try {
      configure(true, true, 1);
      jppfClient.setLoadBalancerSettings("manual", new TypedProperties().setInt("size", 15));
      BaseSetup.checkDriverAndNodesInitialized(jppfClient, 1, 1);
      final int nbTasks = 100, maxDispatchSize = 6;
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class);
      job.getClientSLA().setMaxDispatchSize(maxDispatchSize);
      final DispatchCounterJobListener listener = new DispatchCounterJobListener();
      job.addJobListener(listener);
      final List<Task<?>> results = jppfClient.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      assertCompare(Operator.AT_MOST, maxDispatchSize, listener.maxDispatchSize);
    } finally {
      reset();
    }
  }

  /**
   * Test that a job with {@code allowMultipleDispatchesToSameChannel = true} is dispatched concurrently to the same channel.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testAllowMultipleDispatchesToSameChannelClient() throws Exception {
    try {
      configure(true, false, 1);
      jppfClient.setLoadBalancerSettings("manual", new TypedProperties().setInt("size", 15));
      BaseSetup.checkDriverAndNodesInitialized(jppfClient, 1, 1);
      final JPPFConnectionPool pool = jppfClient.awaitWorkingConnectionPool();
      pool.setMaxJobs(100);
      print(false, false, "got pool %s", pool);
      final int nbTasks = 50;
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class, 10L);
      job.getClientSLA().setMaxDispatchSize(6).setAllowMultipleDispatchesToSameChannel(true);
      final DispatchCounterJobListener listener = new DispatchCounterJobListener();
      job.addJobListener(listener);
      final List<Task<?>> results = jppfClient.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      assertTrue(listener.multipleConcurrentDispatches);
    } finally {
      reset();
    }
  }

  /**
   * Test that a job with {@code allowMultipleDispatchesToSameChannel = false} isn't dispatched concurrently to the same channel.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testPreventMultipleDispatchesToSameChannelClient() throws Exception {
    try {
      configure(true, false, 1);
      jppfClient.setLoadBalancerSettings("manual", new TypedProperties().setInt("size", 15));
      BaseSetup.checkDriverAndNodesInitialized(jppfClient, 1, 1);
      final JPPFConnectionPool pool = jppfClient.awaitWorkingConnectionPool();
      pool.setMaxJobs(100);
      print(false, false, "got pool %s", pool);
      final int nbTasks = 50;
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class, 10L);
      job.getClientSLA().setMaxDispatchSize(6).setAllowMultipleDispatchesToSameChannel(false);
      final DispatchCounterJobListener listener = new DispatchCounterJobListener();
      job.addJobListener(listener);
      final List<Task<?>> results = jppfClient.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      assertFalse(listener.multipleConcurrentDispatches);
    } finally {
      reset();
    }
  }

  /**
   * Configure the client.
   * @param remoteEnabled specifies whether remote execution is enabled.
   * @param localEnabled specifies whether local execution is enabled.
   * @param poolSize the size of the connection pool.
   * @throws Exception if any error occurs.
   */
  private void configure(final boolean remoteEnabled, final boolean localEnabled, final int poolSize) throws Exception {
    BaseSetup.resetClientConfig();
    JPPFConfiguration.set(LOAD_BALANCING_ALGORITHM, "manual")
      .set(LOAD_BALANCING_PROFILE, "manual")
      .setInt(LOAD_BALANCING_PROFILE.getName() + ".manual.size", 5)
      .set(POOL_SIZE, poolSize)
      .set(REMOTE_EXECUTION_ENABLED, remoteEnabled)
      .set(LOCAL_EXECUTION_ENABLED, localEnabled)
      .set(LOCAL_EXECUTION_THREADS, Runtime.getRuntime().availableProcessors());
    jppfClient = BaseSetup.createClient(null, false);
  }

  /**
   * Close the client and reset the configuration.
   */
  private void reset() {
    if (jppfClient != null) {
      jppfClient.close();
      jppfClient = null;
    }
    JPPFConfiguration.reset();
  }

  /**
   * 
   */
  static class DispatchCounterJobListener extends JobListenerAdapter {
    /**
     * Dispatch counter.
     */
    final AtomicInteger dispatchCount = new AtomicInteger(0);
    /**
     * A multimap holding the positions of the tasks for each dispatch.
     */
    final CollectionMap<Integer, Integer> dispatchToPositions = new ArrayListHashMap<>();
    /**
     * 
     */
    final Map<Integer, Integer> positionsToDispatch = new HashMap<>();
    /**
     * 
     */
    boolean multipleConcurrentDispatches;
    /**
     * Holds the size of the largest dispatch for the job.
     */
    int maxDispatchSize;

    @Override
    public synchronized void jobDispatched(final JobEvent event) {
      final int n = event.getJobTasks().size();
      if  (n > maxDispatchSize) maxDispatchSize = n;
      final int dispatchNumber = dispatchCount.incrementAndGet();
      final List<Task<?>> tasks = event.getJobTasks();
      for (final Task<?> task: tasks) {
        dispatchToPositions.putValue(dispatchNumber, task.getPosition());
        positionsToDispatch.put(task.getPosition(), dispatchNumber);
      }
      print(false, false, "job '%s' dispatch #%03d with %d tasks ==> got %d dispatches", event.getJob().getName(), dispatchNumber, tasks.size(), dispatchToPositions.keySet().size());
      if (dispatchToPositions.keySet().size() > 1) multipleConcurrentDispatches = true;
    }

    @Override
    public synchronized void jobReturned(final JobEvent event) {
      int dispatchNumber = -1;
      final List<Task<?>> tasks = event.getJobTasks();
      for (final Task<?> task: tasks) {
        if (dispatchNumber <= 0) dispatchNumber = positionsToDispatch.get(task.getPosition());
        dispatchToPositions.removeValue(dispatchNumber, task.getPosition());
        positionsToDispatch.remove(task.getPosition());
      }
      print(false, false, "job '%s' dispatch #%03d returned %d tasks ==> got %d dispatches", event.getJob().getName(), dispatchNumber, tasks.size(), dispatchToPositions.keySet().size());
    }
  }
}
