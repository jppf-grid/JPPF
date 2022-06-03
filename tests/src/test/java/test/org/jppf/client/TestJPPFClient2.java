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

import java.util.*;

import org.jppf.client.*;
import org.jppf.job.JobSelector;
import org.jppf.node.policy.*;
import org.jppf.utils.*;
import org.junit.*;

import test.org.jppf.test.setup.BaseTest;
import test.org.jppf.test.setup.common.*;

/**
 * 
 * @author Laurent Cohen
 */
public class TestJPPFClient2 extends BaseTest {
  /**
   * Used in jobs client SLA to prevent jobs from being executed.
   */
  private static final ExecutionPolicy NOT_LOCAL_POLICY = new Equal("jppf.channel.local", false);
  /** */
  private static final JobSelector CUSTOM_JOB_SELECTOR = job -> ((Integer) job.getMetadata().getParameter("test")) % 2 == 1;

  /**
   * @throws Exception if any error occurs.
   */
  @BeforeClass
  public static void classSetup() throws Exception {
    final TypedProperties config = JPPFConfiguration.set(LOCAL_EXECUTION_ENABLED, true).set(REMOTE_EXECUTION_ENABLED, false);
    client = new JPPFClient(config);
  }

  /**
   * @throws Exception if any error occurs.
   */
  @AfterClass
  public static void classCleanup() throws Exception {
    client.close();
  }

  /**
   * Test that the last load-balancing settings can be retrieved.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testAllQueuedJobs() throws Exception {
    final int nbJobs = 6;
    final List<JPPFJob> jobs = createJobs(nbJobs, ReflectionUtils.getCurrentMethodName());
    for (final JPPFJob job: jobs) client.submitAsync(job);
    for (int i=0; i<nbJobs; i++) {
      final List<JPPFJob> Queued = client.getQueuedJobs();
      assertNotNull(Queued);
      assertEquals(nbJobs - i, Queued.size());
      for (int j=i; j < nbJobs; j++) assertTrue(String.format("i = %d, j = %d", i, j), Queued.contains(jobs.get(j)));
      final JPPFJob job = jobs.get(i);
      job.cancel();
      job.awaitResults();
    }
    final List<JPPFJob> Queued = client.getQueuedJobs();
    assertNotNull(Queued);
    assertEquals(0, Queued.size());
  }

  /**
   * Test that the last load-balancing settings can be retrieved.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testQueuedJobsWithSelector() throws Exception {
    final int nbJobs = 6;
    final List<JPPFJob> jobs = createJobs(nbJobs, ReflectionUtils.getCurrentMethodName());
    for (final JPPFJob job: jobs) client.submitAsync(job);
    for (int i=0; i<nbJobs; i++) {
      final List<JPPFJob> Queued = client.getQueuedJobs(CUSTOM_JOB_SELECTOR);
      assertNotNull(Queued);
      assertEquals(String.format("i = %d", i), (i % 2) + (nbJobs - i) / 2, Queued.size());
      for (final JPPFJob job: Queued) assertEquals(String.format("job %s", job.getName()), 1, ((Integer) job.getMetadata().getParameter("test")) % 2);
      final JPPFJob job = jobs.get(i);
      job.cancel();
      job.awaitResults();
    }
    final List<JPPFJob> Queued = client.getQueuedJobs();
    assertNotNull(Queued);
    assertEquals(0, Queued.size());
  }

  /**
   * Test that the last load-balancing settings can be retrieved.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testAllQueuedJobsCount() throws Exception {
    final int nbJobs = 6;
    final List<JPPFJob> jobs = createJobs(nbJobs, ReflectionUtils.getCurrentMethodName());
    for (final JPPFJob job: jobs) client.submitAsync(job);
    for (int i=0; i<nbJobs; i++) {
      final int n = client.getQueuedJobsCount();
      assertEquals(nbJobs - i, n);
      final JPPFJob job = jobs.get(i);
      job.cancel();
      job.awaitResults();
    }
    assertEquals(0, client.getQueuedJobsCount());
  }

  /**
   * Test that the last load-balancing settings can be retrieved.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testQueuedJobsCountWithSelector() throws Exception {
    final int nbJobs = 6;
    final List<JPPFJob> jobs = createJobs(nbJobs, ReflectionUtils.getCurrentMethodName());
    for (final JPPFJob job: jobs) client.submitAsync(job);
    for (int i=0; i<nbJobs; i++) {
      final int n = client.getQueuedJobsCount(CUSTOM_JOB_SELECTOR);
      assertEquals(String.format("i = %d", i), (i % 2) + (nbJobs - i) / 2, n);
      final JPPFJob job = jobs.get(i);
      job.cancel();
      job.awaitResults();
    }
    assertEquals(0, client.getQueuedJobsCount());
  }

  /**
   * Create the specified number of jobs.
   * @param nbJobs the number of jobs to create.
   * @param namePrefix the prefix of the job names.
   * @return a list of {@link JPPFJob} instances.
   * @throws Exception if any error occurs.
   */
  private static List<JPPFJob> createJobs(final int nbJobs, final String namePrefix) throws Exception {
    final List<JPPFJob> jobs = new ArrayList<>();
    for (int i=0; i< nbJobs; i++) {
      final JPPFJob job = BaseTestHelper.createJob(namePrefix + '-' + i, false, 1, LifeCycleTask.class, 0L);
      job.getMetadata().setParameter("test", i);
      job.getClientSLA().setExecutionPolicy(NOT_LOCAL_POLICY);
      jobs.add(job);
    }
    return jobs;
  }
}
