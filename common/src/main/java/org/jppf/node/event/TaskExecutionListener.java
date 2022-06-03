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

package org.jppf.node.event;

import java.util.EventListener;

/**
 * Interface for all classes that wish to listen to events occurring during the life span of individual JPPF tasks.
 * @author Laurent Cohen
 */
public interface TaskExecutionListener extends EventListener {
  /**
   * Called to notify a listener that a task was executed.
   * @param event the event encapsulating the task-related data.
   */
  void taskExecuted(TaskExecutionEvent event);

  /**
   * Called when a task sends a notification.
   * @param event the event encapsulating the task-related data.
   * @since 4.0
   */
  void taskNotification(TaskExecutionEvent event);
}
