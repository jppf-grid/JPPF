/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package test.org.jppf.server.job.management;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.server.protocol.JPPFTask;
import org.junit.Test;

import test.org.jppf.test.setup.*;

/**
 * Unit tests for {@link JPPFTask}.
 * In this class, we test that the functionality of the DriverJobManagementMBean from the client point of view.
 * @author Laurent Cohen
 */
public class TestDriverJobManagementMBean extends Setup1D1N1C
{
  /**
   * Count of the number of jobs created.
   */
  private static final  AtomicInteger JOB_COUNT = new AtomicInteger(0);
  /**
   * A "short" duration for this test.
   */
  private static final long TIME_SHORT = 1000L;
  /**
   * A "long" duration for this test.
   */
  private static final long TIME_LONG = 3000L;

  /**
   * We test a job with 1 task, and attempt to cancel it after it has completed.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000L)
  public void testCancelJob() throws Exception
  {
    int nbTasks = 10;
    JPPFJob job = createJob("testCancelJob", nbTasks, TIME_LONG, false);
    JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
    client.submit(job);
    Thread.sleep(TIME_SHORT);
    DriverJobManagementMBean proxy = BaseSetup.getJobManagementProxy(client);
    assertNotNull(proxy);
    proxy.cancelJob(job.getUuid());
    List<JPPFTask> results = collector.waitForResults();
    assertEquals(results.size(), nbTasks);
    assertNotNull(results.get(0));
    int count = 0;
    for (JPPFTask t: results)
    {
      LifeCycleTask task = (LifeCycleTask) t;
      if (task.getResult() == null) count++;
    }
    assertTrue(count > 0);
  }

  /**
   * We test a job with 1 task, and attempt to cancel it after it has completed.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testCancelJobAfterCompletion() throws Exception
  {
    JPPFJob job = createJob("testCancelJobAfterCompletion", 1, TIME_SHORT, true);
    List<JPPFTask> results = client.submit(job);
    assertEquals(results.size(), 1);
    assertNotNull(results.get(0));
    LifeCycleTask task = (LifeCycleTask) results.get(0);
    assertNotNull(task.getResult());
    DriverJobManagementMBean proxy = BaseSetup.getJobManagementProxy(client);
    assertNotNull(proxy);
    proxy.cancelJob(job.getUuid());
  }

  /**
   * Create a job with the specified number of tasks, each with the specified duration.
   * @param jobName the job id.
   * @param nbTasks the number of tasks in the job.
   * @param duration the duration of each task.
   * @param blocking specifies whether the job is blocking or not.
   * @return a {@link JPPFJob} instance.
   * @throws JPPFException if an error occurs while creating the job.
   */
  protected synchronized JPPFJob createJob(final String jobName, final int nbTasks, final long duration, final boolean blocking) throws JPPFException
  {
    JPPFJob job = new JPPFJob();
    job.setName(jobName + " (" + JOB_COUNT.incrementAndGet() + ')');
    for (int i=1; i<=nbTasks; i++)
    {
      JPPFTask task = new LifeCycleTask(duration);
      task.setId(job.getName()  + " - task " + i);
      job.addTask(task);
    }
    job.setBlocking(blocking);
    if (!blocking) job.setResultListener(new JPPFResultCollector(job));
    return job;
  }
}
