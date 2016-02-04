/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import static org.jppf.utils.ReflectionUtils.getCurrentMethodName;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.job.*;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.*;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.*;
import org.junit.Test;
import org.slf4j.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link Task}.
 * In this class, we test that the functionality of the DriverJobManagementMBean from the client point of view.
 * @author Laurent Cohen
 */
public class TestDriverJobManagementMBean extends Setup1D2N1C {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(TestDriverJobManagementMBean.class);
  /**
   * A "short" duration for this test.
   */
  private static final long TIME_SHORT = 1000L;

  /**
   * We test a job with 1 task, and attempt to cancel it before completion.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testCancelJob() throws Exception {
    int nbTasks = 10;
    JPPFJob job = BaseTestHelper.createJob(getCurrentMethodName(), false, false, nbTasks, LifeCycleTask.class, 5000L);
    client.submitJob(job);
    Thread.sleep(TIME_SHORT);
    DriverJobManagementMBean proxy = BaseSetup.getJobManagementProxy(client);
    assertNotNull(proxy);
    proxy.cancelJob(job.getUuid());
    List<Task<?>> results = job.awaitResults();
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (Task<?> t : results) assertNull(t.getResult());
  }

  /**
   * Test 2 jobs and attempt to cancel them using an all jobs selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testCancelJobsWithAllJobsSelector() throws Exception {
    List<JPPFJob> jobs = createMultipleJobs(2, 10, 5000L, ReflectionUtils.getCurrentClassAndMethod(), false);
    testJobSelectorAction(new JobSelectorAction.CancelAction(jobs, new AllJobsSelector()));
  }

  /**
   * Test 2 jobs and attempt to cancel them using a job uuid selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testCancelJobsWithJobUuidSelector() throws Exception {
    List<JPPFJob> jobs = createMultipleJobs(2, 10, 5000L, ReflectionUtils.getCurrentClassAndMethod(), false);
    List<String> uuids = new ArrayList<>();
    for (JPPFJob job: jobs) uuids.add(job.getUuid());
    testJobSelectorAction(new JobSelectorAction.CancelAction(jobs, new JobUuidSelector(uuids)));
  }

  /**
   * Test 2 jobs and attempt to cancel them using an all jobs selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testCancelJobsWithScriptedJobSelector() throws Exception {
    String prefix = ReflectionUtils.getCurrentClassAndMethod();
    List<JPPFJob> jobs = createMultipleJobs(2, 10, 5000L, prefix, false);
    JobSelector selector = new ScriptedJobSelector("javascript", "jppfJob.getName().startsWith('" + prefix + "')");
    testJobSelectorAction(new JobSelectorAction.CancelAction(jobs, selector));
  }

  /**
   * Test 2 suspended jobs jobs and attempt to resume them using an all jobs selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testResumeJobsWithAllJobsSelector() throws Exception {
    List<JPPFJob> jobs = createMultipleJobs(2, 10, 1L, ReflectionUtils.getCurrentClassAndMethod(), true);
    testJobSelectorAction(new JobSelectorAction.ResumeAction(jobs, new AllJobsSelector()));
  }

  /**
   * Test 2 suspended jobs jobs and attempt to resume them using a job uuid selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testResumeJobsWithJobUuidSelector() throws Exception {
    List<JPPFJob> jobs = createMultipleJobs(2, 10, 1L, ReflectionUtils.getCurrentClassAndMethod(), true);
    List<String> uuids = new ArrayList<>();
    for (JPPFJob job: jobs) uuids.add(job.getUuid());
    testJobSelectorAction(new JobSelectorAction.ResumeAction(jobs, new JobUuidSelector(uuids)));
  }

  /**
   * Test 2 suspended jobs jobs and attempt to resume them using an all jobs selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testResumeJobsWithScriptedJobSelector() throws Exception {
    String prefix = ReflectionUtils.getCurrentClassAndMethod();
    List<JPPFJob> jobs = createMultipleJobs(2, 10, 1L, prefix, true);
    JobSelector selector = new ScriptedJobSelector("javascript", "jppfJob.getName().startsWith('" + prefix + "')");
    testJobSelectorAction(new JobSelectorAction.ResumeAction(jobs, selector));
  }

  /**
   * Test 2 jobs and attempt to suspend them using an all jobs selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testSuspendJobsWithAllJobsSelector() throws Exception {
    List<JPPFJob> jobs = createMultipleJobs(2, 4, 500L, ReflectionUtils.getCurrentClassAndMethod(), false);
    testJobSelectorAction(new JobSelectorAction.SuspendAction(jobs, new AllJobsSelector()));
  }

  /**
   * Test 2 jobs and attempt to suspend them using a job uuid selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testSuspendJobsWithJobUuidSelector() throws Exception {
    List<JPPFJob> jobs = createMultipleJobs(2, 4, 500L, ReflectionUtils.getCurrentClassAndMethod(), false);
    List<String> uuids = new ArrayList<>();
    for (JPPFJob job: jobs) uuids.add(job.getUuid());
    testJobSelectorAction(new JobSelectorAction.SuspendAction(jobs, new JobUuidSelector(uuids)));
  }

  /**
   * Test 2 jobs and attempt to suspend them using an all jobs selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testSuspendJobsWithScriptedJobSelector() throws Exception {
    String prefix = ReflectionUtils.getCurrentClassAndMethod();
    List<JPPFJob> jobs = createMultipleJobs(2, 4, 500L, prefix, false);
    JobSelector selector = new ScriptedJobSelector("javascript", "jppfJob.getName().startsWith('" + prefix + "')");
    testJobSelectorAction(new JobSelectorAction.SuspendAction(jobs, selector));
  }

  /**
   * Test 2 jobs and check their node dispatches.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testGetNodeInfoWithAllJobsSelector() throws Exception {
    List<JPPFJob> jobs = createMultipleJobs(2, 4, 500L, ReflectionUtils.getCurrentClassAndMethod(), false);
    testJobSelectorAction(new JobSelectorAction.NodeJobInformationAction(jobs, new AllJobsSelector()));
  }

  /**
   * Test 2 jobs and check their node dispatches.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testGetNodeInfoWithJobUuidSelector() throws Exception {
    List<JPPFJob> jobs = createMultipleJobs(2, 4, 500L, ReflectionUtils.getCurrentClassAndMethod(), false);
    List<String> uuids = new ArrayList<>();
    for (JPPFJob job: jobs) uuids.add(job.getUuid());
    testJobSelectorAction(new JobSelectorAction.NodeJobInformationAction(jobs, new JobUuidSelector(uuids)));
  }

  /**
   * Test 2 jobs and check their node dispatches.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testGetNodeInfoWithScriptedJobSelector() throws Exception {
    String prefix = ReflectionUtils.getCurrentClassAndMethod();
    List<JPPFJob> jobs = createMultipleJobs(2, 4, 500L, prefix, false);
    JobSelector selector = new ScriptedJobSelector("javascript", "jppfJob.getName().startsWith('" + prefix + "')");
    testJobSelectorAction(new JobSelectorAction.NodeJobInformationAction(jobs, selector));
  }

  /**
   * Test 2 jobs and check their node dispatches.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testUpdatePriorityAndMaxNodesWithAllJobsSelector() throws Exception {
    List<JPPFJob> jobs = createMultipleJobs(2, 4, 1L, ReflectionUtils.getCurrentClassAndMethod(), true);
    testJobSelectorAction(new JobSelectorAction.UpdatePriorityAndMaxNodesAction(jobs, new AllJobsSelector()));
  }

  /**
   * Test 2 jobs and check their node dispatches.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testUpdatePriorityAndMaxNodesWithJobUuidSelector() throws Exception {
    List<JPPFJob> jobs = createMultipleJobs(2, 4, 1L, ReflectionUtils.getCurrentClassAndMethod(), true);
    List<String> uuids = new ArrayList<>();
    for (JPPFJob job: jobs) uuids.add(job.getUuid());
    testJobSelectorAction(new JobSelectorAction.UpdatePriorityAndMaxNodesAction(jobs, new JobUuidSelector(uuids)));
  }

  /**
   * Test 2 jobs and check their node dispatches.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000L)
  public void testUpdatePriorityAndMaxNodesWithScriptedJobSelector() throws Exception {
    String prefix = ReflectionUtils.getCurrentClassAndMethod();
    List<JPPFJob> jobs = createMultipleJobs(2, 4, 1L, prefix, true);
    JobSelector selector = new ScriptedJobSelector("javascript", "jppfJob.getName().startsWith('" + prefix + "')");
    testJobSelectorAction(new JobSelectorAction.UpdatePriorityAndMaxNodesAction(jobs, selector));
  }

  /**
   * Create the specified number of jobs with the specified number of tasks.
   * @param nbJobs the number of jobs to create.
   * @param nbTasks the number of tasks to add to each job.
   * @param taskDuration the duration of each task in millis.
   * @param namePrefix the prefix of the job names.
   * @param suspended whether the job is submitted in suspended state.
   * @return a list of {@link JPPFJob} instances.
   * @throws Exception if any error occurs.
   */
  private List<JPPFJob> createMultipleJobs(final int nbJobs, final int nbTasks, final long taskDuration, final String namePrefix, final boolean suspended) throws Exception {
    List<JPPFJob> jobs = new ArrayList<>();
    for (int i=1; i<= nbJobs; i++) {
      JPPFJob job = BaseTestHelper.createJob(namePrefix + '-' + i, false, false, nbTasks, LifeCycleTask.class, taskDuration);
      job.getSLA().setSuspended(suspended);
      jobs.add(job);
    }
    return jobs;
  }

