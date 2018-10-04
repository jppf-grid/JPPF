/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.client.utils;

import java.util.*;
import java.util.concurrent.locks.*;

import org.jppf.client.JPPFJob;
import org.jppf.client.event.*;

/**
 * Instances of this class provide a stream of JPPF jobs.
 * <p>A common usage pattern is as follows:<br>
 * <pre> // concurrency level
 * int concurrency = 4;
 * try (JPPFClient client = new JPPFClient();
 *   AbstractJPPFJobStream jobStream = new MyJobStreamImplementation(concurrency)) {
 *   // set the connection pool size appropriately
 *   client.awaitWorkingConnectionPool().setSize(concurrency);
 *   for (JPPFJob job: jobStream) {
 *     if (job != null)
 *       client.submitJob(job);
 *   }
 * }</pre>
 */
public abstract class AbstractJPPFJobStream extends JobListenerAdapter implements Iterable<JPPFJob>, Iterator<JPPFJob>, AutoCloseable {
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
   * A counter for the total number of submitted jobs.
   */
  private int submittedJobCount = 0;
  /**
   * A counter for the total number of completed jobs.
   */
  private int executedJobCount = 0;
  /**
   * A counter for the total number of submitted tasks.
   */
  private int taskCount = 0;
  /**
   * Synchronize access to this job stream.
   */
  private final Lock lock = new ReentrantLock();
  /**
   * Used to wait for number of concurrent jobs to be less than the concurrentcy limit.
   */
  private final Condition concurrencyLimitCondition = lock.newCondition();

  /**
   * Initialize this job provider.
   * @param concurrencyLimit the maximum number of jobs submitted concurrently.
   */
  public AbstractJPPFJobStream(final int concurrencyLimit) {
    this.concurrencyLimit = concurrencyLimit;
    this.currentNbJobs = 0;
  }

  // implementation of Iterator<JPPFJob>

  /**
   * Determine whether there is at least one more job in the stream.
   * @return {@code true} if there is at least one job in the stream, {@code false} otherwise.
   */
  @Override
  public abstract boolean hasNext();

  /**
   * Get the next job in the stream.
   * @return a newly created {@link JPPFJob} object.
   * @throws NoSuchElementException if this stream has no more job to provide.
   */
  @Override
  public JPPFJob next() throws NoSuchElementException {
    lock.lock();
    try {
      if (!hasNext()) throw new NoSuchElementException();
      while (currentNbJobs >= concurrencyLimit) {
        try {
          concurrencyLimitCondition.await();
        } catch (final InterruptedException e) {
          e.printStackTrace();
        }
      }
      return buildJob();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Configure a job created with {@link #createNextJob()} and update this job stream's state accordingly.
   * @return the created job.
   */
  private JPPFJob buildJob() {
    final JPPFJob job = createNextJob();
    if ((job == null) || job.getJobTasks().isEmpty()) return null;
    submittedJobCount++;
    taskCount += job.getJobTasks().size();
    job.setBlocking(false);
    job.addJobListener(this);
    currentNbJobs++;
    return job;
  }

  /**
   * Create the next job in the stream, along with its tasks. This method must be overriden in subclasses.
   * It does not need to update the internal state of this job stream, however it should manage the underlying
   * resources it uses, such as files or database connections.
   * <p>This method is called each time {@link #next() next()} is invoked, including implicitely in enhanced {@code for} loops.
   * @return the created job.
   */
  protected abstract JPPFJob createNextJob();

  /**
   * This operation is not supported and results in an {@link UnsupportedOperationException} being thrown.
   * @throws UnsupportedOperationException every time this method is called.
   */
  @Override
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("remove() is not supported");
  }

  // implementation of JobListener

  /**
   * This implementation of {@link JobListener#jobEnded(JobEvent)} decreases the counter of running jobs,
   * notifies all threads waiting in {@link #next() next()} and finally processes the results asynchronously.
   * @param event encaspulates the source of the event.
   */
  @Override
  public void jobEnded(final JobEvent event) {
    lock.lock();
    try {
      currentNbJobs--;
      executedJobCount++;
      concurrencyLimitCondition.signalAll();
    } finally {
      lock.unlock();
    }
    // process the results asynchronously
    processResults(event.getJob());
  }

  /**
   * Callback invoked when a job is complete.
   * @param job the job whose results to process.
   */
  protected abstract void processResults(JPPFJob job);

  // implementation of Iterable<JPPFJob>

  @Override
  public Iterator<JPPFJob> iterator() {
    return this;
  }

  // implementation of AutoCloseable

  /**
   * Close this stream and release the underlying resources it uses.
   * @throws Exception if any error occurs.
   */
  @Override
  public abstract void close() throws Exception;

  /**
   * Determine whether any job is still being executed.
   * @return {@code true} if at least one job was submitted and has not yet completed, {@code false} otherwise.
   */
  public boolean hasPendingJob() {
    lock.lock();
    try {
      return currentNbJobs > 0;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the number of submitted jobs.
   * @return the count of submitted jobs.
   */
  public int getJobCount() {
    lock.lock();
    try {
      return submittedJobCount;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the number of completed jobs.
   * @return the count of completed jobs.
   */
  public int getExecutedJobCount() {
    lock.lock();
    try {
      return executedJobCount;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the number of submitted task.
   * @return the count of submitted tasks.
   */
  public int getTaskCount() {
    lock.lock();
    try {
      return taskCount;
    } finally {
      lock.unlock();
    }
  }
}
