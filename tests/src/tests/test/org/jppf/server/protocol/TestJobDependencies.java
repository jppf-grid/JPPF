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
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * 
 * @author Laurent Cohen
 */
public class TestJobDependencies extends Setup1D2N1C {
  /**
   * Test the submission of a simple job grap: in effect a linked list.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testSimpleGraph() throws Exception {
    int oldMaxJobs = 1;
    JPPFConnectionPool pool = null;
    final int layers = 5, nbTasks = 10;
    try {
      pool = client.awaitWorkingConnectionPool();
      oldMaxJobs = pool.getMaxJobs();
      pool.setMaxJobs(Integer.MAX_VALUE);
      final JobEndedListener jobListener = new JobEndedListener();
      final List<JPPFJob> jobs = createLayeredJobs(layers, 1, "job", nbTasks, 1L);
      for (int i = 0; i < layers; i++) {
        final JPPFJob job = jobs.get(i);
        final JobDependencySpec spec = job.getSLA().getDependencySpec();
        if (i == 0) assertTrue(spec.isRemoveUponCompletion());
        if (i < layers - 1) {
          assertTrue(spec.hasDependency());
          final List<String> deps = spec.getDependencies();
          assertEquals(1, deps.size());
          assertEquals(String.format("job-%03d-000", i + 1), spec.getDependencies().get(0));
        }
        else assertFalse(spec.hasDependency());
      }
      for (final JPPFJob job: jobs) job.addJobListener(jobListener);
      for (final JPPFJob job: jobs) client.submitAsync(job);
      for (final JPPFJob job: jobs) checkJobResults(job, false);
      final List<JPPFJob> endedJobs = jobListener.jobs;
      assertFalse(endedJobs.isEmpty());
      assertEquals(layers, endedJobs.size());
      for (int i = 0; i < layers; i++) {
        assertEquals(jobs.get(i).getUuid(), endedJobs.get(layers - 1 - i).getUuid());
      }
    } finally {
      if (pool != null) pool.setMaxJobs(oldMaxJobs);
    }
  }

  /**
   * Test the submission of a multilayered job graph.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testComplexGraph() throws Exception {
    int oldMaxJobs = 1;
    JPPFConnectionPool pool = null;
    final int layers = 5, jobsPerLayer = 3, nbTasks = 10;
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
        if ((Integer) job.getMetadata().getParameter("layer") == layers - 1) job.getSLA().setJobExpirationSchedule(new JPPFSchedule(500L));
      }
      for (final JPPFJob job: jobs) client.submitAsync(job);
      for (final JPPFJob job: jobs) checkJobResults(job, true);
      final List<JPPFJob> endedJobs = jobListener.jobs;
      assertFalse(endedJobs.isEmpty());
      assertEquals(layers * jobsPerLayer, endedJobs.size());
      assertEquals(layers, jobListener.layersOrder.size());
      assertEquals(layers, jobListener.layerMap.size());
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
   * 
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
}