  /**
   * Test the submission and cancellation of the specified jobs filtered by the specified job selector.
   * @param action the action to run.
   * @throws Exception if any error occurs.
   */
  private void testJobSelectorAction(final JobSelectorAction action) throws Exception {
    List<JPPFJob> jobs = action.getJobs();
    JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
    try {
      pool.setSize(jobs.size());
      pool.awaitWorkingConnections(Operator.EQUAL, jobs.size());
      for (JPPFJob job: jobs) client.submitJob(job);
      action.call();
    } finally {
      log.info("setting pool size to 1");
      pool.setSize(1);
      pool.awaitWorkingConnections(Operator.EQUAL, 1);
    }
  }

  /**
   * We test a job with 1 task, and attempt to cancel it after it has completed.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testCancelJobAfterCompletion() throws Exception {
    JPPFJob job = BaseTestHelper.createJob(getCurrentMethodName(), true, false, 1, LifeCycleTask.class, TIME_SHORT);
    List<Task<?>> results = client.submitJob(job);
    assertEquals(1, results.size());
    assertNotNull(results.get(0));
    assertNotNull(results.get(0).getResult());
    DriverJobManagementMBean proxy = BaseSetup.getJobManagementProxy(client);
    assertNotNull(proxy);
    proxy.cancelJob(job.getUuid());
  }

  /**
   * We test that no job remains in the server's queue after resuming, then cancelling an initially suspended job.
   * See <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-126">JPPF-126 Job cancelled from the admin console may get stuck in the server queue</a>.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testResumeAndCancelSuspendedJob() throws Exception {
    int nbTasks = 2;
    JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection();
    assertNotNull(driver);
    DriverJobManagementMBean jobManager = driver.getJobManager();
    assertNotNull(jobManager);
    JPPFJob job = BaseTestHelper.createJob(getCurrentMethodName(), false, false, nbTasks, LifeCycleTask.class, 3500L);
    job.getSLA().setSuspended(true);
    client.submitJob(job);
    Thread.sleep(1500L);
    jobManager.resumeJob(job.getUuid());
    Thread.sleep(1000L);
    jobManager.cancelJob(job.getUuid());
    List<Task<?>> results = job.awaitResults();
    assertEquals(nbTasks, results.size());
    Thread.sleep(1000L);
    String[] ids = jobManager.getAllJobUuids();
    assertNotNull(ids);
    assertEquals("the driver's job queue should be empty", 0, ids.length);
  }

  /**
   * Test that a dynamic update of the job SLA is taken into account.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testUpdateJobSLAAndMetadata() throws Exception {
    int nbTasks = 2;
    JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection();
    assertNotNull(driver);
    LoadBalancingInformation info = driver.loadBalancerInformation();
    assertNotNull(info);
    try {
      TypedProperties props = new TypedProperties();
      props.setInt("size", 1);
      driver.changeLoadBalancerSettings("manual", props);
      JPPFJob job = BaseTestHelper.createJob(getCurrentMethodName(), false, false, nbTasks, LifeCycleTask.class, 2000L);
      for (Task<?> t: job) ((LifeCycleTask) t).setFetchMetadata(true);
      final JobSLA sla = job.getSLA();
      final JobMetadata metadata = job.getMetadata();
      sla.setMaxNodes(1);
      sla.setExecutionPolicy(new Equal("jppf.node.uuid", false, "n1"));
      metadata.setParameter("node.uuid", "n1");
      final DriverJobManagementMBean jobManager = driver.getJobManager();
      assertNotNull(jobManager);
      final AtomicInteger count = new AtomicInteger(0);
      jobManager.addNotificationListener(new NotificationListener() {
        @Override
        public void handleNotification(final Notification notification, final Object handback) {
          JobNotification notif = (JobNotification) notification;
          if (notif.getEventType() == JobEventType.JOB_DISPATCHED) {
            int n = count.incrementAndGet();
            String actualUuid = notif.getNodeInfo().getUuid();
            String expectedUuid = "n" + n;
            assertEquals(String.format("job should have been dispatched to node '%s' but was dispatched to '%s'", expectedUuid, actualUuid), expectedUuid, actualUuid);
            if (n == 1) {
              sla.setExecutionPolicy(new Equal("jppf.node.uuid", false, "n2"));
              metadata.setParameter("node.uuid", "n2");
              jobManager.updateJobs(JobSelector.ALL_JOBS, sla, metadata);
            }
          }
        }
      }, null, null);
      client.submitJob(job);
      List<Task<?>> results = job.awaitResults();
      assertEquals(nbTasks, results.size());
      List<String> nodeUuids = new ArrayList<>();
      for (int i=0; i<nbTasks; i++) {
        Task<?> t = results.get(i);
        assertTrue(t instanceof LifeCycleTask);
        LifeCycleTask task = (LifeCycleTask) t;
        assertNotNull(task.getResult());
        assertNull(task.getThrowable());
        nodeUuids.add(task.getNodeUuid());
        assertNotNull(task.getMetadata());
        assertEquals(task.getNodeUuid(), task.getMetadata().getParameter("node.uuid"));
      }
      Collections.sort(nodeUuids);
      assertEquals(nbTasks, nodeUuids.size());
      for (int i=0; i<nbTasks; i++) assertEquals("n" + (i + 1), nodeUuids.get(i));
    } finally {
      driver.changeLoadBalancerSettings(info.getAlgorithm(), info.getParameters());
    }
  }
}
