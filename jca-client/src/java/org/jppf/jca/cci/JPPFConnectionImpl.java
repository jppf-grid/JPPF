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

package org.jppf.jca.cci;

import java.util.*;

import javax.resource.*;
import javax.resource.cci.*;
import javax.resource.spi.ConnectionEvent;

import org.jppf.client.*;
import org.jppf.client.event.JobStatusListener;
import org.jppf.jca.spi.JPPFManagedConnection;
import org.jppf.jca.work.JcaJobManager;
import org.jppf.node.protocol.Task;
import org.jppf.utils.LoggingUtils;
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
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
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
  public String submit(final JPPFJob job, final JobStatusListener listener) throws Exception
  {
    job.setBlocking(false);
    if (listener != null) job.addJobStatusListener(listener);
    managedConnection.retrieveJppfClient().submitJob(job);
    return job.getUuid();
  }

  @Override
  public void addJobStatusListener(final String jobUuid, final JobStatusListener listener)
  {
    JPPFJob res = getJob(jobUuid);
    if (res != null) res.addJobStatusListener(listener);
  }

  @Override
  public void removeJobStatusListener(final String jobUuid, final JobStatusListener listener)
  {
    JPPFJob res = getJob(jobUuid);
    if (res != null) res.removeJobStatusListener(listener);
  }

  @Override
  public JobStatus getJobStatus(final String jobUuid) throws Exception
  {
    JPPFJob res = getJob(jobUuid);
    if (res == null) return null;
    return res.getStatus();
  }

  /**
   * Get the results of an execution request.<br>
   * This method should be called only once a call to
   * {@link #getJobStatus(java.lang.String) getJobStatus(jobUuid)} has returned
   * either {@link org.jppf.client.JobStatus#COMPLETE COMPLETE} or
   * {@link org.jppf.client.JobStatus#FAILED FAILED}
   * @param jobUuid the id of the job for which to get the execution results.
   * @return the list of resulting JPPF tasks, or null if the execution failed.
   * @throws Exception if an error occurs while submitting the request.
   */
  @Override
  public List<Task<?>> getResults(final String jobUuid) throws Exception
  {
    JcaJobManager mgr = (JcaJobManager) managedConnection.retrieveJppfClient().getJobManager();
    JPPFJob res = mgr.peekJob(jobUuid);
    if (res == null) return null;
    res = mgr.pollJob(jobUuid);
    return res.getAllResults();
  }

  /**
   * Get the job result with the specified uuid.
   * @param jobUuid the id of the job to find.
   * @return a {@link JPPFJob} instance, or null if no job can be found for the specified uuid.
   */
  private JPPFJob getJob(final String jobUuid)
  {
    return ((JcaJobManager) managedConnection.retrieveJppfClient().getJobManager()).peekJob(jobUuid);
  }

  @Override
  public Collection<String> getAllJobIds()
  {
    return ((JcaJobManager) managedConnection.retrieveJppfClient().getJobManager()).getAllJobUuids();
  }

  @Override
  public boolean cancelJob(final String jobUuid) throws Exception
  {
    return managedConnection.retrieveJppfClient().cancelJob(jobUuid);
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
  public List<Task<?>> awaitResults(final String jobUuid) throws Exception
  {
    JPPFJob job = getJob(jobUuid);
    if (debugEnabled) log.debug("job = " + job);
    if (job == null) return null;
    List<Task<?>> tasks = job.awaitResults();
    ((JcaJobManager) managedConnection.retrieveJppfClient().getJobManager()).pollJob(jobUuid);
    return tasks;
  }

  @Override
  public void resetClient()
  {
    if (managedConnection != null) managedConnection.resetClient();
  }
}
