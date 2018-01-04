/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.util.EventObject;

import org.jppf.client.JobStatus;


/**
 * Instances of this class represent a status change notification for a jppf job.
 * @author Laurent Cohen
 */
public class JobStatusEvent extends EventObject {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The status of the job.
   */
  private JobStatus status = null;

  /**
   * Initialize this event with the specified job uuid and status.
   * @param jobUuid the uuid of the job whose status has changed.
   * @param status the new status of the job.
   * @exclude
   */
  public JobStatusEvent(final String jobUuid, final JobStatus status) {
    super(jobUuid);
    this.status = status;
  }

  /**
   * The status of the job.
   * @return a <code>SubmissionStatus</code> typesafe enum value.
   */
  public JobStatus getStatus() {
    return status;
  }

  /**
   * Get the id of the job.
   * @return the job id as a string.
   */
  public String getJobUuid() {
    return (String) getSource();
  }
}
