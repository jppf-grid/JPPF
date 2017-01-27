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

package org.jppf.example.adaptivegrid;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;

/**
 * This isi the main class for the Adaptive Grid demo.
 * @author Laurent Cohen
 */
public class AdaptiveGridDemo implements Runnable {
  /**
   * The name of the client connection pool used in this demo.
   */
  private static final String POOL_NAME = "driver1";
  /**
   * The name of the property which defines the number of job batches to submit and their sizes.
   */
  private static final String JOB_BATCHES_PROPERTY = "jobBatches";
  /**
   * A sequence number generator for the submitted jobs.
   */
  private final AtomicInteger jobSequence = new AtomicInteger(0);
  /**
   * Manages the server connection pool and provides an API for provisioning slave nodes.
   */
  private DriverConnectionManager manager;

  /**
   * Entry point for the Adaptive Grid demo.
   * @param args not used.
   */
  public static void main(final String[] args) {
    new AdaptiveGridDemo().run();
  }

  /**
   * Run the demo.
   */
  @Override
  public void run() {
    // the configuration must be done before the client is initialized
    configure();
    try (JPPFClient client = new JPPFClient()) {
      int maxAllowedNodes = JPPFConfiguration.getProperties().getInt("maxAllowedNodes", 1);
      int maxAllowedPoolSize = JPPFConfiguration.getProperties().getInt("maxAllowedPoolSize", 1);
      print("Starting the demo with maxAllowedNodes=%d and maxAllowedPoolSize=%d", maxAllowedNodes, maxAllowedPoolSize);
      manager = new DriverConnectionManager(client, maxAllowedNodes, maxAllowedPoolSize);
      // parse the number of job batches to submit and their respective sizes from the configuration
      Integer[] batches = parseJobBatches();
      for (int i=0; i<batches.length; i++) {
        // wait a bit before the next batch
        if (i > 0) Thread.sleep(2000L);
        // submit the jobs for this batch and see what the queue listener displays
        runJobs(client, i+1, batches[i]);
      }
      // in the end, reset the number of connections to 1 and stop all slave nodes
      print("demo has completed, resetting to initial grid configuration");
      manager.updateGridSetup(0);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Programmatically configure the JPPF client to ensure the created connection pool has the desired name.
   */
  private void configure() {
    // disable auto-discovery
    JPPFConfiguration.set(JPPFProperties.DISCOVERY_ENABLED, false)
    // set the pool name
    .set(JPPFProperties.DRIVERS, new String[] {POOL_NAME})
    // set the server address
    .setString(POOL_NAME + ".jppf.server.host", "localhost")
    // set the server port
    .setInt(POOL_NAME + ".jppf.server.port", 11111)
    // set the core pool size
    .setInt(POOL_NAME + ".jppf.pool.size", 1);
  }

  /**
   * Submit the specified number of jobs and wait for their results.
   * @param client the JPPF client that submits the jobs and gets their results.
   * @param batchNumber the sequence number of the batch of jobs to run.
   * @param batchSize the number of jobs to run.
   * @throws Exception if any error occurs.
   */
  private void runJobs(final JPPFClient client, final int batchNumber, final int batchSize) throws Exception {
    print("**** submitting jobs batch #%d (%d jobs) *****", batchNumber, batchSize);
    List<JPPFJob> jobs = new ArrayList<>(batchSize);
    long duration = JPPFConfiguration.getProperties().getLong("taskDuration", 1000L);
    // create the jobs
    for (int i=1; i<=batchSize; i++) {
      // give the job a meaningful and human-readable name
      String jobName = String.format("Job %d - batch %d (%d/%d)", jobSequence.incrementAndGet(), batchNumber, i, batchSize);
      try {
        JPPFJob job = new JPPFJob();
        job.setName(jobName);
        job.setBlocking(false);
        // add a single task
        job.add(new SimpleTask(duration));
        jobs.add(job);
      } catch(JPPFException e) {
        print("could not create job '%s' due to %s", jobName, ExceptionUtils.getMessage(e));
      }
    }

    // update the number of connections to the server and the number
    // of slave nodes based on the number of jobs in this batch
    manager.updateGridSetup(batchSize);

    // submit the jobs for execution
    for (JPPFJob job: jobs) client.submitJob(job);
    // get the results of the jobs
    for (JPPFJob job: jobs) {
      List<Task<?>> results = job.awaitResults();
      SimpleTask task = (SimpleTask) results.get(0);
      Throwable t = task.getThrowable();
      if (t != null) print("the job '%' has an error: %s", job.getName(), ExceptionUtils.getMessage(t));
      else print("job '%s' result: %s", job.getName(), task.getResult());
    }
  }

  /**
   * Parse the value of the "jobBatches" configuration property
   * and convert it into an array of integers.
   * @return an array of Integer values.
   */
  private Integer[] parseJobBatches()  {
    String s = JPPFConfiguration.getProperties().getString(JOB_BATCHES_PROPERTY, "1");
    // assume a list of space-separated values
    String[] tokens = s.split("\\s");
    List<Integer> result = new ArrayList<>(tokens.length);
    int position = 0;
    String errorMessage = "the property '" + JOB_BATCHES_PROPERTY + "' has an invalid value '%s' at position %d, it will be ignored";
    for (String token: tokens) {
      try {
        position++;
        int n = Integer.valueOf(token);
        if (n <= 0) System.out.printf(errorMessage, token, position);
        else result.add(n);
      } catch (@SuppressWarnings("unused") NumberFormatException e) {
        print(errorMessage, token, position);
      }
    }
    return result.toArray(new Integer[result.size()]);
  }

  /**
   * Print a string formatted with the specified format and parameters.
   * @param format the format to use.
   * @param params the parmaters, if any.
   */
  public static void print(final String format, final Object...params) {
    System.out.printf("[demo] " + format + "%n", params);
  }
}
