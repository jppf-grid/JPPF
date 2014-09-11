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

package org.jppf.node.idle;

import java.util.EventObject;

/**
 * Event emitted when the idle state of an {@link IdleDetectionTask} changes.
 * @author Laurent Cohen
 */
public class IdleStateEvent extends EventObject {
  /**
   * The idle state when this event was emitted.
   */
  private IdleState state = null;

  /**
   * Initialize this event with the task as source.
   * @param task an {@link IdleDetectionTask} instance.
   */
  public IdleStateEvent(final IdleDetectionTask task) {
    super(task);
    this.state = task.getState();
  }

  /**
   * Get the task source of this event.
   * @return an {@link IdleDetectionTask} instance.
   */
  public IdleDetectionTask getTask() {
    return (IdleDetectionTask) getSource();
  }

  /**
   * Get the idle state when this event was emitted.
   * @return an {@link IdleState} enum value.
   */
  public IdleState getState() {
    return state;
  }
}
