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

package test.client.schedule;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.scheduling.*;

/**
 *
 * @author Laurent Cohen
 */
public class MyJobListener extends JobListenerAdapter implements AutoCloseable {
  /**
   * Key for the expiration schedule in the job metadata.
   */
  public static String EXPIRATION_KEY = "job.expiration";
  /**
   * Schedule handler with a specified thread name prefix.
   */
  private final JPPFScheduleHandler handler = new JPPFScheduleHandler("MyScheduleHandler");
  /**
   * The JPPF client.
   */
  private final JPPFClient client;

  /**
   * Initialize with the specified client
   * @param client the JPPF client.
   */
  public MyJobListener(final JPPFClient client) {
    this.client = client;
  }

  @Override
  public void jobStarted(final JobEvent event) {
    try {
      final JPPFJob job = event.getJob();
      System.out.println("jobStarted() notification for job '" + job.getName() + "'");
      final String uuid = job.getUuid();
      // cancel any previously scheduled action
      handler.cancelAction(uuid);
      final Long timeout = job.getMetadata().getParameter(EXPIRATION_KEY);
      if ((timeout != null) && (timeout > 0L)) {
        System.out.println("rescheduling job '" + job.getName() + "' for " + timeout + " ms");
        // this action cancels the job and is triggered when the timeout expires
        Runnable action = new Runnable() {
          @Override
          public void run() {
            try {
              System.out.println("job '" + job.getName() + "' expired after " + timeout + " ms, cancelling");
              client.cancelJob(uuid);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        };
        long now = System.currentTimeMillis();
        // schedule the action
        handler.scheduleAction(uuid, new JPPFSchedule(timeout), action, now);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void jobEnded(final JobEvent event) {
    System.out.println("jobEnded() notification for job '" + event.getJob().getName() + "'");
    // cancel any previously scheduled action
    handler.cancelAction(event.getJob().getUuid());
  }

  @Override
  public void jobDispatched(final JobEvent event) {
    System.out.println("jobDispatched() notification for job '" + event.getJob().getName() + "'");
  }

  @Override
  public void jobReturned(final JobEvent event) {
    System.out.println("jobReturned() notification for job '" + event.getJob().getName() + "'");
  }

  @Override
  public void close() {
    // shutdown the scheduled executor  so the JVM can terminate cleanly.
    if (handler != null) handler.clear(true);
  }
}
