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

package org.jppf.example.extendedclassloading.client;

import java.util.List;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.example.extendedclassloading.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This client application maintains a repository of Java libraries that are automatically
 * downloaded by the nodes. Each node also maintains its own local repository.
 * The updates are computed by scanning the folder where the libs are stored, and comparing
 * the scan results with the repository's index file to determine which libraries were added,
 * updated or removed. This information is then communicated to the node via the metadata in
 * a JPPF job.
 * <p>This enables the management of the nodes remote repositories by simply removing files from,
 * or dropping files into the folder where the libraries are located.
 * @author Laurent Cohen
 */
public class MyRunner {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(MyRunner.class);
  /**
   * The JPPF client.
   */
  private static JPPFClient client = null;
  /**
   * Location where the downloaded libraries are stored on the client's file system.
   */
  public static final String CLIENT_LIB_DIR = "dynamicLibs";
  /**
   * A number assigned to each job as part of its name.
   */
  private static int jobCount = 1;

  /**
   * Entry point for the demo.
   * @param args there can be one optional argument specifying a file pattern as a wildcard-based expression,
   * which will be used as a filter to delete the libraries in the nodes' repositories. Additional arguments are ignored.
   */
  public static void main(final String[] args) {
    try {
      client = new JPPFClient();

      // create the classpath specified with the '-c' command-line argument
      ClassPath classpath = ClassPathHelper.createClassPathFromArguments(CLIENT_LIB_DIR, args);
      //classpath = ClassPathHelper.createClassPathFromRootFolder(CLIENT_LIB_DIR);
      if ((classpath != null) && (classpath.size() > 0)) output("found dynamic libraries: " + classpath);
      else output("found no dynamic library");

      // create the jobs
      // setting a non-null classpath on the first job will cause the node to update the current task class loader
      JPPFJob job1 = createJob(classpath, new MyTask1());
      // setting a non-null classpath on the second job will cause the node to create a new task class loader
      JPPFJob job2 = createJob(classpath, new MyTask2());

      // if a file pattern is provided, add a corresponding filter to the metadata of the first job
      RepositoryFilter filter = ClassPathHelper.getFilterFromArguments(args);
      if (filter != null) {
        job1.getMetadata().setParameter(ClassPathHelper.REPOSITORY_DELETE_FILTER, filter);
        output("requesting deletion of files matching " + filter);
      }

      // execute the jobs and process their results
      executeJob(job1);
      executeJob(job2);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
    }
  }

  /**
   * Create a job with the specified classpath and tasks.
   * @param classpath the classpath to set onto the job's metadata
   * @param tasks the tasks to add to the job.
   * @return the newly created job.
   * @throws Exception if any error occurs while creating the job.
   */
  private static JPPFJob createJob(final ClassPath classpath, final Task<?>...tasks) throws Exception {
    JPPFJob job = new JPPFJob();
    job.setName("Extended Class Loading " + jobCount++);

    // update the job metadata to specifiy which libraries are needed for the job
    if ((classpath != null) && (classpath.size() > 0)) {
      job.getMetadata().setParameter(ClassPathHelper.JOB_CLASSPATH, classpath);
    }

    // add the tasks to the job
    int taskNumber = 1;
    String prefix = job.getName() + ":task ";
    for (Task<?> task: tasks) job.add(task).setId(prefix + taskNumber++);
    return job;
  }

  /**
   * Execute the specified job on the grid and process its results.
   * @param job the job to execute
   * @throws Exception if any error occurs while creating the job.
   */
  private static void executeJob(final JPPFJob job) throws Exception {
    // submit the job to the grid
    List<Task<?>> results = client.submitJob(job);

    // process the results
    output("*** results for job '" + job.getName() + "'");
    for (Task<?> task: results) {
      String prefix = "task " + task.getId() + " ";
      if (task.getThrowable() != null) {
        // if an error occurred, show the exception stack trace
        output(prefix + "got exception: " + ExceptionUtils.getStackTrace(task.getThrowable()));
      } else {
        // otherwise show the task result
        output(prefix + "result: " + task.getResult());
      }
    }
  }

  /**
   * Print a message to the console and/or log file.
   * @param message the message to print.
   */
  public static void output(final String message) {
    // comment out this line to remove messages from the console
    System.out.println(message);
    // comment out this line to remove messages from the log file
    log.info(message);
  }
}
