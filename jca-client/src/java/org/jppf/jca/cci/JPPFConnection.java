/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
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

import org.jppf.client.ClientExecution;
import org.jppf.jca.spi.JPPFManagedConnection;
import org.jppf.jca.util.JPPFAccessor;
import org.jppf.jca.work.submission.JPPFSubmissionResult;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;

/**
 * Implementation of a JCA connection. This class provides an API to send tasks to a JPPF driver.
 * @author Laurent Cohen
 */
public class JPPFConnection extends JPPFAccessor implements Connection
{
	/**
	 * Determines whether this connection has been closed.
	 */
	private boolean closed = true;
	/**
	 * The associated managed connection.
	 */
	private JPPFManagedConnection conn;

	/**
	 * Initialize this connection from a managed connection.
	 * @param conn a <code>ManagedConnection</code> instance.
	 */
	public JPPFConnection(JPPFManagedConnection conn)
	{
		this.conn = conn;
	}

	/**
	 * Close this connection and notify the associated managed connection.
	 * @see javax.resource.cci.Connection#close()
	 */
	public void close()
	{
		closed = true;
		if (conn != null) conn.fireConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED, null);
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
	 * Submit a task execution request to the JPPF client.
	 * @param tasks the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @return the list of executed tasks with their results.
	 * @throws Exception if an error occurs while sending the request.
	 * @deprecated this method is inherently unsafe for J2EE transactions, as any call to it is blocking
	 * and generally long lived, incurring a serious risk of transaction timeout.
	 */
	public List<JPPFTask> submit(List<JPPFTask> tasks, DataProvider dataProvider) throws Exception
	{
		return getJppfClient().submit(tasks, dataProvider);
	}

	/**
	 * Submit an asynchronous execution request to the JPPF client.<br>
	 * This method exits immediately after adding the request to the requests queue.<br>
	 * The returned id is used to later retieve the results and sttaus of the execution. 
	 * @param tasks the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @return the id of the submission, to use for later retrieval of the results and status of the submission.
	 * @throws Exception if an error occurs while submitting the request.
	 */
	public String submitNonBlocking(List<JPPFTask> tasks, DataProvider dataProvider) throws Exception
	{
		ClientExecution exec = new ClientExecution(tasks, dataProvider, true);
		return getJppfClient().getSubmissionManager().addSubmission(exec);
	}

	/**
	 * Get the execution status of a tasks submission.
	 * @param submissionId the id of the submission for which to get the status.
	 * @return the submission status.
	 * @throws Exception if an error occurs while submitting the request.
	 */
	public JPPFSubmissionResult.Status getSubmissionStatus(String submissionId) throws Exception
	{
		JPPFSubmissionResult res = getJppfClient().getSubmissionManager().peekSubmission(submissionId);
		if (res == null) return null;
		return res.getStatus();
	}

	/**
	 * Get the results of an execution requests.<br>
	 * This method should be called only once a call to
	 * {@link #getSubmissionStatus(java.lang.String submissionId) getSubmissionStatus(submissionId)} has returned
	 * either {@link org.jppf.jca.work.submission.JPPFSubmissionResult.Status#COMPLETE COMPLETE} or
	 * {@link org.jppf.jca.work.submission.JPPFSubmissionResult.Status#FAILED FAILED}
	 * @param submissionId the id of the submission for which to get the execution results.
	 * @return the list of resulting JPPF tasks, or null if the execution failed.
	 * @throws Exception if an error occurs while submitting the request.
	 */
	public List<JPPFTask> getSubmissionResults(String submissionId) throws Exception
	{
		JPPFSubmissionResult res = getJppfClient().getSubmissionManager().pollSubmission(submissionId);
		if (res == null) return null;
		return res.getResults();
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
	public JPPFManagedConnection getConn()
	{
		return conn;
	}

	/**
	 * Set the associated managed connection.
	 * @param conn a <code>JPPFManagedConnection</code> instance.
	 */
	public void setConn(JPPFManagedConnection conn)
	{
		this.conn = conn;
	}
}
