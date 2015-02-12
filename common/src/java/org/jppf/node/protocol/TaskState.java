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

package org.jppf.node.protocol;

/**
 * State for task indicating whether result or exception was received.
 * @exclude
 */
public enum TaskState {
  /**
   * Task was just received - no result or exception.
   */
  PENDING,
  /**
   * Task is to be resubmitted.
   */
  RESUBMIT,
  /**
   * An exception was received for the task.
   */
  EXCEPTION,
  /**
   * A result was received for task.
   */
  RESULT,
  /**
   * The task is cancelled.
   */
  CANCELLED
}