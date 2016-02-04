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

/**
 * This enum describes the types of events emitted by a JPPFJobManager.
 * @author Laurent Cohen
 */
public enum JobEventType {
  /**
   * A new job was submitted to the JPPF driver queue.
   */
  JOB_QUEUED,
  /**
   * A job was completed and sent back to the client.
   */
  JOB_ENDED,
  /**
   * A sub-job was dispatched to a node.
   */
  JOB_DISPATCHED,
  /**
   * A sub-job returned from a node.
   */
  JOB_RETURNED,
  /**
   * One or more attributes of a job, possibly including its current number of tasks, has changed.
   */
  JOB_UPDATED
}
