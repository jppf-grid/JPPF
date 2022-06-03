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
package org.jppf.client;

/**
 * The status of a job.
 */
public enum JobStatus {
  /**
   * The job was just submitted.
   */
  SUBMITTED,
  /**
   * The job is currently in the job queue (on the client side).
   */
  PENDING,
  /**
   * The job is being executed.
   */
  EXECUTING,
  /**
   * The job execution is complete.
   */
  COMPLETE,
  /**
   * The job execution has failed.
   */
  FAILED;
  
  /**
   * Return whether this status indicates that the job is done, that is, this status is either {@link #COMPLETE} or {@link #FAILED}.
   * @return true if this status is {@link #COMPLETE} or {@link #FAILED}, false otherwise.
   */
  public boolean isDone() {
    return (this == COMPLETE) || (this == FAILED);
  }
}
