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

package org.jppf.client.event;

import java.util.EventListener;

/**
 * Listener interface for receiving job started and job ended event notifications.
 * @author Laurent Cohen
 */
public interface JobListener extends EventListener
{
  /**
   * Called when the execution of a job is starting.
   * This method may be called multiple times, in the case where the job is resubmitted,
   * due to a broken connection to the server for instance.
   * @param event encapsulates the event that caused this method to be called.
   */
  void jobStarted(JobEvent event);

  /**
   * Called when the execution of a job is complete.
   * This method may be called multiple times, in the case where the job is resubmitted,
   * due to a broken connection to the server for instance.
   * @param event encapsulates the event that caused this method to be called.
   */
  void jobEnded(JobEvent event);

  /**
   * Called when a job, or a subset of its tasks, is sent to the server, or to the local executor.
   * <p>This method may be called multiple times, depending on the client load-balancer configuration,
   * and on how it will split the jobs into subsets of their tasks.
   * @param event encapsulates the event that caused this method to be called.
   */
  void jobDispatched(JobEvent event);

  /**
   * Called when the execution of a subset of the tasks of a job is complete.
   * <p>This method may be called multiple times, depending on the client load-balancer configuration,
   * and on how it will split the jobs into subsets of their tasks.
   * @param event encapsulates the event that caused this method to be called.
   */
  void jobReturned(JobEvent event);
}
