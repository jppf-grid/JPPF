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

package org.jppf.example.job.dependencies;

import java.util.List;
import java.util.stream.Collectors;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.event.JobEvent;
import org.jppf.client.event.JobListener;
import org.jppf.client.event.JobListenerAdapter;
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
   * @param args {@code args[0]} may contain the path of a dependency graph file to parse,
   * otherwise "./dependency_graph.txt" is used.
   */
  public static void main(final String[] args) {
    try (final JPPFClient client = new JPPFClient()) {
      // read the jobs and their dependencies from the specified or default file
      final String filename = ((args == null) || (args.length < 1)) ? "./dependency_graph.txt" : args[0];
      final List<DependencyDescriptor> dependencies = Utils.readDependencies(filename);

      // Create the jobs according to the dependency graph
      final List<JPPFJob> jobs = dependencies.stream()
        .map(JobDependenciesRunner::createJob)
        .collect(Collectors.toList());

      // submit all the jobs asynchronously
      jobs.forEach(client::submitAsync);

      // await the jobs results and print them
      jobs.forEach(JobDependenciesRunner::printJobResults);

    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Create a job with the specified dependencies.
   * @param desc the dependencies specification for the job.
   * @return the newly created job.
   */
  private static JPPFJob createJob(final DependencyDescriptor desc) {
    try {
      // create the job with a name equal to its dpeendency id
      final JPPFJob job = new JPPFJob().setName(desc.getId());
  
      // retrieve the jkob's dpeendency specification and set its attributes
      job.getSLA().getDependencySpec().setId(desc.getId())
        .addDependencies(desc.getDependencies())
        .setGraphRoot(desc.isGraphRoot());
  
      // add a single task and give it a readable id
      job.add(new MyTask()).setId(desc.getId() + "-task");
  
      // the job listener is not required, we just want to print a message when a job completes on the client side
      job.addJobListener(JOB_LISTENER);
  
      return job;

    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Print the results of a job, task by task.
   * @param job the job whose results to print out.
   */
  private static void printJobResults(final JPPFJob job) {
    Utils.print("runner: ***** awaiting results for '%s' *****", job.getName());
    final List<Task<?>> results = job.awaitResults();
    for (final Task<?> task : results) {
      if (task.getThrowable() != null) Utils.print("runner:   got exception: %s", ExceptionUtils.getStackTrace(task.getThrowable()));
      else Utils.print("runner:   got result: %s", task.getResult());
    }
  }
}
