/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

/**
 *
 * @author Laurent Cohen
 */
public class JobStreamImpl extends AbstractJPPFJobStream {
  /**
   * 
   */
  private final RunOptions options;

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
    return getJobCount() < options.nbJobs;
  }

  @Override
  protected JPPFJob createNextJob() {
    JPPFJob job = new JPPFJob();
    job.setName("streaming job " + getJobCount());
    try {
      for (int i=1; i<=options.tasksPerJob; i++) {
        String message = "this is task " + i;
        MyTask task = new MyTask(message, options.taskDuration, options.useCPU, options.dataSize);
        job.add(task).setId(String.format("%s - task %d", job.getName(), i));
      }
    } catch(Exception e) {
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
}
