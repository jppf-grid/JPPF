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

package test.org.jppf.server.job.management;

import static org.jppf.utils.ReflectionUtils.getCurrentMethodName;
import static org.junit.Assert.*;

import java.util.*;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.job.*;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.*;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.*;
import org.jppf.utils.Operator;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.junit.*;
import org.slf4j.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link DriverJobManagementMBean}.
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
   * Test timeout.
   */
  private static final long TEST_TIMEOUT = 20_000L;

  /**
   * @throws Exception if any error occurs.
   */
  @Before
  public void showIdleNodes() throws Exception {
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection(client);
    final JPPFNodeForwardingMBean forwarder = jmx.getNodeForwarder();
    // cancel currently executing jobs in all nodes
    forwarder.forwardInvoke(NodeSelector.ALL_NODES, "org.jppf:name=debug,type=node", "cancel");
    print(false, "nb idle nodes = %d", jmx.nbIdleNodes());
    ConcurrentUtils.awaitCondition(new ConcurrentUtils.Condition() {
      @Override
      public boolean evaluate() {
        try {
          return jmx.nbIdleNodes() == 2;
        } catch(@SuppressWarnings("unused") final Exception e) {
          return false;
        }
      }
    }, 5000L, 500L, true);
  }

  /**
   * We test a job with 1 task, and attempt to cancel it before completion.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testCancelJob() throws Exception {
    final int nbTasks = 10;
    final JPPFJob job = BaseTestHelper.createJob(getCurrentMethodName(), false, false, nbTasks, LifeCycleTask.class, 5000L);
    final AwaitJobNotificationListener listener = new AwaitJobNotificationListener(client, JobEventType.JOB_DISPATCHED);
    client.submitJob(job);
    listener.await();
    final DriverJobManagementMBean proxy = BaseSetup.getJobManagementProxy(client);
    assertNotNull(proxy);
    proxy.cancelJob(job.getUuid());
    final List<Task<?>> results = job.awaitResults();
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (final Task<?> t : results) assertNull(t.getResult());
  }

  /**
   * Test 2 jobs and attempt to cancel them using an all jobs selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testCancelJobsWithAllJobsSelector() throws Exception {
    final List<JPPFJob> jobs = createMultipleJobs(2, 1, 4000L, ReflectionUtils.getCurrentMethodName(), false);
    testJobSelectorAction(new JobSelectorAction.CancelAction(jobs, JobSelector.ALL_JOBS));
  }

  /**
   * Test 2 jobs and attempt to cancel them using a job uuid selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testCancelJobsWithJobUuidSelector() throws Exception {
    final List<JPPFJob> jobs = createMultipleJobs(2, 1, 4000L, ReflectionUtils.getCurrentMethodName(), false);
    final List<String> uuids = new ArrayList<>();
    for (final JPPFJob job: jobs) uuids.add(job.getUuid());
    testJobSelectorAction(new JobSelectorAction.CancelAction(jobs, new JobUuidSelector(uuids)));
  }

  /**
   * Test 2 jobs and attempt to cancel them using an all jobs selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testCancelJobsWithScriptedJobSelector() throws Exception {
    final String prefix = ReflectionUtils.getCurrentMethodName();
    final List<JPPFJob> jobs = createMultipleJobs(2, 1, 4000L, prefix, false);
    final JobSelector selector = new ScriptedJobSelector("javascript", "jppfJob.getName().startsWith('" + prefix + "')");
    testJobSelectorAction(new JobSelectorAction.CancelAction(jobs, selector));
  }

  /**
   * Test 2 suspended jobs jobs and attempt to resume them using an all jobs selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testResumeJobsWithAllJobsSelector() throws Exception {
    final List<JPPFJob> jobs = createMultipleJobs(2, 10, 1L, ReflectionUtils.getCurrentMethodName(), true);
    testJobSelectorAction(new JobSelectorAction.ResumeAction(jobs, JobSelector.ALL_JOBS));
  }

  /**
   * Test 2 suspended jobs jobs and attempt to resume them using a job uuid selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testResumeJobsWithJobUuidSelector() throws Exception {
    final List<JPPFJob> jobs = createMultipleJobs(2, 10, 1L, ReflectionUtils.getCurrentMethodName(), true);
    final List<String> uuids = new ArrayList<>();
    for (final JPPFJob job: jobs) uuids.add(job.getUuid());
    testJobSelectorAction(new JobSelectorAction.ResumeAction(jobs, new JobUuidSelector(uuids)));
  }

  /**
   * Test 2 suspended jobs jobs and attempt to resume them using an all jobs selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testResumeJobsWithScriptedJobSelector() throws Exception {
    final String prefix = ReflectionUtils.getCurrentMethodName();
    final List<JPPFJob> jobs = createMultipleJobs(2, 10, 1L, prefix, true);
    final JobSelector selector = new ScriptedJobSelector("javascript", "jppfJob.getName().startsWith('" + prefix + "')");
    testJobSelectorAction(new JobSelectorAction.ResumeAction(jobs, selector));
  }

  /**
   * Test 2 jobs and attempt to suspend them using an all jobs selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testSuspendJobsWithAllJobsSelector() throws Exception {
    final List<JPPFJob> jobs = createMultipleJobs(2, 4, 5000L, ReflectionUtils.getCurrentMethodName(), false);
    testJobSelectorAction(new JobSelectorAction.SuspendAction(jobs, JobSelector.ALL_JOBS));
  }

  /**
   * Test 2 jobs and attempt to suspend them using a job uuid selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testSuspendJobsWithJobUuidSelector() throws Exception {
    final List<JPPFJob> jobs = createMultipleJobs(2, 4, 5000L, ReflectionUtils.getCurrentMethodName(), false);
    final List<String> uuids = new ArrayList<>();
    for (final JPPFJob job: jobs) uuids.add(job.getUuid());
    testJobSelectorAction(new JobSelectorAction.SuspendAction(jobs, new JobUuidSelector(uuids)));
  }

  /**
   * Test 2 jobs and attempt to suspend them using an all jobs selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testSuspendJobsWithScriptedJobSelector() throws Exception {
    final String prefix = ReflectionUtils.getCurrentMethodName();
    final List<JPPFJob> jobs = createMultipleJobs(2, 4, 5000L, prefix, false);
    final JobSelector selector = new ScriptedJobSelector("javascript", "jppfJob.getName().startsWith('" + prefix + "')");
    testJobSelectorAction(new JobSelectorAction.SuspendAction(jobs, selector));
  }

  /**
   * Test 2 jobs and check their node dispatches.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGetNodeInfoWithAllJobsSelector() throws Exception {
    final List<JPPFJob> jobs = createMultipleJobs(2, 4, 5000L, ReflectionUtils.getCurrentMethodName(), false);
    testJobSelectorAction(new JobSelectorAction.NodeJobInformationAction(jobs, JobSelector.ALL_JOBS));
  }

  /**
   * Test 2 jobs and check their node dispatches.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGetNodeInfoWithJobUuidSelector() throws Exception {
    final List<JPPFJob> jobs = createMultipleJobs(2, 4, 5000L, ReflectionUtils.getCurrentMethodName(), false);
    final List<String> uuids = new ArrayList<>();
    for (JPPFJob job: jobs) uuids.add(job.getUuid());
    testJobSelectorAction(new JobSelectorAction.NodeJobInformationAction(jobs, new JobUuidSelector(uuids)));
  }

  /**
   * Test 2 jobs and check their node dispatches.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGetNodeInfoWithScriptedJobSelector() throws Exception {
    final String prefix = ReflectionUtils.getCurrentMethodName();
    final List<JPPFJob> jobs = createMultipleJobs(2, 4, 5000L, prefix, false);
    final JobSelector selector = new ScriptedJobSelector("javascript", "jppfJob.getName().startsWith('" + prefix + "')");
    testJobSelectorAction(new JobSelectorAction.NodeJobInformationAction(jobs, selector));
  }

  /**
   * Test 2 jobs and check their node dispatches.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testUpdatePriorityAndMaxNodesWithAllJobsSelector() throws Exception {
    final List<JPPFJob> jobs = createMultipleJobs(2, 4, 1L, ReflectionUtils.getCurrentMethodName(), true);
    testJobSelectorAction(new JobSelectorAction.UpdatePriorityAndMaxNodesAction(jobs, new AllJobsSelector()));
  }

  /**
   * Test 2 jobs and check their node dispatches.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testUpdatePriorityAndMaxNodesWithJobUuidSelector() throws Exception {
    final List<JPPFJob> jobs = createMultipleJobs(2, 4, 1L, ReflectionUtils.getCurrentMethodName(), true);
    final List<String> uuids = new ArrayList<>();
    for (final JPPFJob job: jobs) uuids.add(job.getUuid());
    testJobSelectorAction(new JobSelectorAction.UpdatePriorityAndMaxNodesAction(jobs, new JobUuidSelector(uuids)));
  }

  /**
   * Test 2 jobs and check their node dispatches.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testUpdatePriorityAndMaxNodesWithScriptedJobSelector() throws Exception {
    final String prefix = ReflectionUtils.getCurrentMethodName();
    final List<JPPFJob> jobs = createMultipleJobs(2, 4, 1L, prefix, true);
    final JobSelector selector = new ScriptedJobSelector("javascript", "jppfJob.getName().startsWith('" + prefix + "')");
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
  private static List<JPPFJob> createMultipleJobs(final int nbJobs, final int nbTasks, final long taskDuration, final String namePrefix, final boolean suspended) throws Exception {
    final List<JPPFJob> jobs = new ArrayList<>();
    for (int i=1; i<= nbJobs; i++) {
      final JPPFJob job = BaseTestHelper.createJob(namePrefix + '-' + i, false, false, nbTasks, LifeCycleTask.class, taskDuration);
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
  private static void testJobSelectorAction(final JobSelectorAction action) throws Exception {
    final List<JPPFJob> jobs = action.getJobs();
    final int n = jobs.size();
    print(false, false, "waiting for connection pool");
    final JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
    print(false, false, "setting pool size to %d", n);
    pool.setSize(jobs.size());
    print(false, false, "waiting for %d connections in the pool", n);
    pool.awaitWorkingConnections(Operator.EQUAL, n);
    print(false, false, "submitting %d jobs", n);
    for (JPPFJob job: jobs) client.submitJob(job);
    print(false, false, "checking jobs");
    action.call();
    print(false, false, "waiting for %d active connections", n);
    pool.awaitActiveConnections(Operator.EQUAL, n);
    log.info("setting pool size to 1");
    pool.setSize(1);
    print(false, false, "waiting for 1 connection in the pool");
    pool.awaitWorkingConnections(Operator.EQUAL, 1);
    print(false, false, "got 1 connection in the pool");
  }

  /**
   * We test a job with 1 task, and attempt to cancel it after it has completed.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testCancelJobAfterCompletion() throws Exception {
    final JPPFJob job = BaseTestHelper.createJob(getCurrentMethodName(), true, false, 1, LifeCycleTask.class, TIME_SHORT);
    final List<Task<?>> results = client.submitJob(job);
    assertEquals(1, results.size());
    assertNotNull(results.get(0));
    assertNotNull(results.get(0).getResult());
    final DriverJobManagementMBean proxy = BaseSetup.getJobManagementProxy(client);
    assertNotNull(proxy);
    proxy.cancelJob(job.getUuid());
  }

  /**
   * We test that no job remains in the server's queue after resuming, then cancelling an initially suspended job.
   * See <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-126">JPPF-126 Job cancelled from the admin console may get stuck in the server queue</a>.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testResumeAndCancelSuspendedJob() throws Exception {
    final int nbTasks = 2;
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection();
    assertNotNull(driver);
    final DriverJobManagementMBean jobManager = driver.getJobManager();
    assertNotNull(jobManager);
    final JPPFJob job = BaseTestHelper.createJob(getCurrentMethodName(), false, false, nbTasks, LifeCycleTask.class, 3500L);
    job.getSLA().setSuspended(true);
    client.submitJob(job);
    Thread.sleep(1000L);
    jobManager.resumeJob(job.getUuid());
    Thread.sleep(1000L);
    jobManager.cancelJob(job.getUuid());
    final List<Task<?>> results = job.awaitResults();
    assertEquals(nbTasks, results.size());
  }

  /**
   * Test that a dynamic update of the job SLA is taken into account.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testUpdateJobSLAAndMetadata() throws Exception {
    final int nbTasks = 2;
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection();
    assertNotNull(driver);
    final LoadBalancingInformation lbInfo = driver.loadBalancerInformation();
    assertNotNull(lbInfo);
    try {
      driver.changeLoadBalancerSettings("manual", new TypedProperties().setInt("size", 1));
      final JPPFJob job = BaseTestHelper.createJob(getCurrentMethodName(), false, false, nbTasks, LifeCycleTask.class, 2000L);
      final JobSLA sla = job.getSLA();
      final JobMetadata metadata = job.getMetadata();
      sla.setMaxNodes(1);
      sla.setExecutionPolicy(new Equal("jppf.node.uuid", false, "n1"));
      metadata.setParameter("node.uuid", "n1");
      final DriverJobManagementMBean jobManager = driver.getJobManager();
      assertNotNull(jobManager);
      final MyNotifListener listener = new MyNotifListener(job);
      jobManager.addNotificationListener(listener, null, null);
      client.submitJob(job);
      listener.await();
      jobManager.removeNotificationListener(listener);
      final List<Task<?>> results = job.awaitResults();
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      final List<String> nodeUuids = new ArrayList<>();
      for (int i=0; i<nbTasks; i++) {
        final Task<?> t = results.get(i);
        assertTrue(t instanceof LifeCycleTask);
        final LifeCycleTask task = (LifeCycleTask) t;
        assertNotNull(task.getResult());
        assertNull(task.getThrowable());
        nodeUuids.add(task.getNodeUuid());
        assertEquals("n" + (i+1), task.getNodeUuid());
      }
      assertEquals(nbTasks, nodeUuids.size());
    } finally {
      driver.changeLoadBalancerSettings(lbInfo.getAlgorithm(), lbInfo.getParameters());
    }
  }

  /** */
  public static class MyNotifListener implements NotificationListener {
    /** */
    public int count;
    /** */
    private final JPPFJob job;

    /**
     * Intiialize with a job.
     * @param job the job to use.
     */
    public MyNotifListener(final JPPFJob job) {
      this.job = job;
    }
    
    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      final JobNotification notif = (JobNotification) notification;
      if (notif.getEventType() == JobEventType.JOB_DISPATCHED) {
        synchronized(this) {
          count++;
          if (count == 2) {
            notifyAll();
          } else if (count == 1) {
            job.getSLA().setExecutionPolicy(new Equal("jppf.node.uuid", false, "n2"));
            job.getMetadata().setParameter("node.uuid", "n2");
            try {
              BaseSetup.getJobManagementProxy(client).updateJobs(JobSelector.ALL_JOBS, job.getSLA(), job.getMetadata());
            } catch (final Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

    /** */
    public void await() {
      synchronized(this) {
        try {
          while (count < 2) wait(10L);
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
}
