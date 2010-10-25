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

import javax.resource.*;
import javax.resource.cci.*;
import javax.resource.spi.ConnectionEvent;

import org.jppf.client.JPPFJob;
import org.jppf.jca.spi.JPPFManagedConnection;
import org.jppf.jca.util.JPPFAccessorImpl;
import org.jppf.jca.work.submission.*;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;

/**
 * Implementation of a JCA connection. This class provides an API to send tasks to a JPPF driver.
 * @author Laurent Cohen
 */
public class JPPFConnectionImpl extends JPPFAccessorImpl implements JPPFConnection
{
	/**
	 * Determines whether this connection has been closed.
	 */
	private boolean closed = true;
	/**
	 * The associated managed connection.
	 */
	private JPPFManagedConnection managedConnection;

	/**
	 * Initialize this connection from a managed connection.
	 * @param conn a <code>ManagedConnection</code> instance.
	 */
	public JPPFConnectionImpl(JPPFManagedConnection conn)
	{
		this.managedConnection = conn;
	}

	/**
	 * Close this connection and notify the associated managed connection.
	 * @see javax.resource.cci.Connection#close()
	 */
	public void close()
	{
		closed = true;
		if (managedConnection != null) managedConnection.fireConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED, null);
	}

	/**
	 * Create an interaction.
	 * @return an <code>Interaction</code> instance.
	 * @see javax.resource.cci.Connection#createInteraction()
	 */
	public Interaction createInteraction()
	{
		return new JPPFInteraction(this);
	}

	/**
	 * Transaction management is not supported in this version.
	 * @return nothing.
	 * @throws ResourceException this method always throws a NotSupportedException.
	 * @see javax.resource.cci.Connection#getLocalTransaction()
	 */
	public LocalTransaction getLocalTransaction() throws ResourceException
	{
		throw new NotSupportedException("Method not supported");
	}

	/**
	 * Get the connection metadata.
	 * @return a <code>JPPFConnectionMetaData</code> instance.
	 * @see javax.resource.cci.Connection#getMetaData()
	 */
	public ConnectionMetaData getMetaData()
	{
		return new JPPFConnectionMetaData(null);
	}

	/**
	 * This method is not supported in this version.
	 * @return nothing.
	 * @throws ResourceException this method always throws a NotSupportedException.
	 * @see javax.resource.cci.Connection#getResultSetInfo()
	 */
	public ResultSetInfo getResultSetInfo() throws ResourceException
	{
		throw new NotSupportedException("Method not supported");
	}

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
	public String submitNonBlocking(List<JPPFTask> tasks, DataProvider dataProvider) throws Exception
	{
		return submitNonBlocking(null, tasks, dataProvider, null);
	}

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
	public String submitNonBlocking(ExecutionPolicy policy, List<JPPFTask> tasks, DataProvider dataProvider) throws Exception
	{
		return submitNonBlocking(policy, tasks, dataProvider, null);
	}

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
	public String submitNonBlocking(List<JPPFTask> tasks, DataProvider dataProvider,
		SubmissionStatusListener listener) throws Exception
	{
		return submitNonBlocking(null, tasks, dataProvider, listener);
	}

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
	public String submitNonBlocking(ExecutionPolicy policy, List<JPPFTask> tasks, DataProvider dataProvider,
		SubmissionStatusListener listener) throws Exception
	{
		JPPFJobSLA sla = new JPPFJobSLA(policy);
		JPPFJob job = new JPPFJob(dataProvider, sla);
		for (JPPFTask task: tasks) job.addTask(task);
		return getJppfClient().getSubmissionManager().addSubmission(job, listener);
	}

	/**
	 * Submit an asynchronous execution request to the JPPF client.<br>
	 * This method exits immediately after adding the request to the requests queue.<br>
	 * The returned id is used to later retieve the results and sttaus of the execution. 
	 * @param job - the job to execute.
	 * @return the id of the submission, to use for later retrieval of the results and status of the submission.
	 * @throws Exception if an error occurs while submitting the request.
	 */
	public String submitNonBlocking(JPPFJob job) throws Exception
	{
		return getJppfClient().getSubmissionManager().addSubmission(job);
	}

	/**
	 * Submit an asynchronous execution request to the JPPF client.<br>
	 * This method exits immediately after adding the request to the requests queue.<br>
	 * The returned id is used to later retieve the results and sttaus of the execution. 
	 * @param job - the job to execute.
	 * @param listener - an optional listener to receive submission status change notifications, may be null.
	 * @return the id of the submission, to use for later retrieval of the results and status of the submission.
	 * @throws Exception if an error occurs while submitting the request.
	 */
	public String submitNonBlocking(JPPFJob job, SubmissionStatusListener listener) throws Exception
	{
		job.setBlocking(false);
		return getJppfClient().getSubmissionManager().addSubmission(job, listener);
	}

	/**
	 * Add a listener to the submission with the specified id.
	 * @param submissionId - the id of the submission.
	 * @param listener - the listener to add.
	 */
	public void addSubmissionStatusListener(String submissionId, SubmissionStatusListener listener)
	{
		JPPFSubmissionResult res = getSubmissionResult(submissionId);
		if (res != null) res.addSubmissionStatusListener(listener);
	}

	/**
	 * Remove a listener from the submission with the specified id.
	 * @param submissionId the id of the submission.
	 * @param listener the listener to remove.
	 */
	public void removeSubmissionStatusListener(String submissionId, SubmissionStatusListener listener)
	{
		JPPFSubmissionResult res = getSubmissionResult(submissionId);
		if (res != null) res.removeSubmissionStatusListener(listener);
	}

	/**
	 * Get the execution status of a tasks submission.
	 * @param submissionId the id of the submission for which to get the status.
	 * @return the submission status.
	 * @throws Exception if an error occurs while submitting the request.
	 */
	public SubmissionStatus getSubmissionStatus(String submissionId) throws Exception
	{
		JPPFSubmissionResult res = getSubmissionResult(submissionId);
		if (res == null) return null;
		return res.getStatus();
	}

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
	public List<JPPFTask> getSubmissionResults(String submissionId) throws Exception
	{
		JPPFSubmissionManager mgr = getJppfClient().getSubmissionManager();
		JPPFSubmissionResult res = mgr.peekSubmission(submissionId);
		if (res == null) return null;
		res = mgr.pollSubmission(submissionId);
		return res.getResults();
	}

	/**
	 * Get the submission result with the specified id.
	 * @param submissionId the id of the submission to find.
	 * @return a <code>JPPFSubmissionResult</code> instance, or null if no submission can be found for the specified id.
	 */
	private JPPFSubmissionResult getSubmissionResult(String submissionId)
	{
		return getJppfClient().getSubmissionManager().peekSubmission(submissionId);
	}

	/**
	 * Get the ids of all currently available submissions.
	 * @return a collection of ids as strings.
	 */
	public Collection<String> getAllSubmissionIds()
	{
		return getJppfClient().getSubmissionManager().getAllSubmissionIds();
	}

	/**
	 * Determine whether this connection has been closed.
	 * @return true if the connection was closed, false otherwise.
	 */
	public boolean isClosed()
	{
		return closed;
	}

	/**
	 * Set the closed  state of this connection.
	 */
	public void setAvailable()
	{
		this.closed = false;
	}

	/**
	 * Get the associated managed connection.
	 * @return a <code>JPPFManagedConnection</code> instance.
	 */
	public JPPFManagedConnection getManagedConnection()
	{
		return managedConnection;
	}

	/**
	 * Set the associated managed connection.
	 * @param conn a <code>JPPFManagedConnection</code> instance.
	 */
	public void setManagedConnection(JPPFManagedConnection conn)
	{
		this.managedConnection = conn;
	}
}
