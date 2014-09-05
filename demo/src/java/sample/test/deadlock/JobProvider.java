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

package sample.test.deadlock;

import static sample.test.deadlock.DeadlockRunner.processResults;

import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.client.event.*;

/**
 * Instances of this class provide a stream of JPPF jobs, based on the data contained in a text file.
 */
public class JobProvider extends JobListenerAdapter implements Iterable<JPPFJob>, Iterator<JPPFJob>, AutoCloseable {
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
  private final int tasksPerJob;
  /**
   * The duration of each task.
   */
  private final long taskDuration;
  /**
   * The number of jobs to submit.
   */
  private final int nbJobs;
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
   * @param nbJobs the number of jobs to submit.
   * @param tasksPerJob the maximum number of tasks in each job.
   * @param taskDuration the duration of each task.
   */
  public JobProvider(final int concurrencyLimit, final int nbJobs, final int tasksPerJob, final long taskDuration) {
    this.concurrencyLimit = concurrencyLimit;
    this.nbJobs = nbJobs;
    this.tasksPerJob = tasksPerJob;
    this.taskDuration = taskDuration;
    this.currentNbJobs = 0;
  }

  // implementation of Iterator<JPPFJob>

  @Override
  public synchronized boolean hasNext() {
    return jobCount < nbJobs;
  }

  @Override
  public synchronized JPPFJob next() {
    if (!hasNext()) throw new NoSuchElementException();
    while (currentNbJobs >= concurrencyLimit) {
      try {
        wait();
      } catch (InterruptedException e) {
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
  private JPPFJob buildJob() {
    JPPFJob job = new JPPFJob();
    jobCount++;
    job.setName("streaming job " + jobCount);
    try {
      for (int i=1; i<=tasksPerJob; i++) {
        String message = "this is task " + i;
        MyTask task = new MyTask(message, taskDuration);
        //TaskWithDates task = new TaskWithDates();
        job.add(task).setId(String.format("%s - task %d", job.getName(), i));
        taskCount++;
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
    job.setBlocking(false);
    job.addJobListener(this);
    currentNbJobs++;
    return job;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("remove() is not supported");
  }

  // implementation of JobListener

  @Override
  public synchronized void jobEnded(final JobEvent event) {
    processResults(event.getJob());
    currentNbJobs--;
    notifyAll();
  }

  // implementation of Iterable<JPPFJob>

  @Override
  public Iterator<JPPFJob> iterator() {
    return this;
  }

  // implementation of Closeable

  @Override
  public void close() {
    System.out.println("closing job provider");
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