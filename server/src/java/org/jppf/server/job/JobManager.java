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
package org.jppf.server.job;

import org.jppf.job.JobManagerListener;
import org.jppf.server.protocol.ServerJob;

import java.util.Set;

/**
 * Interface for job manager that handles states and monitor the jobs throughout their processing within the JPPF.
 * @author Martin JANDA
 */
public interface JobManager {
  /**
   * Cancel the job with the specified UUID
   *
   * @param jobId the uuid of the job to cancel.
   * @throws Exception when unexpected error occurs.
   * @return whether cancellation was successful.
   */
  boolean cancelJob(final String jobId) throws Exception;

//  /**
//   * Suspend or resume job with the specified UUID
//   *
//   * @param jobId the uuid of the job to suspend/resume.
//   * @param suspend
//   * @return whether action was successful.
//   */
//  public boolean setSuspendJob(final String jobId, final boolean suspend) throws Exception;

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add to the list.
   */
  void addJobListener(final JobManagerListener listener);

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove from the list.
   */
  void removeJobListener(final JobManagerListener listener);

  /**
   * Get the job for the job unique identifier.
   * @param jobUuid the uuid of the job.
   * @return a <code>ServerJob</code> instance.
   */
  ServerJob getBundleForJob(final String jobUuid);

  /**
   * Update the priority of the job with the specified uuid.
   * @param jobUuid the uuid of the job to re-prioritize.
   * @param newPriority the new priority of the job.
   */
  void updatePriority(final String jobUuid, final int newPriority);

  /**
   * Get the set of all job ids.
   * @return the unmodifiable <code>Set</code> of all job ids.
   */
  Set<String> getAllJobIds();
}
