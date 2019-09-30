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

import java.util.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.server.job.management.JobDependencyManagerMBean;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for the job dependencies and job graph feature.
 * @author Laurent Cohen
 */
public class TestJobDependencies extends BaseTest {
  /** */
  @Rule
  public TestWatcher setup1D1NInstanceWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      if (client != null) BaseTestHelper.printToAll(client, false, false, true, true, true, "starting method %s()", description.getMethodName());
    }
  };

  /**
   * Launches 1 driver with 1 node.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final TestConfiguration cfg = BaseSetup.DEFAULT_CONFIG.copy();
    cfg.driver.log4j = "classes/tests/config/log4j-driver.TestJobDependencies.properties";
    client = BaseSetup.setup(1, 2, true, cfg);
  }

  /**
   * Stops the driver and node.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    BaseSetup.cleanup();
  }

  /**
   * Test the submission of a simple job grap: in effect a linked list.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testSimpleGraph() throws Exception {
    testLayeredGraph(5, 1, 10);
  }

  /**
   * Test the submission of a simple job grap: in effect a linked list.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testDeepGraph() throws Exception {
    testLayeredGraph(100, 1, 10);
  }

  /**
   * Test the submission of a multilayered job graph.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testComplexGraph() throws Exception {
    testLayeredGraph(5, 3, 10);
  }

  /**
   * Test the submission of a multilayered job graph.
   * @param depth number of layers in the graph.
   * @param jobsPerLayer number of jobs in each layer.
   * @param nbTasks number of tasks in each job.
   * @throws Exception if any error occurs.
   */
  private static void testLayeredGraph(final int depth, final int jobsPerLayer, final int nbTasks) throws Exception {
    int oldMaxJobs = 1;
    JPPFConnectionPool pool = null;
    final int layers = depth;
    try {
      pool = client.awaitWorkingConnectionPool();
      oldMaxJobs = pool.getMaxJobs();
      pool.setMaxJobs(Integer.MAX_VALUE);
      final JobEndedListener jobListener = new JobEndedListener();
      final List<JPPFJob> jobs = createLayeredJobs(layers, jobsPerLayer, "job", nbTasks, 1L);
      for (final JPPFJob job: jobs) job.addJobListener(jobListener);
      for (final JPPFJob job: jobs) client.submitAsync(job);
      for (final JPPFJob job: jobs) checkJobResults(job, false);
      final List<JPPFJob> endedJobs = jobListener.jobs;
      assertFalse(endedJobs.isEmpty());
      assertEquals(layers * jobsPerLayer, endedJobs.size());
      assertEquals(layers, jobListener.layersOrder.size());
      for (int i = 0; i < layers; i++) assertEquals(layers - i - 1, jobListener.layersOrder.get(i).intValue());
      assertEquals(layers, jobListener.layerMap.size());
      int count = 0;
      for (final Map.Entry<Integer, List<Integer>> entry: jobListener.layerMap.entrySet()) {
        final int layer = entry.getKey();
        assertEquals(layers - count - 1, layer);
        final List<Integer> layerList = entry.getValue();
        assertNotNull(layerList);
        assertEquals(jobsPerLayer, layerList.size());
        count++;
      }
      checkGraphEmpty();
    } finally {
      if (pool != null) pool.setMaxJobs(oldMaxJobs);
    }
  }

  /**
   * Test the submission of a simple job grap: in effect a linked list.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testCancellation() throws Exception {
    int oldMaxJobs = 1;
    JPPFConnectionPool pool = null;
    final int layers = 5, jobsPerLayer = 1, nbTasks = 1;
    try {
      pool = client.awaitWorkingConnectionPool();
      oldMaxJobs = pool.getMaxJobs();
      pool.setMaxJobs(Integer.MAX_VALUE);
      final JobEndedListener jobListener = new JobEndedListener();
      final List<JPPFJob> jobs = createLayeredJobs(layers, jobsPerLayer, "job", nbTasks, 5000L);
      for (final JPPFJob job: jobs) {
        job.addJobListener(jobListener);
        // last job (leaf in the graph) is set to expire 500 ms after it is queued in the server
        if ((Integer) job.getMetadata().getParameter("layer") == layers - 1) job.getSLA().setJobExpirationSchedule(new JPPFSchedule(500L));
      }
      for (final JPPFJob job: jobs) client.submitAsync(job);
      for (final JPPFJob job: jobs) checkJobResults(job, true);
      final List<JPPFJob> endedJobs = jobListener.jobs;
      assertFalse(endedJobs.isEmpty());
      assertEquals(layers * jobsPerLayer, endedJobs.size());
      assertEquals(layers, jobListener.layersOrder.size());
      assertEquals(layers, jobListener.layerMap.size());
      checkGraphEmpty();
    } finally {
      if (pool != null) pool.setMaxJobs(oldMaxJobs);
    }
  }

  /**
   * Test the submissioon of a job graph with a cycle.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testDependencyCycle() throws Exception {
    int oldMaxJobs = 1;
    JPPFConnectionPool pool = null;
    final int layers = 3, jobsPerLayer = 1, nbTasks = 1;
    try {
      pool = client.awaitWorkingConnectionPool();
      oldMaxJobs = pool.getMaxJobs();
      pool.setMaxJobs(Integer.MAX_VALUE);
      final JobEndedListener jobListener = new JobEndedListener();
      final List<JPPFJob> jobs = createLayeredJobs(layers, jobsPerLayer, "job", nbTasks, 5000L);
      // last job depends on the first one, creating a cycle
      jobs.get(layers - 1).getSLA().getDependencySpec().addDependencies(jobs.get(0).getSLA().getDependencySpec().getId());
      for (final JPPFJob job: jobs) job.addJobListener(jobListener);
      for (final JPPFJob job: jobs) client.submitAsync(job);
      for (final JPPFJob job: jobs) checkJobResults(job, true);
      final List<JPPFJob> endedJobs = jobListener.jobs;
      assertFalse(endedJobs.isEmpty());
      assertEquals(layers * jobsPerLayer, endedJobs.size());
      assertEquals(layers, jobListener.layersOrder.size());
      assertEquals(layers, jobListener.layerMap.size());
      checkGraphEmpty();
    } finally {
      if (pool != null) pool.setMaxJobs(oldMaxJobs);
    }
  }

  /**
   * Test the submission of a simple job graph via 2 separate JPPFClient instances.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testSubmissionFrom2Clients() throws Exception {
    int oldMaxJobs = 1;
    JPPFConnectionPool pool = null;
    final int layers = 4, nbTasks = 10;
    try (final JPPFClient client2 = new JPPFClient()) {
      pool = client.awaitWorkingConnectionPool();
      oldMaxJobs = pool.getMaxJobs();
      pool.setMaxJobs(Integer.MAX_VALUE);
      client2.awaitWorkingConnectionPool().setMaxJobs(Integer.MAX_VALUE);
      final JobEndedListener jobListener = new JobEndedListener();
      final List<JPPFJob> jobs = createLayeredJobs(layers, 1, "job", nbTasks, 1L);
      for (final JPPFJob job: jobs) job.addJobListener(jobListener);
      client.submitAsync(jobs.get(0));
      client2.submitAsync(jobs.get(1));
      client2.submitAsync(jobs.get(2));
      client.submitAsync(jobs.get(3));
      for (final JPPFJob job: jobs) checkJobResults(job, false);
      final List<JPPFJob> endedJobs = jobListener.jobs;
      assertFalse(endedJobs.isEmpty());
      assertEquals(layers, endedJobs.size());
      for (int i = 0; i < layers; i++) {
        final JPPFJob job1 = jobs.get(i), job2 = endedJobs.get(layers - 1 - i);
        assertEquals(job1.getName(), job2.getName());
        assertEquals(job1.getUuid(), job2.getUuid());
      }
      checkGraphEmpty();
    } finally {
      if (pool != null) pool.setMaxJobs(oldMaxJobs);
    }
  }

  /**
   * @param job the job to check.
   * @param expectCancelled whether the job is expected to have been cancelled.
   * @throws Exception if any error occurs.
   */
  private static void checkJobResults(final JPPFJob job, final boolean expectCancelled) throws Exception {
    final List<Task<?>> results = job.awaitResults();
    assertNotNull(results);
    assertEquals(job.getTaskCount(), results.size());
    for (final Task<?> task: results) {
      assertNull(task.getThrowable());
      if (!expectCancelled) assertNotNull(task.getResult());
      else assertNull(task.getResult());
    }
  }

  /**
   * Create a multi-layered job graph where each job in a layer - except for the last layer - depends on all the jobs in the next layer.
   * @param nbLayers number of job layers.
   * @param jobsPerLayer number of jobs in each layer.
   * @param namePrefix job name prefix.
   * @param tasksPerJob number of tasks in each job.
   * @param taskDuration the duration of each task in millis.
   * @return a list of {@link JPPFJob} instances.
   * @throws Exception if any error occurs.
   */
  private static List<JPPFJob> createLayeredJobs(final int nbLayers, final int jobsPerLayer, final String namePrefix, final int tasksPerJob, final long taskDuration) throws Exception {
    final List<JPPFJob> allJobs = new ArrayList<>(nbLayers * jobsPerLayer);
    final List<List<JPPFJob>> layers = new ArrayList<>(nbLayers);
    for (int i = 0; i < nbLayers; i++) {
      final List<JPPFJob> jobs = new ArrayList<>(jobsPerLayer);
      layers.add(jobs);
      for (int j = 0; j < jobsPerLayer; j++) {
        final JPPFJob job = BaseTestHelper.createJob(String.format("%s-%03d-%03d", namePrefix, i, j), false, tasksPerJob, LifeCycleTask.class, taskDuration);
        jobs.add(job);
        final JobDependencySpec spec = job.getSLA().getDependencySpec();
        spec.setId(job.getName()).setCascadeCancellation(true);
        if (i == 0) spec.setRemoveUponCompletion(true);
        job.getMetadata().setParameter("layer", i).setParameter("index", j);
      }
      if (i > 0) {
        for (final JPPFJob previousLayerJob: layers.get(i - 1)) {
          final JobDependencySpec spec = previousLayerJob.getSLA().getDependencySpec();
          for (final JPPFJob job: jobs) spec.addDependencies(job.getSLA().getDependencySpec().getId());
        }
      }
      allJobs.addAll(jobs);
    }
    return allJobs;
  }

  /**
   * Create a simple diamond job graph.
   * @param namePrefix job name prefix.
   * @param tasksPerJob number of tasks in each job.
   * @param taskDuration the duration of each task in millis.
   * @return a list of {@link JPPFJob} instances.
   * @throws Exception if any error occurs.
   */
  static List<JPPFJob> createDiamondGraph(final String namePrefix, final int tasksPerJob, final long taskDuration) throws Exception {
    final List<JPPFJob> jobs = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      final JPPFJob job = BaseTestHelper.createJob(String.format("%s-%03d", namePrefix, i), false, tasksPerJob, LifeCycleTask.class, taskDuration);
      setDependencyId(job);
      jobs.add(job);
    }
    final JobDependencySpec spec = jobs.get(0).getSLA().getDependencySpec();
    spec.setRemoveUponCompletion(true);
    for (int i=1; i<=2; i++) {
      final String name = jobs.get(i).getName();
      spec.addDependencies(name);
      setDependencyId(jobs.get(i)).addDependencies(jobs.get(3).getName());
    }
    return jobs;
  }

  /** */
  public static class JobEndedListener extends JobListenerAdapter {
    /** */
    public final List<JPPFJob> jobs = new ArrayList<>();
    /** */
    public final LinkedHashMap<Integer, List<Integer>> layerMap = new LinkedHashMap<>();
    /** */
    public final List<Integer> layersOrder = new ArrayList<>();

    @Override
    public void jobEnded(final JobEvent event) {
      final JPPFJob job = event.getJob();
      BaseTest.print(false, false, "job ended: %s", job);
      final JobMetadata meta = job.getMetadata();
      synchronized (jobs) {
        jobs.add(job);
        final int layer = meta.getParameter("layer");
        if (!layersOrder.contains(layer)) layersOrder.add(layer);
        List<Integer> layerList = layerMap.get(layer);
        if (layerList == null) {
          layerList = new ArrayList<>();
          layerMap.put(layer, layerList);
        }
        layerList.add(meta.getParameter("index"));
      }
    }
  }

  /**
   * Set the name of a specified job as its dpeendency id. 
   * @param job the job to update.
   * @return the jobs dependency spec.
   */
  private static JobDependencySpec setDependencyId(final JPPFJob job) {
    final JobDependencySpec spec = job.getSLA().getDependencySpec();
    spec.setId(job.getName());
    return spec;
  }

  /**
   * Check that the graph becomes emty after a maximum set time.
   * @throws Exception if any error occurs.
   */
  private static void checkGraphEmpty() throws Exception {
    final JobDependencyManagerMBean dependencyManager = BaseSetup.getJMXConnection().getJobDependencyManager();
    assertTrue("remaining ids: " + dependencyManager.getNodeIds(),
      ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> dependencyManager.getNodeIds().isEmpty(), 250L, 5000L, false));
  }
}
