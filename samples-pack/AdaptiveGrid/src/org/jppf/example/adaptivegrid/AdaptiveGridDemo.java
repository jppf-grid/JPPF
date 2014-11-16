/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
import org.jppf.client.event.ClientQueueListener;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;

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
   * The name of the property which defines the number of job batches to sublut and their sizes.
   */
  private static final String JOB_BATCHES_PROPERTY = "jobBatches";
  /**
   * A sequence number generator for the submitted jobs.
   */
  private final AtomicInteger jobSequence = new AtomicInteger(0);

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
      DriverConnectionManager manager = new DriverConnectionManager(client, POOL_NAME);
      int maxAllowedNodes = JPPFConfiguration.getProperties().getInt("maxAllowedNodes", 1);
      int maxAllowedPoolSize = JPPFConfiguration.getProperties().getInt("maxAllowedPoolSize", 1);
      ClientQueueListener queuelistener = new MyQueueListener(manager, maxAllowedNodes, maxAllowedPoolSize);
      client.addClientQueueListener(queuelistener);
      // parse the number of job batches to submit and their respective sizes from the configuration
      Integer[] batches = parseJobBatches();
      for (int i=0; i<batches.length; i++) {
        // wait a bit before the next batch
        if (i > 0) Thread.sleep(2000L);
        // submit the jobs for this batch and see what the queue listener displays
        runJobs(client, i+1, batches[i]);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Programmatically configure the JPPF client to ensure the created connection pool has the desired name.
   */
  private void configure() {
    TypedProperties config = JPPFConfiguration.getProperties();
    // disable auto-discovery
    config.setBoolean("jppf.discovery.enabled", false);
    // set the pool name
    config.setString("jppf.drivers", POOL_NAME);
    // set the server address
    config.setString(POOL_NAME + ".jppf.server.host", "localhost");
    // set the server port
    config.setInt(POOL_NAME + ".jppf.server.port", 11111);
    // set the core pool size
    config.setInt(POOL_NAME + ".jppf.pool.size", 1);
  }

  /**
   * Submit the specified number of jobs and wait for their results.
   * @param client the JPPF client that submits the jobs and gets their results.
   * @param batchNumber the sequence number of the batch of jobs to run.
   * @param batchSize the number of jobs to run.
   * @throws Exception if any error occurs.
   */
  private void runJobs(final JPPFClient client, final int batchNumber, final int batchSize) throws Exception {
    System.out.println("**** submitting jobs batch #" + batchNumber + " *****");
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
        System.err.printf("could not create job '%s' due to %s\n", jobName, ExceptionUtils.getMessage(e));
      }
    }
    // submit the jobs for execution
    for (JPPFJob job: jobs) client.submitJob(job);
    // get the results of the jobs
    for (JPPFJob job: jobs) {
      List<Task<?>> results = job.awaitResults();
      SimpleTask task = (SimpleTask) results.get(0);
      Throwable t = task.getThrowable();
      if (t != null) System.out.printf("the job '%' has an error: %s\n", job.getName(), ExceptionUtils.getMessage(t));
      else System.out.printf("job '%s' result: %s\n", job.getName(), task.getResult());
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
    for (String token: tokens) {
      try {
        position++;
        result.add(Integer.valueOf(token));
      } catch (NumberFormatException e) {
        System.out.printf("the property '%s' has an invalid value '%s' at position %d, it will be ignored\n",
          JOB_BATCHES_PROPERTY, token, position);
      }
    }
    return result.toArray(new Integer[result.size()]);
  }
}
