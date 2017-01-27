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

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.Callable;

import org.jppf.client.JPPFJob;
import org.jppf.job.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.protocol.Task;
import org.jppf.server.job.management.*;
import org.slf4j.*;

import test.org.jppf.test.setup.BaseSetup;
import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * @author Laurent Cohen
 */
abstract class JobSelectorAction implements Callable<Void> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JobSelectorAction.class);
  /**
   * A "short" duration for this test.
   */
  private static final long SLEEP_TIME = 750L;
  /** */
  final JobSelector selector;
  /** */
  final List<JPPFJob> jobs;
  /** */
  DriverJobManagementMBean jobManager;

  /**
   * @param jobs .
   * @param selector .
   */
  JobSelectorAction(final List<JPPFJob> jobs, final JobSelector selector) {
    this.selector = selector;
    this.jobs = jobs;
  }

  /**
   * Get the selector.
   * @return a {@link JobSelector} instance.
   */
  public JobSelector getSelector() {
    return selector;
  }

  @Override
  public Void call() throws Exception {
    Thread.sleep(SLEEP_TIME);
    jobManager = BaseSetup.getJobManagementProxy(BaseSetup.getClient());
    assertNotNull(jobManager);
    performCall();
    return null;
  }

  /**
   * @throws Exception .
   */
  abstract void performCall() throws Exception;

  /**
   * Get the jobs.
   * @return a list of {@link JPPFJob} instances.
   */
  public List<JPPFJob> getJobs() {
    return jobs;
  }

  /**
   * @param shouldBeNull .
   * @throws Exception .
   */
  void checkResults(final boolean shouldBeNull) throws Exception {
    for (JPPFJob job: jobs) {
      int nbTasks = job.getJobTasks().size();
      List<Task<?>> results = job.awaitResults();
      log.info("got results for '" + job.getName() + "'");
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      for (Task<?> t : results) {
        if (shouldBeNull) assertNull(t.getResult());
        else assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, t.getResult());
      }
    }
  }

  /** */
  static class CancelAction extends JobSelectorAction {
    /**
     * @param jobs .
     * @param selector .
     */
    CancelAction(final List<JPPFJob> jobs, final JobSelector selector) {
      super(jobs, selector);
    }

    @Override
    public void performCall() throws Exception {
      jobManager.cancelJobs(selector);
      checkResults(true);
    }
  }

  /** */
  static class ResumeAction extends JobSelectorAction {
    /**
     * @param jobs .
     * @param selector .
     */
    ResumeAction(final List<JPPFJob> jobs, final JobSelector selector) {
      super(jobs, selector);
    }

    @Override
    public void performCall() throws Exception {
      jobManager.resumeJobs(selector);
      checkResults(false);
    }
  }

  /** */
  static class SuspendAction extends JobSelectorAction {
    /**
     * @param jobs .
     * @param selector .
     */
    SuspendAction(final List<JPPFJob> jobs, final JobSelector selector) {
      super(jobs, selector);
    }

    @Override
    public void performCall() throws Exception {
      jobManager.suspendJobs(selector, true);
      Thread.sleep(SLEEP_TIME);
      JobInformation[] jobInfos = jobManager.getJobInformation(selector);
      assertNotNull(jobInfos);
      assertEquals(jobs.size(), jobInfos.length);
      Set<String> jobUuids = new HashSet<>();
      for (JPPFJob job: jobs) jobUuids.add(job.getUuid());
      for (JobInformation info: jobInfos) {
        assertTrue(info.isSuspended());
        assertTrue(jobUuids.contains(info.getJobUuid()));
      }
      jobManager.cancelJobs(selector);
      checkResults(true);
    }
  }

  /** */
  static class NodeJobInformationAction extends JobSelectorAction {
    /**
     * @param jobs .
     * @param selector .
     */
    NodeJobInformationAction(final List<JPPFJob> jobs, final JobSelector selector) {
      super(jobs, selector);
    }

    @Override
    public void performCall() throws Exception {
      Map<String, NodeJobInformation[]> infos = jobManager.getNodeInformation(selector);
      assertNotNull(infos);
      assertEquals(jobs.size(), infos.size());
      Map<String, JPPFJob> jobUuids = new HashMap<>();
      for (JPPFJob job: jobs) jobUuids.put(job.getUuid(), job);
      Set<String> nodeUuids = new HashSet<>();
      for (Map.Entry<String, NodeJobInformation[]> entry: infos.entrySet()) {
        assertNotNull(entry.getValue());
        assertEquals(1, entry.getValue().length);
        NodeJobInformation nji = entry.getValue()[0];
        JobInformation jobInfo = nji.getJobInformation();
        assertTrue(jobUuids.containsKey(jobInfo.getJobUuid()));
        JPPFJob job = jobUuids.get(jobInfo.getJobUuid());
        assertEquals(job.getJobTasks().size(), jobInfo.getInitialTaskCount());
        assertEquals(job.getJobTasks().size(), jobInfo.getTaskCount());
        JPPFManagementInfo nodeInfo = nji.getNodeInfo();
        assertFalse(nodeUuids.contains(nodeInfo.getUuid()));
        nodeUuids.add(nodeInfo.getUuid());
      }
      jobManager.cancelJobs(selector);
      checkResults(true);
    }
  }

  /** */
  static class UpdatePriorityAndMaxNodesAction extends JobSelectorAction {
    /**
     * @param jobs .
     * @param selector .
     */
    UpdatePriorityAndMaxNodesAction(final List<JPPFJob> jobs, final JobSelector selector) {
      super(jobs, selector);
    }

    @Override
    public void performCall() throws Exception {
      JobInformation[] jobInfos = jobManager.getJobInformation(selector);
      assertEquals(jobs.size(), jobInfos.length);
      for (JobInformation info: jobInfos) {
        assertEquals(0, info.getPriority());
        assertEquals(Integer.MAX_VALUE, info.getMaxNodes());
      }
      jobManager.updatePriority(selector, 100);
      jobManager.updateMaxNodes(selector, 10);
      Thread.sleep(SLEEP_TIME);
      jobInfos = jobManager.getJobInformation(selector);
      assertEquals(jobs.size(), jobInfos.length);
      for (JobInformation info: jobInfos) {
        assertEquals(100, info.getPriority());
        assertEquals(10, info.getMaxNodes());
      }
      jobManager.cancelJobs(selector);
      checkResults(true);
    }
  }
}
