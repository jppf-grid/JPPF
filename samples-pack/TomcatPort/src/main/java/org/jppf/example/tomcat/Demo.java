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

package org.jppf.example.tomcat;

import java.util.List;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.utils.DateTimeUtils;

/**
 * This class contains the code for the demo web application
 * of the Tomcat port. Essentially it holds a {@link JPPFClient} singleton,
 * along with utility methods to submit a job and gather the results.
 * @author Laurent Cohen
 */
public class Demo {
  /**
   * The singleton {@link JPPFClient} instance.
   */
  private static JPPFClient jppfClient = null;

  /**
   * Get a reference to the JPPF client, lazily initializing it if needed.
   * @return a{@link JPPFClient} instance.
   */
  public static synchronized JPPFClient getClient() {
    if (jppfClient == null) {
      jppfClient = new JPPFClient();
      try {
        Thread.sleep(500L);
      } catch (@SuppressWarnings("unused") final InterruptedException e) {
      }
    }
    return jppfClient;
  }

  /**
   * Execute a job and return the result as a string.
   * @param jobName the name given to the JPPF job.
   * @param nbTasks the number of tasks in the job.
   * @param taskDuration the duration in milliseconds of each task in the job.
   * @return the job result as a string message.
   */
  public String submitJob(final String jobName, final int nbTasks, final long taskDuration) {
    final long start = System.nanoTime();
    JPPFJob job = null;
    final StringBuilder sb = new StringBuilder();
    sb.append("<h2>Results for job ").append(jobName).append("</h2>");
    try {
      job = new JPPFJob();
      job.setName(jobName);
      for (int i = 1; i <= nbTasks; i++) {
        final LongTask task = new LongTask(taskDuration);
        task.setId("" + i);
        job.add(task);
      }
      final List<Task<?>> results = getClient().submit(job);
      for (final Task<?> task: results) {
        sb.append("Task ").append(task.getId()).append(" : ").append(task.getResult()).append("<br/>");
      }
    } catch (final Exception e) {
      sb.append(e.getClass().getName()).append(" : ").append(e.getMessage()).append("<br/>");
      for (final StackTraceElement elt: e.getStackTrace()) {
        sb.append(elt).append("<br/>");
      }
    }
    final long elapsed = DateTimeUtils.elapsedFrom(start);
    sb.append("<p> Total processing time: ").append(elapsed).append(" ms");
    return sb.toString();
  }
}
