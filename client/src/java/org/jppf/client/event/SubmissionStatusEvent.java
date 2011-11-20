/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import org.jppf.client.SubmissionStatus;


/**
 * Instances of this class represent a status change notification for a jppf submission.
 * @author Laurent Cohen
 */
public class SubmissionStatusEvent extends EventObject
{
  /**
   * The status of the submission.
   */
  private SubmissionStatus status = null;

  /**
   * Initialize this event with the specified submission id and status.
   * @param submissionId the id of the submission whose status has changed.
   * @param status the new status of the submission.
   */
  public SubmissionStatusEvent(final String submissionId, final SubmissionStatus status)
  {
    super(submissionId);
    this.status = status;
  }

  /**
   * The status of the submission.
   * @return a <code>SubmissionStatus</code> typesafe enum value.
   */
  public SubmissionStatus getStatus()
  {
    return status;
  }

  /**
   * Get the id of the submission.
   * @return the submission id as a string.
   */
  public String getSubmissionId()
  {
    return (String) getSource();
  }
}
