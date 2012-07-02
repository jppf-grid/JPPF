/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
package org.jppf.client;

import org.jppf.client.balancer.SubmissionManagerClient;
import org.jppf.client.event.ClientListener;
import org.jppf.client.submission.SubmissionManager;
import org.jppf.client.submission.SubmissionManagerImpl;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.TypedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from
 * the submitting application should be dynamically reloaded or not, depending on whether
 * the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFClient extends AbstractGenericClient
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFClient.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this client with an automatically generated application UUID.
   */
  public JPPFClient()
  {
    super(null, JPPFConfiguration.getProperties());
  }

  /**
   * Initialize this client with an automatically generated application UUID.
   * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
   */
  public JPPFClient(final ClientListener... listeners)
  {
    super(null, JPPFConfiguration.getProperties(), listeners);
  }

  /**
   * Initialize this client with a specified application UUID.
   * @param uuid the unique identifier for this local client.
   */
  public JPPFClient(final String uuid)
  {
    super(uuid, JPPFConfiguration.getProperties());
  }

  /**
   * Initialize this client with the specified application UUID and new connection listeners.
   * @param uuid the unique identifier for this local client.
   * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
   */
  public JPPFClient(final String uuid, final ClientListener... listeners)
  {
    super(uuid, JPPFConfiguration.getProperties(), listeners);
  }

  /**
   * Initialize this client's configuration.
   * @param configuration an object holding the JPPF configuration.
   */
  @Override
  protected void initConfig(final Object configuration)
  {
    config = (TypedProperties) configuration;
  }

  /**
   * Create a new driver connection based on the specified parameters.
   * @param uuid the uuid of the JPPF client.
   * @param name the name of the connection.
   * @param info the driver connection information.
   * @param ssl determines whether this is an SSL connection.
   * @return an instance of a subclass of {@link AbstractJPPFClientConnection}.
   */
  @Override
  protected AbstractJPPFClientConnection createConnection(final String uuid, final String name, final JPPFConnectionInformation info, final boolean ssl)
  {
    return new JPPFClientConnectionImpl(this, uuid, name, info, ssl);
  }

  /**
   * Submit a job execution request.
   * @param job the job to execute.
   * @return the list of executed tasks with their results for a blocking job, or <code>null</code> for a non-blocking job.
   * @throws IllegalArgumentException if the job is null or empty.
   * @throws Exception if an error occurs while sending the request.
   * @see org.jppf.client.AbstractJPPFClient#submit(org.jppf.client.JPPFJob)
   */
  @Override
  public List<JPPFTask> submit(final JPPFJob job) throws Exception
  {
    if (job == null) throw new IllegalArgumentException("job cannot be null");
    if (job.getTasks().isEmpty()) throw new IllegalArgumentException("job cannot be empty");
    if ((job.getResultListener() == null) ||
            (job.isBlocking() && !(job.getResultListener() instanceof JPPFResultCollector))) job.setResultListener(new JPPFResultCollector(job));
    SubmissionManager submissionManager = getSubmissionManager();
    submissionManager.submitJob(job);
    if (job.isBlocking())
    {
      JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
      return collector.waitForResults();
    }
    return null;
  }

  /**
   * Send a request to get the statistics collected by the JPPF server.
   * @return a <code>JPPFStats</code> instance.
   * @throws Exception if an error occurred while trying to get the server statistics.
   * @deprecated this method does not allow to chose which driver to get the statistics from.
   * Use <code>((JPPFClientConnectionImpl) getConnection(java.lang.String)).getJmxConnection().statistics()</code> instead.
   */
  public JPPFStats requestStatistics() throws Exception
  {
    JPPFClientConnectionImpl conn = (JPPFClientConnectionImpl) getClientConnection(true);
    return (conn == null) ? null : conn.getJmxConnection().statistics();
  }

  /**
   * Close this client and release all the resources it is using.
   */
  @Override
  public void close()
  {
    super.close();
    if (debugEnabled) log.debug("closing submission manager");
    SubmissionManager submissionManager = getSubmissionManager();
    if (submissionManager != null) submissionManager.close();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected SubmissionManager createSubmissionManager()
  {
    SubmissionManager submissionManager = null;
    try
    {
      if (config != null && config.getBoolean("jppf.balancer.old.enabled", false))
      {
        submissionManager = new SubmissionManagerImpl(this);
      }
      else
      {
        submissionManager = new SubmissionManagerClient(this);
      }
    }
    catch (Exception e)
    {
      log.error("Can't initialize Submission Manager", e);
    }
    if (submissionManager != null) new Thread(submissionManager, "SubmissionManager").start();
    return submissionManager;
  }

  /**
   * Cancel the job with the specified id.
   * @param jobId the id of the job to cancel.
   * @throws Exception if any error occurs.
   * @see org.jppf.server.job.management.DriverJobManagementMBean#cancelJob(java.lang.String)
   * @return a <code>true</code> when cancel was successful <code>false</code> otherwise.
   */
  @Override
  public boolean cancelJob(final String jobId) throws Exception
  {
    if (jobId == null || jobId.isEmpty()) throw new IllegalArgumentException("jobId is blank");

    SubmissionManager submissionManager = getSubmissionManager();
    if (submissionManager instanceof SubmissionManagerClient)
      return ((SubmissionManagerClient) submissionManager).cancelJob(jobId);
    else
      return super.cancelJob(jobId);
  }
}
