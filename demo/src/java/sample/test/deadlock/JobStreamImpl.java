/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import org.jppf.client.JPPFJob;
import org.jppf.client.event.JobEvent;
import org.jppf.client.utils.AbstractJPPFJobStream;

/**
 *
 * @author Laurent Cohen
 */
public class JobStreamImpl extends AbstractJPPFJobStream {
  /**
   *
   */
  final RunOptions options;
  /**
   * Whether this job stream is stopped.
   */
  boolean stopped = false;

  /**
   * Initialize this job provider.
   * @param options the maximum number of jobs submitted concurrently.
   */
  public JobStreamImpl(final RunOptions options) {
    super(options.concurrencyLimit);
    this.options = options;
  }

  @Override
  public boolean hasNext() {
    return !isStopped() && (getJobCount() < options.nbJobs);
  }

  @Override
  protected JPPFJob createNextJob() {
    final JPPFJob job = new JPPFJob();
    job.setName("streaming job " + getJobCount());
    if (options.callback != null) options.callback.jobCreated(job);
    try {
      for (int i=1; i<=options.tasksPerJob; i++) {
        final String message = "this is task " + i;
        final MyTask task = new MyTask(message, options.taskOptions);
        job.add(task).setId(String.format("%s - task %d", job.getName(), i));
      }
    } catch(final Exception e) {
      e.printStackTrace();
    }
    return job;
  }

  @Override
  public void close() {
    System.out.println("closing job provider");
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
      DeadlockRunner.printf("terminating after %d jobs", options.closeClientAfter);
      System.exit(0);
    }
  }
}
