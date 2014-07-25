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

package org.jppf.jca.cci;

import java.util.*;

import javax.resource.cci.Connection;

import org.jppf.client.JPPFJob;
import org.jppf.client.event.SubmissionStatusListener;
import org.jppf.client.submission.SubmissionStatus;
import org.jppf.node.protocol.Task;

/**
 * Interface for JPPF JCA connections. This interface provides an API to send jobs to a JPPF driver.
 * @author Laurent Cohen
 */
public interface JPPFConnection extends Connection
{
  /**
   * Submit a job to the JPPF client.<br>
   * This method exits immediately after adding the request to the requests queue.<br>
   * The returned id is used to later retrieve the results and status of the execution.
   * @param job the job to execute.
   * @return the id of the submission, to use for later retrieval of the results and status of the submission.
   * @throws Exception if an error occurs while submitting the request.
   */
  String submit(JPPFJob job) throws Exception;

  /**
   * Submit a job to the JPPF client.<br>
   * This method exits immediately after adding the request to the requests queue.<br>
   * The returned id is used to later retrieve the results and status of the execution.
   * @param job the job to execute.
   * @param listener an optional listener to receive submission status change notifications, may be null.
   * @return the id of the submission, to use for later retrieval of the results and status of the submission.
   * @throws Exception if an error occurs while submitting the request.
   */
  String submit(JPPFJob job, SubmissionStatusListener listener) throws Exception;

  /**
   * Wait until all results for the specified job submission have been received.
   * @param submissionId the id of the job submission.
   * @return the results as a list of {@link Task} instances.
   * @throws Exception if any error occurs.
   * @since 4.0
   */
  List<Task<?>> awaitResults(final String submissionId) throws Exception;

  /**
   * Cancel the job with the specified id.
   * @param submissionId the id of the job to cancel.
   * @throws Exception if any error occurs.
   * @see org.jppf.server.job.management.DriverJobManagementMBean#cancelJob(java.lang.String)
   * @return a <code>true</code> when cancel was successful <code>false</code> otherwise.
   */
  boolean cancelJob(String submissionId) throws Exception;

  /**
   * Add a listener to the submission with the specified id.
   * @param submissionId the id of the submission.
   * @param listener the listener to add.
   */
  void addSubmissionStatusListener(String submissionId, SubmissionStatusListener listener);

  /**
   * Remove a listener from the submission with the specified id.
   * @param submissionId the id of the submission.
   * @param listener the listener to remove.
   */
  void removeSubmissionStatusListener(String submissionId, SubmissionStatusListener listener);

  /**
   * Get the execution status of a tasks submission.
   * @param submissionId the id of the submission for which to get the status.
   * @return the submission status.
   * @throws Exception if an error occurs while submitting the request.
   */
  SubmissionStatus getSubmissionStatus(String submissionId) throws Exception;

  /**
   * Get the results of an execution request.<br>
   * This method should be called only once a call to
   * {@link #getSubmissionStatus(java.lang.String) getSubmissionStatus(submissionId)} has returned
   * either {@link org.jppf.client.submission.SubmissionStatus#COMPLETE COMPLETE} or
   * {@link org.jppf.client.submission.SubmissionStatus#FAILED FAILED}
   * @param submissionId the id of the submission for which to get the execution results.
   * @return the list of resulting JPPF tasks, or null if the execution failed.
   * @throws Exception if an error occurs while submitting the request.
   * @since 4.0
   */
  List<Task<?>> getResults(final String submissionId) throws Exception;

  /**
   * Get the ids of all currently available submissions.
   * @return a collection of ids as strings.
   */
  Collection<String> getAllSubmissionIds();

  /**
   * Reset the client and reload its configuration.
   * @see org.jppf.client.JPPFClient#reset()
   * @since 4.0
   */
  void resetClient();
}
