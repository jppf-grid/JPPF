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

package sample.test.deadlock;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.client.JPPFJob;
import org.jppf.client.event.JobEvent;
import org.jppf.client.utils.AbstractJPPFJobStream;
import org.jppf.node.protocol.*;

/**
 *
 * @author Laurent Cohen
 */
public class JobStreamImpl extends AbstractJPPFJobStream {
  /**
   * Format for the name of each job, where the parameter is the job sequence number.
   */
  private static final String JOB_NAME_FORMAT = "streaming job %06d";
  /**
   * Options set in the configuration, describing the number and attributes of each job.
   */
  final RunOptions options;
  /**
   * Whether this job stream is stopped.
   */
  boolean stopped;
  /**
   * Whether streaming has started.
   */
  final AtomicBoolean started = new AtomicBoolean(false);
  /**
   * Streaming start time.
   */
  long start;
  /**
   * An optional data provider set onto each job.
   */
  final DataProvider dp;
  /**
   * 
   */
  final Object completionLock = new Object();

  /**
   * Initialize this job provider.
   * @param options the maximum number of jobs submitted concurrently.
   */
  public JobStreamImpl(final RunOptions options) {
    super(options.concurrencyLimit);
    this.options = options;
    if (options.dataProviderSize >= 0) {
      dp = new MemoryMapDataProvider();
      dp.setParameter("data", new byte[options.dataProviderSize]);
    } else dp = null;
  }

  @Override
  public boolean hasNext() {
    if (started.compareAndSet(false, true)) start = System.nanoTime();
    if (isStopped()) return false;
    if ((options.nbJobs > 0) && (getJobCount() >= options.nbJobs)) return false;
    return ((options.streamDuration <= 0L) || ((System.nanoTime() - start) / 1_000_000L < options.streamDuration));
  }

  @Override
  public JPPFJob next() throws NoSuchElementException {
    if (started.compareAndSet(false, true)) start = System.nanoTime();
    return super.next();
  }

  @Override
  protected JPPFJob createNextJob() {
    final JPPFJob job = new JPPFJob();
    job.setName(String.format(JOB_NAME_FORMAT, getJobCount()));
    if (options.callback != null) options.callback.jobCreated(job);
    try {
      for (int i=1; i<=options.tasksPerJob; i++) {
        final String message = "this is task " + i;
        final MyTask task = new MyTask(message, options.taskOptions);
        job.add(task).setId(String.format("%s - task %d", job.getName(), i));
      }
      if (dp != null) job.setDataProvider(dp);
    } catch(final Exception e) {
      e.printStackTrace();
    }
    return job;
  }

  @Override
  public void close() {
    System.out.println("closing job provider");
    setStopped(true);
  }

  @Override
  protected void processResults(final JPPFJob job) {
    DeadlockRunner.processResults(job);
  }

  /**
   * Check whether this job stream is stopped.
   * @return {@code true} if the job stream has stopped, {@code false} otherwise.
   */
  public synchronized boolean isStopped() {
    return stopped;
  }

  /**
   * Specify whether this job stream is to be stopped.
   * @param stopped {{@code true} if the job stream must be stopped, {@code false} otherwise.
   */
  public synchronized void setStopped(final boolean stopped) {
    this.stopped = stopped;
  }

  @Override
  public void jobEnded(final JobEvent event) {
    super.jobEnded(event);
    if (options.callback != null) options.callback.jobCompleted(event.getJob(), this);
    if ((options.closeClientAfter > 0) && (getExecutedJobCount() >= options.closeClientAfter)) {
      DeadlockRunner.print("terminating after %d jobs", options.closeClientAfter);
      System.exit(0);
    }
    synchronized(completionLock) {
      completionLock.notifyAll();
    }
  }

  /**
   * 
   */
  public void awaitStreamCompletion() {
    try {
      synchronized(completionLock) {
        while (getExecutedJobCount() < options.nbJobs) completionLock.wait();
      }
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
