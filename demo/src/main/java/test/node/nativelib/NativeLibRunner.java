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
package test.node.nativelib;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;

/**
 * This is a template JPPF application runner.
 * It is fully commented and is designed to be used as a staring point
 * to write an application using JPPF.
 * @author Laurent Cohen
 */
public class NativeLibRunner {
  /**
   * The JPPF client, handles all communications with the server.
   * It is recommended to only use one JPPF client per JVM, so it
   * should generally be created and used as a singleton.
   */
  private static JPPFClient jppfClient = null;

  /**
   * The entry point for this application runner to be run from a Java command line.
   * @param args by default, we do not use the command line arguments,
   * however nothing prevents us from using them if need be.
   */
  public static void main(final String... args) {
    try {
      // create the JPPFClient. This constructor call causes JPPF to read the configuration file
      // and connect with one or multiple JPPF drivers.
      jppfClient = new JPPFClient();

      // create a runner instance.
      final NativeLibRunner runner = new NativeLibRunner();

      for (int i = 0; i < 3; i++) {
        System.out.println("submitting job #" + (i + 1) + " ...");
        // Create a job
        final JPPFJob job = runner.createJob();
        job.setName("" + (i + 1));

        // execute a blocking job
        runner.executeBlockingJob(job);
      }
      // execute a non-blocking job
      //runner.executeNonBlockingJob(job);
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Create a JPPF job that can be submitted for execution.
   * @return an instance of the {@link org.jppf.client.JPPFJob JPPFJob} class.
   * @throws Exception if an error occurs while creating the job or adding tasks.
   */
  public JPPFJob createJob() throws Exception {
    // create a JPPF job
    final JPPFJob job = new JPPFJob();

    // give this job a readable unique id that we can use to monitor and manage it.
    job.setName("Template Job Id");

    // add a task to the job.
    final NativeLibTask task = new NativeLibTask();
    //task.setTimeout(1000);
    job.add(task);

    // add more tasks here ...

    // there is no guarantee on the order of execution of the tasks,
    // however the results are guaranteed to be returned in the same order as the tasks.
    return job;
  }

  /**
   * Execute a job in blocking mode. The application will be blocked until the job
   * execution is complete.
   * @param job the JPPF job to execute.
   * @throws Exception if an error occurs while executing the job.
   */
  public void executeBlockingJob(final JPPFJob job) throws Exception {
    // Submit the job and wait until the results are returned.
    // The results are returned as a list of JPPFTask instances,
    // in the same order as the one in which the tasks where initially added the job.
    final List<Task<?>> results = jppfClient.submit(job);

    // process the results
    for (final Task<?> task : results) {
      // if the task execution resulted in an exception
      if (task.getThrowable() != null) {
        // process the exception here ...
        System.out.println("Caught exception:");
        task.getThrowable().printStackTrace();
      } else {
        // process the result here ...
        System.out.println("Result:" + task.getResult());
      }
    }
  }

  /**
   * Execute a job in non-blocking mode. The application has the responsibility
   * for handling the notification of job completion and collecting the results.
   * @param job the JPPF job to execute.
   * @throws Exception if an error occurs while executing the job.
   */
  public void executeNonBlockingJob(final JPPFJob job) throws Exception {
    // Submit the job. This call returns immediately without waiting for the execution of
    // the job to complete. As a consequence, the object returned for a non-blocking job is
    // always null. Note that we are calling the exact same method as in the blocking case.
    jppfClient.submitAsync(job);

    // do something else here, while the job is being executed ...

    // We are now ready to get the results of the job execution.
    // We use JPPFResultCollector.waitForResults() for this. This method returns immediately
    // with the results if the job has completed, otherwise it waits until the job execution
    // is complete.
    final List<Task<?>> results = job.awaitResults();

    // process the results
    for (Task<?> task : results) {
      // if the task execution resulted in an exception
      if (task.getThrowable() != null) {
        // process the exception here ...
      } else {
        // process the result here ...
      }
    }
  }
}
