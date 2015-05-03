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

package org.jppf.example.concurrentjobs;

import static org.jppf.example.concurrentjobs.ConcurrentJobs.processResults;

import java.io.*;
import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.client.event.*;
import org.jppf.node.protocol.Task;

/**
 * Instances of this class provide a stream of JPPF jobs, based on the data contained in a text file.
 */
public class JobProvider extends JobListenerAdapter implements Iterable<JPPFJob>, Iterator<JPPFJob>, Closeable {
  /**
   * The maximum number of jobs submitted concurrently.
   */
  private final int concurrencyLimit;
  /**
   * The current number of submitted jobs.
   * Always less than or eaqual to {@code concurrencyLimit}.
   */
  private int currentNbJobs;
  /**
   * The maximum number of tasks in each job.
   */
  private final int tasksPerJob = 10;
  /**
   * Used to read the text file line by line.
   */
  private BufferedReader reader;
  /**
   * Indicates whether there is no more line to reead from the file.
   */
  private boolean eof = false;
  /**
   * A counter for the total number of submitted jobs.
   */
  private int jobCount = 0;
  /**
   * A counter for the total number of submitted tasks.
   */
  private int taskCount = 0;

  /**
   * Initialize this job provider.
   * @param concurrencyLimit the maximum number of jobs submitted concurrently.
   * @throws IOException if any error occcurs while open the input text file.
   */
  public JobProvider(final int concurrencyLimit) throws IOException {
    this.concurrencyLimit = concurrencyLimit;
    this.currentNbJobs = 0;
    reader = new BufferedReader(new FileReader("input.txt"));
  }

  // implementation of Iterator<JPPFJob>

  @Override
  public synchronized boolean hasNext() {
    // return true as long as there is a line to read in the file
    return !eof;
  }

  @Override
  public synchronized JPPFJob next() {
    if (!hasNext()) throw new NoSuchElementException();
    // wait until the number of running jobs is less than the concurrency limit
    while (currentNbJobs >= concurrencyLimit) {
      try {
        wait();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return buildJob();
  }

  /**
   * Build a job and its tasks by reading data from a text file.
   * A task is constructed for each line in the file.
   * @return the created job.
   */
  private synchronized JPPFJob buildJob() {
    JPPFJob job = new JPPFJob();
    try {
      for (int i=1; i<=tasksPerJob; i++) {
        String message = reader.readLine();
        if (message == null) {
          eof = true;
          break;
        }
        taskCount++;
        MyTask task = new MyTask(message, 200L);
        // add the task to the job
        job.add(task);
      }
    } catch(Exception e) {
      eof = true;
      e.printStackTrace();
    }
    // happens for the last job when the number of lines in the file is a multiple of tasksPerJob
    if (job.getJobTasks().isEmpty()) {
      eof = true;
      return null;
    }
    jobCount++;
    job.setName("streaming job " + jobCount);
    int i = 0;
    // give each task a readable id
    for (Task<?> task: job) task.setId(String.format("%s - task %d", job.getName(), ++i));
    job.setBlocking(false);
    // add a listener to update the concurrent jobs count when a job ends
    job.addJobListener(this);
    // increase the count of concurrently running jobs
    currentNbJobs++;
    return job;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("remove() is not supported");
  }

  // implementation of JobListener

  @Override
  public void jobEnded(final JobEvent event) {
    // process the job results
    synchronized(JobProvider.this) {
      processResults(event.getJob());
      // decrease the count of concurrently running jobs
      currentNbJobs--;
      // wake up the threads waiting in next()
      notifyAll();
    }
  }

  // implementation of Iterable<JPPFJob>

  @Override
  public Iterator<JPPFJob> iterator() {
    return this;
  }

  // implementation of Closeable

  @Override
  public void close() throws IOException {
    if (reader != null) reader.close();
  }

  /**
   * Determine whether any job is still being executed.
   * @return {@code true} if at least one job was submitted and has not yet completed, {@code false} otherwise.
   */
  public synchronized boolean hasPendingJob() {
    return currentNbJobs > 0;
  }

  /**
   * Get the number of executed jobs.
   * @return the count of executed jobs.
   */
  public synchronized int getJobCount() {
    return jobCount;
  }

  /**
   * Get the number of executed task.
   * @return the count of executed tasks.
   */
  public synchronized int getTaskCount() {
    return taskCount;
  }
}