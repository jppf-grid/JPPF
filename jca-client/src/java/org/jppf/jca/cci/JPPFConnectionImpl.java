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

import javax.resource.*;
import javax.resource.cci.*;
import javax.resource.spi.ConnectionEvent;

import org.jppf.client.*;
import org.jppf.client.event.SubmissionStatusListener;
import org.jppf.client.submission.SubmissionStatus;
import org.jppf.jca.spi.JPPFManagedConnection;
import org.jppf.jca.work.JcaSubmissionManager;
import org.jppf.node.protocol.Task;
import org.slf4j.*;

/**
 * Implementation of a JCA connection. This class provides an API to send tasks to a JPPF driver.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFConnectionImpl implements JPPFConnection
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFConnectionImpl.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
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

  @Override
  public void close()
  {
    if (managedConnection != null) managedConnection.fireConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED, null);
  }

  @Override
  public Interaction createInteraction()
  {
    return new JPPFInteraction(this);
  }

  /**
   * Transaction management is not supported in this version.
   * @return nothing.
   * @throws ResourceException this method always throws a NotSupportedException.
   */
  @Override
  public LocalTransaction getLocalTransaction() throws ResourceException
  {
    throw new NotSupportedException("Method not supported");
  }

  @Override
  public ConnectionMetaData getMetaData()
  {
    return new JPPFConnectionMetaData(null);
  }

  /**
   * This method is not supported in this version.
   * @return nothing.
   * @throws ResourceException this method always throws a NotSupportedException.
   */
  @Override
  public ResultSetInfo getResultSetInfo() throws ResourceException
  {
    throw new NotSupportedException("Method not supported");
  }

  @Override
  public String submit(final JPPFJob job) throws Exception
  {
    return submit(job, null);
  }

  @Override
  @SuppressWarnings("deprecation")
  public String submit(final JPPFJob job, final SubmissionStatusListener listener) throws Exception
  {
    job.setBlocking(false);
    if (listener != null) job.getResultCollector().addSubmissionStatusListener(listener);
    managedConnection.retrieveJppfClient().submitJob(job);
    return job.getUuid();
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addSubmissionStatusListener(final String submissionId, final SubmissionStatusListener listener)
  {
    JPPFJob res = getJob(submissionId);
    if (res != null) res.getResultCollector().addSubmissionStatusListener(listener);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void removeSubmissionStatusListener(final String submissionId, final SubmissionStatusListener listener)
  {
    JPPFJob res = getJob(submissionId);
    if (res != null) res.getResultCollector().removeSubmissionStatusListener(listener);
  }

  @Override
  public SubmissionStatus getSubmissionStatus(final String submissionId) throws Exception
  {
    JPPFJob res = getJob(submissionId);
    if (res == null) return null;
    return res.getStatus();
  }

  /**
   * Get the results of an execution request.<br>
   * This method should be called only once a call to
   * {@link #getSubmissionStatus(java.lang.String submissionId) getSubmissionStatus(submissionId)} has returned
   * either {@link org.jppf.client.submission.SubmissionStatus#COMPLETE COMPLETE} or
   * {@link org.jppf.client.submission.SubmissionStatus#FAILED FAILED}
   * @param submissionId the id of the submission for which to get the execution results.
   * @return the list of resulting JPPF tasks, or null if the execution failed.
   * @throws Exception if an error occurs while submitting the request.
   */
  @Override
  public List<Task<?>> getResults(final String submissionId) throws Exception
  {
    JcaSubmissionManager mgr = (JcaSubmissionManager) managedConnection.retrieveJppfClient().getSubmissionManager();
    JPPFJob res = mgr.peekSubmission(submissionId);
    if (res == null) return null;
    res = mgr.pollSubmission(submissionId);
    return res.getAllResults();
  }

  /**
   * Get the submission result with the specified id.
   * @param submissionId the id of the submission to find.
   * @return a <code>JPPFSubmissionResult</code> instance, or null if no submission can be found for the specified id.
   */
  private @SuppressWarnings("deprecation") JPPFJob getJob(final String submissionId)
  {
    return ((JcaSubmissionManager) managedConnection.retrieveJppfClient().getSubmissionManager()).peekSubmission(submissionId);
  }

  @Override
  public Collection<String> getAllSubmissionIds()
  {
    return ((JcaSubmissionManager) managedConnection.retrieveJppfClient().getSubmissionManager()).getAllSubmissionIds();
  }

  @Override
  public boolean cancelJob(final String submissionId) throws Exception
  {
    return managedConnection.retrieveJppfClient().cancelJob(submissionId);
  }

  /**
   * Set the associated managed connection.
   * @param conn a <code>JPPFManagedConnection</code> instance.
   * @exclude
   */
  public void setManagedConnection(final JPPFManagedConnection conn)
  {
    this.managedConnection = conn;
  }

  @Override
  @SuppressWarnings("deprecation")
  public List<Task<?>> awaitResults(final String submissionId) throws Exception
  {
    JPPFJob job = getJob(submissionId);
    if (debugEnabled) log.debug("job = " + job);
    if (job == null) return null;
    List<Task<?>> tasks = job.awaitResults();
    ((JcaSubmissionManager) managedConnection.retrieveJppfClient().getSubmissionManager()).pollSubmission(submissionId);
    return tasks;
  }

  @Override
  public void resetClient()
  {
    if (managedConnection != null) managedConnection.resetClient();
  }
}
