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

import org.jppf.client.JPPFJob;
import org.jppf.client.utils.AbstractJPPFJobStream;
import org.jppf.utils.JPPFConfiguration;

/**
 *
 * @author Laurent Cohen
 */
public class JobStreamImpl extends AbstractJPPFJobStream {
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
   * Whether the tasks should simulate CPU usage.
   */
  private final boolean useCPU = JPPFConfiguration.getProperties().getBoolean("deadlock.useCPU", false);

  /**
   * Initialize this job provider.
   * @param concurrencyLimit the maximum number of jobs submitted concurrently.
   * @param nbJobs the number of jobs to submit.
   * @param tasksPerJob the maximum number of tasks in each job.
   * @param taskDuration the duration of each task.
   */
  public JobStreamImpl(final int concurrencyLimit, final int nbJobs, final int tasksPerJob, final long taskDuration) {
    super(concurrencyLimit);
    this.nbJobs = nbJobs;
    this.tasksPerJob = tasksPerJob;
    this.taskDuration = taskDuration;
  }

  @Override
  public boolean hasNext() {
    return getJobCount() < nbJobs;
  }

  @Override
  protected JPPFJob createNextJob() {
    JPPFJob job = new JPPFJob();
    job.setName("streaming job " + getJobCount());
    try {
      for (int i=1; i<=tasksPerJob; i++) {
        String message = "this is task " + i;
        MyTask task = new MyTask(message, taskDuration, useCPU);
        job.add(task).setId(String.format("%s - task %d", job.getName(), i));
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
    return job;
  }

  // implementation of Closeable

  @Override
  public void close() {
    System.out.println("closing job provider");
  }

  @Override
  protected void processResults(final JPPFJob job) {
    DeadlockRunner.processResults(job);
  }
}
