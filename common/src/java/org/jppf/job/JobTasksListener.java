/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.job;

import java.util.EventListener;

/**
 * Listener interface for classes that wish to receive notifications of job tasks dispatched or returned in the driver.
 * @author Laurent Cohen
 */
public interface JobTasksListener extends EventListener {
  /**
   * Called when a set of tasks from a job is dispatched to a node.
   * @param event encapsulates information on the job dispatch.
   */
  void tasksDispatched(JobTasksEvent event);

  /**
   * Called when a job dispatch returns from a node.
   * @param event encapsulates information on the job dispatch.
   */
  void tasksReturned(JobTasksEvent event);

  /**
   * Called when tasks results are about to be sent back to the client.
   * @param event encapsulates information on the tasks results.
   */
  void resultsReceived(JobTasksEvent event);
}
