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

package org.jppf.jca.cci;

import java.util.*;

import javax.resource.*;
import javax.resource.cci.*;
import javax.resource.spi.ConnectionEvent;

import org.jppf.client.*;
import org.jppf.client.event.SubmissionStatusListener;
import org.jppf.jca.spi.JPPFManagedConnection;
import org.jppf.jca.util.JPPFAccessorImpl;
import org.jppf.jca.work.submission.*;
import org.jppf.server.protocol.JPPFTask;

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
	public JPPFConnectionImpl(final JPPFManagedConnection conn)
	{
		this.managedConnection = conn;
	}

	/**
	 * Close this connection and notify the associated managed connection.
	 * @see javax.resource.cci.Connection#close()
	 */
	@Override
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
	@Override
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
	@Override
	public LocalTransaction getLocalTransaction() throws ResourceException
	{
		throw new NotSupportedException("Method not supported");
	}

	/**
	 * Get the connection metadata.
	 * @return a <code>JPPFConnectionMetaData</code> instance.
	 * @see javax.resource.cci.Connection#getMetaData()
	 */
	@Override
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
	@Override
	public ResultSetInfo getResultSetInfo() throws ResourceException
	{
		throw new NotSupportedException("Method not supported");
	}

	/**
	 * Submit an asynchronous execution request to the JPPF client.<br>
	 * This method exits immediately after adding the request to the requests queue.<br>
	 * The returned id is used to later retieve the results and sttaus of the execution.
	 * @param job the job to execute.
	 * @return the id of the submission, to use for later retrieval of the results and status of the submission.
	 * @throws IllegalArgumentException if the job is null or empty.
	 * @throws Exception if an error occurs while submitting the request.
	 */
	@Override
	public String submitNonBlocking(final JPPFJob job) throws Exception
	{
		if (job == null) throw new IllegalArgumentException("job cannot be null");
		if (job.getTasks().isEmpty()) throw new IllegalArgumentException("job cannot be empty");
		return getJppfClient().getSubmissionManager().submitJob(job);
	}

	/**
	 * Submit an asynchronous execution request to the JPPF client.<br>
	 * This method exits immediately after adding the request to the requests queue.<br>
	 * The returned id is used to later retieve the results and sttaus of the execution.
	 * @param job the job to execute.
	 * @param listener an optional listener to receive submission status change notifications, may be null.
	 * @return the id of the submission, to use for later retrieval of the results and status of the submission.
	 * @throws IllegalArgumentException if the job is null or empty.
	 * @throws Exception if an error occurs while submitting the request.
	 */
	@Override
	public String submitNonBlocking(final JPPFJob job, final SubmissionStatusListener listener) throws Exception
	{
		if (job == null) throw new IllegalArgumentException("job cannot be null");
		if (job.getTasks().isEmpty()) throw new IllegalArgumentException("job cannot be empty");
		job.setBlocking(false);
		return getJppfClient().getSubmissionManager().submitJob(job, listener);
	}

	/**
	 * Add a listener to the submission with the specified id.
	 * @param submissionId the id of the submission.
	 * @param listener the listener to add.
	 */
	@Override
	public void addSubmissionStatusListener(final String submissionId, final SubmissionStatusListener listener)
	{
		JcaSubmissionResult res = getSubmissionResult(submissionId);
		if (res != null) res.addSubmissionStatusListener(listener);
	}

	/**
	 * Remove a listener from the submission with the specified id.
	 * @param submissionId the id of the submission.
	 * @param listener the listener to remove.
	 */
	@Override
	public void removeSubmissionStatusListener(final String submissionId, final SubmissionStatusListener listener)
	{
		JcaSubmissionResult res = getSubmissionResult(submissionId);
		if (res != null) res.removeSubmissionStatusListener(listener);
	}

	/**
	 * Get the execution status of a tasks submission.
	 * @param submissionId the id of the submission for which to get the status.
	 * @return the submission status.
	 * @throws Exception if an error occurs while submitting the request.
	 */
	@Override
	public SubmissionStatus getSubmissionStatus(final String submissionId) throws Exception
	{
		JcaSubmissionResult res = getSubmissionResult(submissionId);
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
	@Override
	public List<JPPFTask> getSubmissionResults(final String submissionId) throws Exception
	{
		JcaSubmissionManager mgr = getJppfClient().getSubmissionManager();
		JcaSubmissionResult res = mgr.peekSubmission(submissionId);
		if (res == null) return null;
		res = mgr.pollSubmission(submissionId);
		return res.getResults();
	}

	/**
	 * Get the submission result with the specified id.
	 * @param submissionId the id of the submission to find.
	 * @return a <code>JPPFSubmissionResult</code> instance, or null if no submission can be found for the specified id.
	 */
	private JcaSubmissionResult getSubmissionResult(final String submissionId)
	{
		return getJppfClient().getSubmissionManager().peekSubmission(submissionId);
	}

	/**
	 * Get the ids of all currently available submissions.
	 * @return a collection of ids as strings.
	 */
	@Override
	public Collection<String> getAllSubmissionIds()
	{
		return getJppfClient().getSubmissionManager().getAllSubmissionIds();
	}

	/**
	 * Determine whether this connection has been closed.
	 * @return true if the connection was closed, false otherwise.
	 */
	@Override
	public boolean isClosed()
	{
		return closed;
	}

	/**
	 * Set the closed  state of this connection.
	 */
	@Override
	public void setAvailable()
	{
		this.closed = false;
	}

	/**
	 * Get the associated managed connection.
	 * @return a <code>JPPFManagedConnection</code> instance.
	 */
	@Override
	public JPPFManagedConnection getManagedConnection()
	{
		return managedConnection;
	}

	/**
	 * Set the associated managed connection.
	 * @param conn a <code>JPPFManagedConnection</code> instance.
	 */
	@Override
	public void setManagedConnection(final JPPFManagedConnection conn)
	{
		this.managedConnection = conn;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<JPPFTask> waitForResults(final String submissionId) throws Exception
	{
		JcaSubmissionResult result = getSubmissionResult(submissionId);
		if (result == null) return null;
		result.waitForResults(0L);
		return result.getResults();
	}
}
