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

import org.jppf.client.*;
import org.jppf.client.event.JobStatusListener;
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
   * @return the uuid of the job, to use for later retrieval of the results and status of the job.
   * @throws Exception if an error occurs while submitting the request.
   */
  String submit(JPPFJob job) throws Exception;

  /**
   * Submit a job to the JPPF client.<br>
   * This method exits immediately after adding the request to the requests queue.<br>
   * The returned id is used to later retrieve the results and status of the execution.
   * @param job the job to execute.
   * @param listener an optional listener to receive job status change notifications, may be null.
   * @return the id of the job, to use for later retrieval of the results and status of the job.
   * @throws Exception if an error occurs while submitting the request.
   */
  String submit(JPPFJob job, JobStatusListener listener) throws Exception;

  /**
   * Wait until all results for the specified job have been received.
   * @param jobUuid the uuid of the job.
   * @return the results as a list of {@link Task} instances.
   * @throws Exception if any error occurs.
   * @since 4.0
   */
  List<Task<?>> awaitResults(final String jobUuid) throws Exception;

  /**
   * Cancel the job with the specified id.
   * @param jobUuid the id of the job to cancel.
   * @throws Exception if any error occurs.
   * @see org.jppf.server.job.management.DriverJobManagementMBean#cancelJob(java.lang.String)
   * @return a <code>true</code> when cancel was successful <code>false</code> otherwise.
   */
  boolean cancelJob(String jobUuid) throws Exception;

  /**
   * Add a listener to the job with the specified id.
   * @param jobUuid the uuid of the jobn.
   * @param listener the listener to add.
   */
  void addJobStatusListener(String jobUuid, JobStatusListener listener);

  /**
   * Remove a listener from the job with the specified uuid.
   * @param jobUuid the id of the job.
   * @param listener the listener to remove.
   */
  void removeJobStatusListener(String jobUuid, JobStatusListener listener);

  /**
   * Get the execution status of a tasks job.
   * @param jobUuid the id of the job for which to get the status.
   * @return the job status.
   * @throws Exception if an error occurs while submitting the request.
   */
  JobStatus getJobStatus(String jobUuid) throws Exception;

  /**
   * Get the results of an execution request.<br>
   * This method should be called only once a call to
   * {@link #getJobStatus(java.lang.String) getJobStatus(String)} has returned
   * either {@link org.jppf.client.JobStatus#COMPLETE COMPLETE} or
   * {@link org.jppf.client.JobStatus#FAILED FAILED}
   * @param jobUuid the id of the job for which to get the execution results.
   * @return the list of resulting JPPF tasks, or null if the execution failed.
   * @throws Exception if an error occurs while submitting the request.
   * @since 4.0
   */
  List<Task<?>> getResults(final String jobUuid) throws Exception;

  /**
   * Get the ids of all currently available jobs.
   * @return a collection of ids as strings.
   */
  Collection<String> getAllJobIds();

  /**
   * Reset the client and reload its configuration.
   * @see org.jppf.client.JPPFClient#reset()
   * @since 4.0
   */
  void resetClient();
}
