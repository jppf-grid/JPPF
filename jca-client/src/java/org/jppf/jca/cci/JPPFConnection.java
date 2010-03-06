/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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
import org.jppf.jca.spi.JPPFManagedConnection;
import org.jppf.jca.util.JPPFAccessor;
import org.jppf.jca.work.submission.*;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;

/**
 * Interface for JPPF JCA connections. This interface provides an API to send jobs to a JPPF driver.
 * @author Laurent Cohen
 */
public interface JPPFConnection extends Connection, JPPFAccessor
{
	/**
	 * Submit an asynchronous execution request to the JPPF client.<br>
	 * This method exits immediately after adding the request to the requests queue.<br>
	 * The returned id is used to later retieve the results and sttaus of the execution. 
	 * @param tasks - the list of tasks to execute remotely.
	 * @param dataProvider - the provider of the data shared among tasks, may be null.
	 * @return the id of the submission, to use for later retrieval of the results and status of the submission.
	 * @throws Exception if an error occurs while submitting the request.
	 * @deprecated this method is deprecated, {@link #submitNonBlocking(JPPFJob) submitNonBlocking(JPPFJob)} or
	 * {@link #submitNonBlocking(JPPFJob,SubmissionStatusListener) submitNonBlocking(JPPFJob, SubmissionStatusListener)} should be used instead.
	 */
	String submitNonBlocking(List<JPPFTask> tasks, DataProvider dataProvider) throws Exception;

	/**
	 * Submit an asynchronous execution request to the JPPF client.<br>
	 * This method exits immediately after adding the request to the requests queue.<br>
	 * The returned id is used to later retieve the results and sttaus of the execution. 
	 * @param policy - an execution policy that deternmines on which node(s) the tasks will be permitted to run.
	 * @param tasks - the list of tasks to execute remotely.
	 * @param dataProvider - the provider of the data shared among tasks, may be null.
	 * @return the id of the submission, to use for later retrieval of the results and status of the submission.
	 * @throws Exception if an error occurs while submitting the request.
	 * @deprecated this method is deprecated, {@link #submitNonBlocking(JPPFJob) submitNonBlocking(JPPFJob)} or
	 * {@link #submitNonBlocking(JPPFJob,SubmissionStatusListener) submitNonBlocking(JPPFJob, SubmissionStatusListener)} should be used instead.
	 */
	String submitNonBlocking(ExecutionPolicy policy, List<JPPFTask> tasks, DataProvider dataProvider) throws Exception;

	/**
	 * Submit an asynchronous execution request to the JPPF client.<br>
	 * This method exits immediately after adding the request to the requests queue.<br>
	 * The returned id is used to later retieve the results and sttaus of the execution. 
	 * @param tasks - the list of tasks to execute remotely.
	 * @param dataProvider - the provider of the data shared among tasks, may be null.
	 * @param listener - an optional listener to receive submission status change notifications, may be null.
	 * @return the id of the submission, to use for later retrieval of the results and status of the submission.
	 * @throws Exception if an error occurs while submitting the request.
	 * @deprecated this method is deprecated, {@link #submitNonBlocking(JPPFJob) submitNonBlocking(JPPFJob)} or
	 * {@link #submitNonBlocking(JPPFJob,SubmissionStatusListener) submitNonBlocking(JPPFJob, SubmissionStatusListener)} should be used instead.
	 */
	String submitNonBlocking(List<JPPFTask> tasks, DataProvider dataProvider, SubmissionStatusListener listener) throws Exception;

	/**
	 * Submit an asynchronous execution request to the JPPF client.<br>
	 * This method exits immediately after adding the request to the requests queue.<br>
	 * The returned id is used to later retieve the results and sttaus of the execution. 
	 * @param policy - an execution policy that deternmines on which node(s) the tasks will be permitted to run.
	 * @param tasks - the list of tasks to execute remotely.
	 * @param dataProvider - the provider of the data shared among tasks, may be null.
	 * @param listener - an optional listener to receive submission status change notifications, may be null.
	 * @return the id of the submission, to use for later retrieval of the results and status of the submission.
	 * @throws Exception if an error occurs while submitting the request.
	 * @deprecated this method is deprecated, {@link #submitNonBlocking(JPPFJob) submitNonBlocking(JPPFJob)} or
	 * {@link #submitNonBlocking(JPPFJob,SubmissionStatusListener) submitNonBlocking(JPPFJob, SubmissionStatusListener)} should be used instead.
	 */
	String submitNonBlocking(ExecutionPolicy policy, List<JPPFTask> tasks, DataProvider dataProvider, SubmissionStatusListener listener) throws Exception;

	/**
	 * Submit an asynchronous execution request to the JPPF client.<br>
	 * This method exits immediately after adding the request to the requests queue.<br>
	 * The returned id is used to later retieve the results and sttaus of the execution. 
	 * @param job - the job to execute.
	 * @return the id of the submission, to use for later retrieval of the results and status of the submission.
	 * @throws Exception if an error occurs while submitting the request.
	 */
	String submitNonBlocking(JPPFJob job) throws Exception;

	/**
	 * Submit an asynchronous execution request to the JPPF client.<br>
	 * This method exits immediately after adding the request to the requests queue.<br>
	 * The returned id is used to later retieve the results and sttaus of the execution. 
	 * @param job - the job to execute.
	 * @param listener - an optional listener to receive submission status change notifications, may be null.
	 * @return the id of the submission, to use for later retrieval of the results and status of the submission.
	 * @throws Exception if an error occurs while submitting the request.
	 */
	String submitNonBlocking(JPPFJob job, SubmissionStatusListener listener) throws Exception;

	/**
	 * Add a listener to the submission with the specified id.
	 * @param submissionId - the id of the submission.
	 * @param listener - the listener to add.
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
	 * {@link #getSubmissionStatus(java.lang.String submissionId) getSubmissionStatus(submissionId)} has returned
	 * either {@link org.jppf.jca.work.submission.SubmissionStatus#COMPLETE COMPLETE} or
	 * {@link org.jppf.jca.work.submission.SubmissionStatus#FAILED FAILED}
	 * @param submissionId the id of the submission for which to get the execution results.
	 * @return the list of resulting JPPF tasks, or null if the execution failed.
	 * @throws Exception if an error occurs while submitting the request.
	 */
	List<JPPFTask> getSubmissionResults(String submissionId) throws Exception;

	/**
	 * Get the ids of all currently available submissions.
	 * @return a collection of ids as strings.
	 */
	Collection<String> getAllSubmissionIds();

	/**
	 * Determine whether this connection has been closed.
	 * @return true if the connection was closed, false otherwise.
	 */
	boolean isClosed();

	/**
	 * Set the closed  state of this connection.
	 */
	void setAvailable();

	/**
	 * Get the associated managed connection.
	 * @return a <code>JPPFManagedConnection</code> instance.
	 */
	JPPFManagedConnection getConn();

	/**
	 * Set the associated managed connection.
	 * @param conn a <code>JPPFManagedConnection</code> instance.
	 */
	void setConn(JPPFManagedConnection conn);

}
