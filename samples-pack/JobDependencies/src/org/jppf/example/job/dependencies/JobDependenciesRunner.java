/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.example.job.dependencies;

import java.util.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;

/**
 * Run the dependencies managment sample.
 * @author Laurent Cohen
 */
public class JobDependenciesRunner {
  /**
   * Job listener registered with each job to display a message when it completes.
   */
  private static final JobListener JOB_LISTENER = new JobListenerAdapter() {
    @Override
    public synchronized void jobEnded(final JobEvent event) {
      Utils.print("job_listener: '%s' has ended", event.getJob().getName());
    }
  };

  /**
   * Entry point for the Job Dependencies demo.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try (JPPFClient client = new JPPFClient()) {
      // read the jobs and their dependencies from the "./dependency_graph.txt" file
      List<DependencySpec> dependencies = Utils.readDependencies();

      // ensure all jobs can be submitted concurrently by adjusting the connection pool size
      int n = Math.max(dependencies.size(), 1);
      JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
      pool.setSize(n);
      // wait until all connections are initialized
      pool.awaitWorkingConnections(Operator.AT_LEAST, n);

      // Create the jobs according to the dependency graph
      List<JPPFJob> jobs = new ArrayList<>();
      for (DependencySpec spec: dependencies) {
        jobs.add(createJob(spec));
      }

      // submit all the jobs asynchronously
      for (JPPFJob job: jobs) client.submitJob(job);
      // await the jobs results and print them
      for (JPPFJob job: jobs) printJobResults(job);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Create a job with the specified dependencies.
   * @param spec the dependencies specification for the job.
   * @return the newly created job.
   * @throws Exception if any error occurs.
   */
  private static JPPFJob createJob(final DependencySpec spec) throws Exception {
    JPPFJob job = new JPPFJob();
    job.setName(spec.getId());
    // add the dependencies information to the job metadata
    job.getMetadata().setParameter(DependencySpec.DEPENDENCIES_METADATA_KEY, spec);
    // asynchronous job execution
    job.setBlocking(false);
    // the job MUST be suspended before submission
    job.getSLA().setSuspended(true);
    // add a single task and give it a readable id
    job.add(new MyTask()).setId(spec.getId() + "-task");
    // the job listener is not required, we just want to print a message when a job completes on the client side
    job.addJobListener(JOB_LISTENER);
    return job;
  }

  /**
   * Print the results of a job, task by task.
   * @param job the job whose results to print out.
   */
  private static void printJobResults(final JPPFJob job) {
    Utils.print("runner: ***** awaiting results for '%s' *****", job.getName());
    List<Task<?>> results = job.awaitResults();
    for (Task<?> task : results) {
      if (task.getThrowable() != null) Utils.print("runner:   got exception: %s", ExceptionUtils.getStackTrace(task.getThrowable()));
      else Utils.print("runner:   got result: %s", task.getResult());
    }
  }
}
