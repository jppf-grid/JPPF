/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.text.SimpleDateFormat;
import java.util.*;

import org.jppf.client.*;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link org.jppf.node.protocol.JobClientSLA JobClientSLA}.
 * In this class, we test that the behavior is the expected one, from the client point of view,
 * as specified in the job SLA.
 * @author Laurent Cohen
 */
public class TestJPPFJobClientSLA extends Setup1D1N
{
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
  private JPPFClient client = null;

  /**
   * Simply test that a job does expires at a specified date.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAtDateClient() throws Exception
  {
    try
    {
      configure(false, true, 1);
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, SimpleTask.class, TIME_LONG);
      SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
      Date date = new Date(System.currentTimeMillis() + TIME_SHORT);
      job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(sdf.format(date), DATE_FORMAT));
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(results.size(), 1);
      Task<?> task = results.get(0);
      assertNull(task.getResult());
    }
    finally
    {
      reset();
    }
  }

  /**
   * Test that a job does not expire at a specified date, because it completes before that date.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAtDateTooLateClient() throws Exception
  {
    try
    {
      configure(false, true, 1);
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, SimpleTask.class, TIME_SHORT);
      SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
      Date date = new Date(System.currentTimeMillis() + TIME_LONG);
      job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(sdf.format(date), DATE_FORMAT));
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(results.size(), 1);
      Task<?> task = results.get(0);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
    finally
    {
      reset();
    }
  }

  /**
   * Simply test that a job does expires after a specified delay.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAfterDelayClient() throws Exception
  {
    try
    {
      configure(false, true, 1);
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, SimpleTask.class, TIME_LONG);
      job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_SHORT));
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(results.size(), 1);
      Task<?> task = results.get(0);
      assertNull(task.getResult());
    }
    finally
    {
      reset();
    }
  }

  /**
   * Test that a job does not expire after a specified delay, because it completes before that.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAfterDelayTooLateClient() throws Exception
  {
    try
    {
      configure(false, true, 1);
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, SimpleTask.class, TIME_SHORT);
      job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_LONG));
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(results.size(), 1);
      Task<?> task = results.get(0);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
    finally
    {
      reset();
    }
  }

  /**
   * Test that a job queued in the client does not expire there.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testMultipleJobsExpirationClient() throws Exception
  {
    try
    {
      configure(false, true, 1);
      String methodName = ReflectionUtils.getCurrentMethodName();
      JPPFJob job1 = BaseTestHelper.createJob(methodName + "-1", false, false, 1, SimpleTask.class, TIME_LONG);
      job1.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_SHORT));
      JPPFJob job2 = BaseTestHelper.createJob(methodName + "-2", false, false, 1, SimpleTask.class, TIME_SHORT);
      job2.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_LONG));
      client.submitJob(job1);
      client.submitJob(job2);
      List<Task<?>> results = ((JPPFResultCollector) job1.getResultListener()).awaitResults();
      assertNotNull(results);
      assertEquals(results.size(), 1);
      Task<?> task = results.get(0);
      assertNull(task.getResult());
      results = ((JPPFResultCollector) job2.getResultListener()).awaitResults();
      assertNotNull(results);
      assertEquals(results.size(), 1);
      task = results.get(0);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
    finally
    {
      reset();
    }
  }

  /**
   * Test that a job is only sent to the server according to its execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobInNodeExecutionPolicyClient() throws Exception
  {
    try
    {
      configure(true, true, 1);
      BaseSetup.checkDriverAndNodesInitialized(client, 1, 1);
      int nbTasks = 10;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class);
      job.getClientSLA().setExecutionPolicy(new Equal("jppf.channel.local", false));
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      for (Task<?> t: results)
      {
        LifeCycleTask task = (LifeCycleTask) t;
        assertTrue(task.isExecutedInNode());
        assertNotNull(task.getResult());
        assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
      }
    }
    finally
    {
      reset();
    }
  }

  /**
   * Test that a job is only executed locally in the client according to its execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobLocalExecutionPolicyClient() throws Exception
  {
    try
    {
      configure(true, true, 1);
      BaseSetup.checkDriverAndNodesInitialized(client, 1, 1);
      int nbTasks = 10;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class);
      job.getClientSLA().setExecutionPolicy(new Equal("jppf.channel.local", true));
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      for (Task<?> t: results)
      {
        LifeCycleTask task = (LifeCycleTask) t;
        assertFalse(task.isExecutedInNode());
        assertNotNull(task.getResult());
        assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
      }
    }
    finally
    {
      reset();
    }
  }

  /**
   * Test that a job is only executed on one channel at a time, either local or remote.
   * @throws Exception if any error occurs.
   */
  //@Test(timeout=8000)
  public void testJobMaxChannelsClient() throws Exception
  {
    try
    {
      configure(true, true, 1);
      BaseSetup.checkDriverAndNodesInitialized(client, 1, 1);
      int nbTasks = 10;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 250L);
      job.getClientSLA().setMaxChannels(1);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      // check that no 2 tasks were executing at the same time on different channels
      for (int i=0; i<results.size()-1; i++)
      {
        LifeCycleTask t1 = (LifeCycleTask) results.get(i);
        Range<Double> r1 = new Range<>(t1.getStart(), t1.getStart() + t1.getElapsed());
        for (int j=i+1; j<results.size(); j++)
        {
          LifeCycleTask t2 = (LifeCycleTask) results.get(j);
          Range<Double> r2 = new Range<>(t2.getStart(), t2.getStart() + t2.getElapsed());
          assertFalse("r1=" + r1 + ", r2=" + r2 + ", uuid1=" + t1.getNodeUuid() + ", uuid2=" + t2.getNodeUuid(), 
            r1.intersects(r2, false) && !t1.getNodeUuid().equals(t2.getNodeUuid()));
        }
      }
    }
    finally
    {
      reset();
    }
  }

  /**
   * Test that a job is executed on both local and remote channels.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobMaxChannels2Client() throws Exception
  {
    try
    {
      configure(true, true, 1);
      BaseSetup.checkDriverAndNodesInitialized(client, 1, 1);
      int nbTasks = Math.max(2*Runtime.getRuntime().availableProcessors(), 10);
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 500L);
      job.getClientSLA().setMaxChannels(2);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      boolean found = false;
      // check that no 2 tasks were executing at the same time on different channels
      for (int i=0; i<results.size()-1; i++)
      {
        LifeCycleTask t1 = (LifeCycleTask) results.get(i);
        Range<Double> r1 = new Range<>(t1.getStart(), t1.getStart() + t1.getElapsed());
        for (int j=i+1; j<results.size(); j++)
        {
          LifeCycleTask t2 = (LifeCycleTask) results.get(j);
          Range<Double> r2 = new Range<>(t2.getStart(), t2.getStart() + t2.getElapsed());
          if (r1.intersects(r2) && !t1.getNodeUuid().equals(t2.getNodeUuid()))
          {
            found = true;
            break;
          }
        }
        if (found) break;
      }
      assertTrue(found);
    }
    finally
    {
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
  private void configure(final boolean remoteEnabled, final boolean localEnabled, final int poolSize) throws Exception
  {
    TypedProperties config = JPPFConfiguration.getProperties();
    config.setProperty("jppf.remote.execution.enabled", String.valueOf(remoteEnabled));
    config.setProperty("jppf.local.execution.enabled", String.valueOf(localEnabled));
    config.setProperty("jppf.local.execution.threads", String.valueOf(Runtime.getRuntime().availableProcessors()));
    config.setProperty("jppf.load.balancing.algorithm", "manual");
    config.setProperty("jppf.load.balancing.profile", "manual");
    config.setProperty("jppf.load.balancing.profile.manual.size", "5");
    config.setProperty("jppf.pool.size", String.valueOf(poolSize));
    client = BaseSetup.createClient(null, false);
  }

  /**
   * Close the client and reset the configuration.
   */
  private void reset()
  {
    if (client != null)
    {
      client.close();
      client = null;
    }
    JPPFConfiguration.reset();
  }
}
