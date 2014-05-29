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
package org.jppf.example.loadbalancer.client;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.jppf.client.*;
import org.jppf.example.loadbalancer.common.MyCustomPolicy;
import org.jppf.node.policy.AtLeast;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.JPPFJobMetadata;

/**
 * This is a fully commented job runner for the Custom Load Balancer sample.
 * @author Laurent Cohen
 */
public class CustomLoadBalancerRunner
{
  /**
   * The JPPF client, handles all communications with the server.
   * It is recommended to only use one JPPF client per JVM, so it
   * should generally be created and used as a singleton.
   */
  private static JPPFClient jppfClient =  null;
  /**
   * Value representing one kilobyte.
   */
  private static int KB = 1024;
  /**
   * Value representing one megabyte.
   */
  private static int MB = 1024 * KB;

  /**
   * The entry point for this application runner to be run from a Java command line.
   * @param args by default, we do not use the command line arguments,
   * however nothing prevents us from using them if need be.
   */
  public static void main(final String...args)
  {
    try
    {
      // create the JPPFClient. This constructor call causes JPPF to read the configuration file
      // and connect with one or multiple JPPF drivers.
      jppfClient = new JPPFClient();

      // create a runner instance.
      CustomLoadBalancerRunner runner = new CustomLoadBalancerRunner();

      // Create a "heavy" job
      // The is the maximum memory footprint of a task in the job
      int taskFootprint = 20 * MB;
      // We want 40 MB available for each heavy task running concurrently.
      // This is not easily doable with a standard execution policy, so we create a custom one.
      // We use double the task footprint because it will take approximately twice the memory footprint
      // when each task is serialized or deserialized in the node (serialized data + the object itself).
      ExecutionPolicy heavyPolicy = new MyCustomPolicy("" + (2*taskFootprint));
      // Tasks in the job will have 20 MB data size, will last at most 5 seconds,
      // and the maximum execution time for a set of tasks will be no more than 60 seconds.
      // With 4 tasks and the node's heap set to 64 MB, the load-balancer will be forced to split the job in 2 at least.
      JPPFJob heavyJob = runner.createJob("Heavy Job", 4, taskFootprint, 5L*1000L, 60L*1000L, heavyPolicy);
      // This job has long-running tasks, so we can submit it and create the second job while it is executing.
      jppfClient.submitJob(heavyJob);

      // Create a "light" job
      // We want at least 2 light tasks executing concurrently in a node, to mitigate the network overhead.
      ExecutionPolicy lightPolicy = new AtLeast("jppf.processing.threads", 2);
      // Tasks in the job will have 10 KB data size, will last at most 80 milliseconds,
      // and the maximum execution time for a set of tasks will be no more than 3 seconds.
      // Here the allowed time will be the limiting factor for the number of tasks that can be sent to a node,
      // so the if the node has 4 threads, the job should be split in 2: one set of 150 tasks, then one set of 50 (approximately).
      // (total time = 200 * 80 / 4)
      JPPFJob lightJob = runner.createJob("Light Job", 200, 10*KB, 80L, 3L*1000L, lightPolicy);
      // Submit the light job.
      jppfClient.submitJob(lightJob);

      // now we obtain and process the results - this will cause a lot of logging to the console!
      runner.processJobResults(heavyJob);
      runner.processJobResults(lightJob);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Create a JPPF job that can be submitted for execution.
   * @param jobName the name (or id) assigned to the job.
   * @param nbTasks the number of tasks to add to the job.
   * @param size the data size of each task, in bytes.
   * @param duration the duration of each tasks, in milliseconds.
   * @param allowedTime the maximum execution time for a set of tasks on a node.
   * @param policy execution policy assigned to the job.
   * @return an instance of the {@link JPPFJob} class.
   * @throws Exception if an error occurs while creating the job or adding tasks.
   */
  public JPPFJob createJob(final String jobName, final int nbTasks, final int size, final long duration,
      final long allowedTime, final ExecutionPolicy policy) throws Exception
  {
    // Create a JPPF job.
    JPPFJob job = new JPPFJob();

    // Give this job a readable unique id that we can use to monitor and manage it.
    job.setName(jobName);

    // Specify the job metadata.
    JPPFJobMetadata metadata = (JPPFJobMetadata) job.getMetadata();
    metadata.setParameter("task.memory", "" + size);
    metadata.setParameter("task.time", "" + duration);
    metadata.setParameter("allowed.time", "" + allowedTime);
    metadata.setParameter("id", jobName);

    // Add the tasks to the job.
    for (int i=1; i<=nbTasks; i++)
    {
      // create a task with the specified data size and duration
      Task<Object> task = new CustomLoadBalancerTask(size, duration);
      // task id contains the job name for easier identification
      task.setId(jobName + " - task " + i);
      job.add(task);
    }

    // Assign an execution policy to the job.
    job.getSLA().setExecutionPolicy(policy);

    // Set the job in non-blocking (asynchronous) mode.
    job.setBlocking(false);

    return job;
  }

  /**
   * Collect and process the execution results of a job.
   * @param job the JPPF job to process.
   * @throws Exception if an error occurs while processing the results.
   */
  public void processJobResults(final JPPFJob job) throws Exception
  {
    // Print a banner to visually separate the results for each job.
    System.out.println("\n********** Results for job : " + job.getName() + " **********\n");

    // We are now ready to get the results of the job execution.
    // We use JPPFJob.awaitResults() for this. This method returns immediately with the
    // results if the job has completed, otherwise it waits until the job execution is complete.
    List<Task<?>> results = job.awaitResults();

    // Process the results
    for (Task<?> task: results)
    {
      // If the task execution resulted in an exception, display the stack trace.
      if (task.getThrowable() != null)
      {
        // process the exception here ...
        StringWriter sw = new StringWriter();
        task.getThrowable().printStackTrace(new PrintWriter(sw));
        System.out.println("Exception occurred while executing task " + task.getId() + ":");
        System.out.println(sw.toString());
      }
      else
      {
        // Display the task result.
        System.out.println("Result for task " + task.getId() + " : " + task.getResult());
      }
    }
  }
}
