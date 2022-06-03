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

package org.jppf.example.concurrentjobs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.event.JobEvent;
import org.jppf.client.event.JobListenerAdapter;
import org.jppf.client.utils.AbstractJPPFJobStream;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;

/**
 * An illustration of the patterns for submitting multiple jobs in parallel.
 */
public class ConcurrentJobs {
  /**
   * Entry point for this demo.
   * @param args the first argument is a function number that determines which method to call.
   */
  public static void main(final String[] args) {
    try {
      // parse the function number from the first command line argument
      final int function = Integer.parseInt(args[0]);
      if ((function < 1) || (function > 4)) {
        throw new IllegalArgumentException("function number must be between 1 and 4");
      }
      final ConcurrentJobs runner = new ConcurrentJobs();
      // call the appropriate method based on the function number
      switch(function) {
        case 1:
          runner.multipleThreadsBlockingJobs();
          break;
        case 2:
          runner.singleThreadNonBlockingJobs();
          break;
        case 3:
          runner.asynchronousNonBlockingJobs();
          break;
        case 4:
          runner.jobStreaming();
          break;
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Execute multiple blocking jobs from multiple threads.
   * @throws Exception if any error occurs.
   */
  public void multipleThreadsBlockingJobs() throws Exception {
    final int nbJobs = 4;
    final ExecutorService executor = Executors.newFixedThreadPool(nbJobs);
    try (final JPPFClient jppfClient = new JPPFClient()) {
      final List<Future<JPPFJob>> futures = new ArrayList<>(nbJobs);
      // delegate the job submissions to separate threads
      for (int i=1; i<=nbJobs; i++) {
        final JPPFJob job = createJob("multipleThreadsBlockingJob " + i, 3, 1000L);
        futures.add(executor.submit(new MyCallable(jppfClient, job)));
      }
      for (final Future<JPPFJob> future: futures) {
        try {
          final JPPFJob job = future.get();
          // process the job results
          processResults(job);
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }
    } finally {
      executor.shutdown();
    }
  }

  /**
   * Submits a job and returns it after it completes.
   */
  public static class MyCallable implements Callable<JPPFJob> {
    /**
     * The JPPF client which submits the job.
     */
    private final JPPFClient jppfClient;
    /**
     * The job to submit.
     */
    private final JPPFJob job;

    /**
     * Initialize this callable.
     * @param jppfClient the JPPF client which submits the job.
     * @param job the job to submit.
     */
    public MyCallable(final JPPFClient jppfClient, final JPPFJob job) {
      this.jppfClient = jppfClient;
      this.job = job;
    }

    @Override
    public JPPFJob call() throws Exception {
      // submit the job, blocking until the job completes
      jppfClient.submit(job);
      // return the job once completed
      return job;
    }
  }

  /**
   * Submit multiple non-blocking jobs from a single thread, process the results in sequence.
   * @throws Exception if any error occurs.
   */
  public void singleThreadNonBlockingJobs() throws Exception {
    final int nbJobs = 4;
    try (final JPPFClient jppfClient = new JPPFClient()) {
      final List<JPPFJob> jobs = new ArrayList<>(nbJobs);
      for (int i=0; i<nbJobs; i++) {
        // create the job and its tasks
        final JPPFJob job = createJob("singleThreadNonBlockingJob " + i, 3, 1000L);
        jobs.add(job);
        jppfClient.submitAsync(job);
      }
      for (final JPPFJob job: jobs) {
        job.awaitResults();
        // process the job results
        processResults(job);
      }
    }
  }

  /**
   * Execute multiple non-blocking jobs from a single thread, process the results asynchronously.
   * @throws Exception if any error occurs.
   */
  public void asynchronousNonBlockingJobs() throws Exception {
    final int nbJobs = 4;
    try (final JPPFClient jppfClient = new JPPFClient()) {
      // synchronization helper that tells us when all jobs have completed
      final CountDownLatch countDown = new CountDownLatch(nbJobs);
      for (int i=1; i<=nbJobs; i++) {
        final JPPFJob job = createJob("asynchronousNonBlockingJob " + i, 3, 1000L);
        // results will be processed asynchronously within
        // the job listener's jobEnded() notifications
        job.addJobListener(new JobListenerAdapter() {
          @Override
          public synchronized void jobEnded(final JobEvent event) {
            // ... process the job results ...
            processResults(event.getJob());
            // decrease the jobs count down: when the count reaches 0, countDown.await() will exit immediately
            countDown.countDown();
          }
        });
        // submit the job
        jppfClient.submitAsync(job);
      }
      // wait until all jobs are complete
      // i.e. until the count down reaches 0
      countDown.await();
    }
  }

  /**
   * Execute a stream of non-blocking jobs from a single thread, process the results asynchronously.
   * @throws Exception if any error occurs.
   */
  public void jobStreaming() throws Exception {
    final int concurrencyLimit = 4;
    try (final JPPFClient jppfClient = new JPPFClient();
        AbstractJPPFJobStream jobProvider = new JobProvider(concurrencyLimit)) {
      // build and submit the provided jobs until no more is available
      jobProvider.forEach(jppfClient::submitAsync);
      // wait until no more job is pending or executing before exiting
      jobProvider.awaitEndOfStream();
      System.out.printf("*** executed a total of %,d jobs and %,d tasks%n", jobProvider.getJobCount(), jobProvider.getTaskCount());
    }
  }

  /**
   * Create a job with the specified name, number of tasks and duration for each task.
   * @param jobName the name to assign to the job.
   * @param nbTasks the number of tasks to create and add to the job.
   * @param duration the time in millis that each task will wait, simulating actual processing.
   * @return the created job.
   */
  public static JPPFJob createJob(final String jobName, final int nbTasks, final long duration) {
    final JPPFJob job = new JPPFJob();
    // set the job name
    job.setName(jobName);
    for (int i=1; i<=nbTasks; i++) {
      // create a new task
      final MyTask task = new MyTask("this is task #" + i, duration);
      try {
        // add the task to the job and give it a readable id
        job.add(task).setId(jobName + " - " + "task " + i);
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
    return job;
  }

  /**
   * Process the results of a job.
   * @param job the JPPF job whose results are printed.
   */
  public static void processResults(final JPPFJob job) {
    System.out.printf("*** results for job '%s' ***%n", job.getName());
    final List<Task<?>> results = job.getAllResults();
    for (final Task<?> task: results) {
      if (task.getThrowable() != null) { // if the task execution raised an exception
        System.out.printf("%s raised an exception : %s%n", task.getId(), ExceptionUtils.getMessage(task.getThrowable()));
      } else { // otherwise display the task result
        System.out.printf("result of %s : %s%n", task.getId(), task.getResult());
      }
    }
  }
}
