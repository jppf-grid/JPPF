/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.util.List;

import org.jppf.client.balancer.SubmissionManagerClient;
import org.jppf.client.event.ClientListener;
import org.jppf.client.submission.SubmissionManager;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

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
   * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
   */
  public JPPFClient(final ClientListener... listeners)
  {
    super(null, JPPFConfiguration.getProperties(), listeners);
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
   * Initialize this client with the specified application UUID and new connection listeners.
   * @param uuid the unique identifier for this local client.
   * @param config the JPPF configuration to use for this client.
   * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
   */
  public JPPFClient(final String uuid, final TypedProperties config, final ClientListener... listeners)
  {
    super(uuid, config, listeners);
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  protected AbstractJPPFClientConnection createConnection(final String uuid, final String name, final JPPFConnectionInformation info, final boolean ssl)
  {
    return new JPPFClientConnectionImpl(this, uuid, name, info, ssl);
  }

  /**
   * {@inheritDoc}
   * @deprecated use {@link #submitJob(JPPFJob)} instead.
   */
  @Deprecated
  @Override
  public List<JPPFTask> submit(final JPPFJob job) throws Exception
  {
    if (job == null) throw new IllegalArgumentException("job cannot be null");
    if (job.getJobTasks().isEmpty()) throw new IllegalArgumentException("job cannot be empty");
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

  @Override
  public List<Task<?>> submitJob(final JPPFJob job) throws Exception
  {
    if (job == null) throw new IllegalArgumentException("job cannot be null");
    if (job.getJobTasks().isEmpty()) throw new IllegalArgumentException("job cannot be empty");
    if ((job.getResultListener() == null) ||
        (job.isBlocking() && !(job.getResultListener() instanceof JPPFResultCollector))) job.setResultListener(new JPPFResultCollector(job));
    SubmissionManager submissionManager = getSubmissionManager();
    submissionManager.submitJob(job);
    if (job.isBlocking())
    {
      JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
      return collector.awaitResults();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  protected SubmissionManager createSubmissionManager()
  {
    SubmissionManager submissionManager = null;
    try
    {
      submissionManager = new SubmissionManagerClient(this);
    }
    catch (Exception e)
    {
      log.error("Can't initialize Submission Manager", e);
    }
    return submissionManager;
  }

  /**
   * Reset this client, that is, close it if necessary, reload its configuration, then open it again.
   * @see #reset(TypedProperties)
   * @since 4.0
   */
  public void reset()
  {
    if (isClosed()) throw new IllegalStateException("This client has already been closed and cannot be reset - a new instance must be created");
    close(true);
    JPPFConfiguration.reset();
    init(JPPFConfiguration.getProperties());
  }

  /**
   * Reset this client, that is, close it if necessary, then open it again, using the specified confguration.
   * @param configuration the configuration to initialize this client with.
   * @see #reset()
   * @since 4.0
   */
  public void reset(final TypedProperties configuration)
  {
    if (isClosed()) throw new IllegalStateException("This client has already been closed and cannot be reset - a new instance must be created");
    close(true);
    init(configuration);
  }
}
