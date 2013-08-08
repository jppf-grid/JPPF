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

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.*;
import org.jppf.client.event.TaskResultEvent;
import org.jppf.node.policy.*;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.server.protocol.results.SendResultsStrategy;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link org.jppf.node.protocol.JobSLA JobSLA}.
 * In this class, we test that the behavior is the expected one, from the client point of view,
 * as specified in the job SLA.
 * @author Laurent Cohen
 */
public class TestJPPFJobSLA extends Setup1D2N1C
{
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
   * Simply test that a job does expires at a specified date.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAtDate() throws Exception
  {
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, SimpleTask.class, TIME_LONG);
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    Date date = new Date(System.currentTimeMillis() + TIME_SHORT);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(sdf.format(date), DATE_FORMAT));
    List<JPPFTask> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), 1);
    JPPFTask task = results.get(0);
    assertNull(task.getResult());
  }

  /**
   * Test that a job does not expires at a specified date, because it completes before that date.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAtDateTooLate() throws Exception
  {
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, SimpleTask.class, TIME_SHORT);
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    Date date = new Date(System.currentTimeMillis() + TIME_LONG);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(sdf.format(date), DATE_FORMAT));
    List<JPPFTask> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), 1);
    JPPFTask task = results.get(0);
    assertNotNull(task.getResult());
    assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
  }

  /**
   * Simply test that a job does expires after a specified delay.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAfterDelay() throws Exception
  {
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, SimpleTask.class, TIME_LONG);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_SHORT));
    List<JPPFTask> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), 1);
    JPPFTask task = results.get(0);
    assertNull(task.getResult());
  }

  /**
   * Test that a job does not expire after a specified delay, because it completes before that.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExpirationAfterDelayTooLate() throws Exception
  {
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, SimpleTask.class, TIME_SHORT);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_LONG));
    List<JPPFTask> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), 1);
    JPPFTask task = results.get(0);
    assertNotNull(task.getResult());
    assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
  }

  /**
   * Simply test that a suspended job does expires after a specified delay.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testSuspendedJobExpiration() throws Exception
  {
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, SimpleTask.class, TIME_LONG);
    job.getSLA().setSuspended(true);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_SHORT));
    List<JPPFTask> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), 1);
    JPPFTask task = results.get(0);
    assertNull(task.getResult());
  }

  /**
   * Test that a job queued in the client does not expire there.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testMultipleJobsExpiration() throws Exception
  {
    JPPFJob job1 = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + "-1", false, false, 1, SimpleTask.class, TIME_LONG);
    job1.getSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_SHORT));
    JPPFJob job2 = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + "-2", false, false, 1, SimpleTask.class, TIME_SHORT);
    job2.getSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_LONG));
    client.submit(job1);
    client.submit(job2);
    List<JPPFTask> results = ((JPPFResultCollector) job1.getResultListener()).waitForResults();
    assertNotNull(results);
    assertEquals(results.size(), 1);
    JPPFTask task = results.get(0);
    assertNull(task.getResult());
    results = ((JPPFResultCollector) job2.getResultListener()).waitForResults();
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
  @Test(timeout=5000)
  public void testCancelJobUponClientDisconnect() throws Exception
  {
    String fileName = "testCancelJobUponClientDisconnect";
    File f = new File(fileName + ".tmp");
    f.deleteOnExit();
    try
    {
      assertFalse(f.exists());
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, false, 1, FileTask.class, fileName, false);
      job.getSLA().setCancelUponClientDisconnect(false);
      client.submit(job);
      Thread.sleep(1000L);
      client.close();
      Thread.sleep(2000L);
      assertTrue(f.exists());
    }
    finally
    {
      f.delete();
      client = BaseSetup.createClient(null);
    }
  }

  /**
   * Test that a job with a higher priority is executed before a job with a smaller priority.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobPriority() throws Exception
  {
    int nbJobs = 3;
    JPPFJob[] jobs = new JPPFJob[3];
    ExecutionPolicy policy = new Equal("jppf.node.uuid", false, "n1");
    for (int i=0; i<nbJobs; i++)
    {
      jobs[i] = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + i, false, false, 1, LifeCycleTask.class, 750L);
      jobs[i].getSLA().setPriority(i);
      jobs[i].getSLA().setExecutionPolicy(policy);
    }
    for (int i=0; i<nbJobs; i++)
    {
      client.submit(jobs[i]);
      if (i == 0) Thread.sleep(500L);
    }
    List<List<JPPFTask>> results = new ArrayList<>();
    for (int i=0; i<nbJobs; i++) results.add(((JPPFResultCollector) jobs[i].getResultListener()).waitForResults());
    LifeCycleTask t1 = (LifeCycleTask) results.get(1).get(0);
    assertNotNull(t1);
    LifeCycleTask t2 = (LifeCycleTask) results.get(2).get(0);
    assertNotNull(t2);
    assertTrue("3rd job (start=" + t2.getStart() + ") should have started before the 2nd (start=" + t1.getStart() + ")", t2.getStart() < t1.getStart());
  }

  /**
   * Test that a job is only sent to the server according to its execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobExecutionPolicy() throws Exception
  {
    int nbTasks = 10;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class);
    job.getSLA().setExecutionPolicy(new Equal("jppf.node.uuid", false, "n2"));
    List<JPPFTask> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (JPPFTask t: results)
    {
      LifeCycleTask task = (LifeCycleTask) t;
      assertTrue("n2".equals(task.getNodeUuid()));
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
  }

  /**
   * Test that a job is only executed on one node at a time.
   * @throws Exception if any error occurs.
   */
  //@Test(timeout=8000)
  public void testJobMaxNodes() throws Exception
  {
    int nbTasks = 10;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 250L);
    job.getSLA().setMaxNodes(1);
    List<JPPFTask> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    // check that no 2 tasks were executing at the same time on different nodes
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

  /**
   * Test that a job is executed on both nodes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testJobMaxNodes2() throws Exception
  {
    int nbTasks = Math.max(2*Runtime.getRuntime().availableProcessors(), 10);
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 250L);
    job.getSLA().setMaxNodes(2);
    List<JPPFTask> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    boolean found = false;
    // check that at least 2 tasks were executing at the same time on different nodes
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

  /**
   * Test that a broadcast job is executed on all nodes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testBroadcastJob() throws Exception
  {
    String suffix = "node-";
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, true, 1, FileTask.class, suffix, true);
    job.getSLA().setMaxNodes(2);
    List<JPPFTask> results = client.submit(job);
    for (int i=1; i<=2; i++)
    {
      File file = new File("node-n" + i + ".tmp");
      assertTrue("file '" + file + "' does not exist", file.exists());
      file.delete();
    }
  }

  /**
   * Test that a broadcast job is executed on all nodes,
   * event though another job is already executing at the time it is submitted.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testBroadcastJob2() throws Exception
  {
    try
    {
      client.close();
      TypedProperties config = JPPFConfiguration.getProperties();
      config.setProperty("jppf.pool.size", "2");
      client = BaseSetup.createClient(null, false);
      String methodName = ReflectionUtils.getCurrentMethodName();
      JPPFJob job1 = BaseTestHelper.createJob(methodName + "-normal", false, false, 10, LifeCycleTask.class, 1000L);
      job1.getSLA().setPriority(1000);
      String suffix = "broadcast-node-";
      JPPFJob job2 = BaseTestHelper.createJob(methodName + "-broadcast", false, true, 1, FileTask.class, suffix, true);
      job2.getSLA().setPriority(-1000);
      client.submit(job1);
      Thread.sleep(500L);
      client.submit(job2);
      JPPFResultCollector collector = (JPPFResultCollector) job1.getResultListener();
      collector.waitForResults();
      collector = (JPPFResultCollector) job2.getResultListener();
      collector.waitForResults();
      for (int i=1; i<=2; i++)
      {
        File file = new File(suffix + "n" + i + ".tmp");
        assertTrue("file '" + file + "' does not exist", file.exists());
        file.delete();
      }
    }
    finally
    {
      client.close();
      client = BaseSetup.createClient(null, true);
    }
  }

  /**
   * Test that results are returned according to the SendNodeResultsStrategy specified in the SLA.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testSendNodeResultsStrategy() throws Exception
  {
    checkSendResultsStrategy(ReflectionUtils.getCurrentMethodName(), SendResultsStrategy.NODE_RESULTS, 4);
  }

  /**
   * Test that results are returned according to the SendAllResultsStrategy specified in the SLA.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testSendAllResultsStrategy() throws Exception
  {
    checkSendResultsStrategy(ReflectionUtils.getCurrentMethodName(), SendResultsStrategy.ALL_RESULTS, 1);
  }

  /**
   * Test that results are returned according to the default strategy (SendNodeResultsStrategy).
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testDefaultSendResultsStrategy() throws Exception
  {
    checkSendResultsStrategy(ReflectionUtils.getCurrentMethodName(), null, 4);
  }

  /**
   * Test that results are returned according to the specified strategy.
   * @param jobName the name the job to execute.
   * @param strategyName the name of the strategy to test.
   * @param expectedReturnedCount the expected number of 'job returned' notifications.
   * @throws Exception if any error occurs.
   */
  public void checkSendResultsStrategy(final String jobName, final String strategyName, final int expectedReturnedCount) throws Exception
  {
    int nbTasks = 20;
    JPPFJob job = BaseTestHelper.createJob(jobName, true, false, nbTasks, LifeCycleTask.class, 1L);
    final AtomicInteger returnedCount = new AtomicInteger(0);
    JPPFResultCollector collector = new JPPFResultCollector(job) {
      @Override
      public synchronized void resultsReceived(final TaskResultEvent event) {
        super.resultsReceived(event);
        returnedCount.incrementAndGet();
      }
    };
    job.setResultListener(collector);
    job.getSLA().setResultsStrategy(strategyName);
    List<JPPFTask> results = client.submit(job);
    assertEquals(expectedReturnedCount, returnedCount.get());
  }

  /**
   * A task that creates a file.
   */
  public static class FileTask extends JPPFTask
  {
    /**
     * 
     */
    private final String filePath;
    /**
     * 
     */
    private final boolean appendNodeSuffix;

    /**
     * Initialize this task with the specified file path.
     * @param filePath the path of the file to create.
     * @param appendNodeSuffix <code>true</code> to append the node name to the file's name, <code>false</code> otherwise.
     */
    public FileTask(final String filePath, final boolean appendNodeSuffix)
    {
      this.filePath = filePath;
      this.appendNodeSuffix = appendNodeSuffix;
    }

    @Override
    public void run()
    {
      try
      {
        String name = filePath;
        if (appendNodeSuffix) name = name + JPPFConfiguration.getProperties().getString("jppf.node.uuid");
        name = name + ".tmp";

        File f = new File(name);
        Thread.sleep(2000L);
        Writer writer = new FileWriter(f);
        StreamUtils.closeSilent(writer);
      }
      catch (Exception e)
      {
        setException(e);
        e.printStackTrace();
      }
    }
  }
}
