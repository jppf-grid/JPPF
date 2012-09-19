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

package org.jppf.client.submission;

import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.client.loadbalancer.LoadBalancer;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Abstract super class for J2SE and JCA submission managers.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractSubmissionManager extends ThreadSynchronization implements SubmissionManager, ClientConnectionStatusListener
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractSubmissionManager.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether trace-level logging is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Maximum wait time in milliseconds in the the submission manager loop.
   */
  private final long maxWaitMillis;
  /**
   * Maximum wait time in milliseconds in the the submission manager loop.
   */
  private final int maxWaitNanos;
  /**
   * The queue of submissions pending execution.
   */
  protected ConcurrentLinkedQueue<JPPFJob> execQueue = new ConcurrentLinkedQueue<JPPFJob>();
  /**
   * The queue of submissions pending execution.
   */
  protected ConcurrentLinkedQueue<JPPFJob> broadcastJobsQueue = new ConcurrentLinkedQueue<JPPFJob>();
  /**
   * The JPPF client that manages connections to the JPPF drivers.
   */
  private final AbstractGenericClient client;
  /**
   * The load balancer for local versus remote execution.
   */
  protected final LoadBalancer loadBalancer;
  /**
   * Keeps a list of the valid connections not currently executing tasks.
   */
  protected final Vector<JPPFClientConnection> availableConnections = new Vector<JPPFClientConnection>();

  /**
   * Initialize this submission worker with the specified JPPF client.
   * @param client the JPPF client that manages connections to the JPPF drivers.
   */
  protected AbstractSubmissionManager(final AbstractGenericClient client)
  {
    if (client == null) throw new IllegalArgumentException("client is null");

    this.client = client;
    TypedProperties config = client.getConfig();
    this.loadBalancer = new LoadBalancer(config);
    this.maxWaitMillis = config.getLong("jppf.submission.manager.maxwait.millis", 0L);
    this.maxWaitNanos = config.getInt("jppf.submission.manager.maxwait.nanos", 100000);
  }

  /**
   * Run the loop of this submission manager, watching for the queue and starting a job
   * when the queue has one and a connection is available.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    try
    {
      while (!isStopped())
      {
        Pair<Boolean, Boolean> execFlags = null;
        while (((execQueue.isEmpty() && broadcastJobsQueue.isEmpty()) || !(execFlags = handleAvailableConnection()).first()) && !isStopped())
        {
          goToSleep(maxWaitMillis, maxWaitNanos);
        }
        if (isStopped()) break;
        synchronized(this)
        {
          /*
          if (debugEnabled) log.debug("execFlags.first=" + execFlags.first() + ", execFlags.second=" + execFlags.second() +
            ", execQueue.isEmpty()=" + execQueue.isEmpty() + ", broadcastJobsQueue.isEmpty()=" + broadcastJobsQueue.isEmpty());
           */
          JPPFJob job = null;
          AbstractJPPFClientConnection c = (AbstractJPPFClientConnection) client.getClientConnection(true);
          if ((c != null) && !broadcastJobsQueue.isEmpty())
          {
            job = broadcastJobsQueue.poll();
            if (debugEnabled) log.debug("remote connection available for broadcast job " + job + ", submitting this job");
            if (execFlags.second()) getLoadBalancer().setLocallyExecuting(false);
          }
          else
          {
            job = execQueue.poll();
            if (job == null) continue;
            if ((c == null) && job.getSLA().isBroadcastJob())
            {
              if (execFlags.second()) getLoadBalancer().setLocallyExecuting(false);
              if (debugEnabled) log.debug("no remote connection available for broadcast job " + job + " setting this job on hold");
              broadcastJobsQueue.offer(job);
              continue;
            }
          }
          if (debugEnabled) log.debug("submitting job " + job.getName());
          if (c != null) c.getTaskServerConnection().setStatus(JPPFClientConnectionStatus.EXECUTING);
          JobSubmission submission = createSubmission(job, c, job.getSLA().isBroadcastJob() ? false : execFlags.second());
          client.getExecutor().submit(submission);
        }
      }
    }
    finally
    {
      if (loadBalancer != null) loadBalancer.stop();
    }
  }

  /**
   * Create a job submission for this submission manager.
   * @param job the job to submit.
   * @param c a connection tyo the server, may be null.
   * @param locallyExecuting whether the job will be executed locally, even partially.
   * @return a new {@link JobSubmission} instance.
   */
  protected abstract JobSubmission createSubmission(final JPPFJob job, final AbstractJPPFClientConnection c, final boolean locallyExecuting);

  /**
   * Get the load balancer for local versus remote execution.
   * @return a <code>LoadBalancer</code> instance.
   */
  public LoadBalancer getLoadBalancer()
  {
    return loadBalancer;
  }

  /**
   * Determine whether local execution is enabled on this client.
   * @return <code>true</code> if local execution is enabled, <code>false</code> otherwise.
   */
  @Override
  public boolean isLocalExecutionEnabled()
  {
    return loadBalancer != null && loadBalancer.isLocalEnabled();
  }

  /**
   * Specify whether local execution is enabled on this client.
   * @param localExecutionEnabled <code>true</code> to enable local execution, <code>false</code> otherwise
   */
  @Override
  public void setLocalExecutionEnabled(final boolean localExecutionEnabled)
  {
    if (loadBalancer != null) loadBalancer.setLocalEnabled(localExecutionEnabled);
  }

  /**
   * Determine whether there is a client connection available for execution.
   * @return true if at least one connection is available, false otherwise.
   */
  @Override
  public boolean hasAvailableConnection()
  {
    if (traceEnabled)
    {
      StringBuilder sb = new StringBuilder();
      sb.append("available connections: ").append(getAvailableConnections().size()).append(", ");
      sb.append("local execution enabled: ").append(loadBalancer.isLocalEnabled()).append(", ");
      sb.append("locally executing: ").append(loadBalancer.isLocallyExecuting());
      log.trace(sb.toString());
    }
    return (!availableConnections.isEmpty() || (loadBalancer.isLocalEnabled() && !loadBalancer.isLocallyExecuting()));
  }

  /**
   * Determine whether there is a client connection available for execution.
   * @return true if at least one connection is available, false otherwise.
   */
  protected Pair<Boolean, Boolean> handleAvailableConnection()
  {
    synchronized(loadBalancer.getAvailableConnectionLock())
    {
      boolean b1 = hasAvailableConnection();
      boolean b2 = false;
      if (b1 && (loadBalancer.isLocalEnabled() && !loadBalancer.isLocallyExecuting()))
      {
        loadBalancer.setLocallyExecuting(true);
        b2 = true;
      }
      return new Pair<Boolean, Boolean>(b1, b2);
    }
  }

  /**
   * Perform the execution.
   * @param jobSubmission the job submission to execute.
   * @param connection the client connection for sending remote execution requests.
   * @param localJob determines whether the job will be executed locally, at least partially.
   * @throws Exception if an error is raised during execution.
   */
  public void execute(final JobSubmission jobSubmission, final AbstractJPPFClientConnection connection, final boolean localJob) throws Exception
  {
    loadBalancer.execute(jobSubmission, connection, localJob);
  }

  @Override
  public void statusChanged(final ClientConnectionStatusEvent event)
  {
    JPPFClientConnection c = (JPPFClientConnection) event.getClientConnectionStatusHandler();
    JPPFClientConnectionStatus status = c.getStatus();
    switch(status)
    {
      case ACTIVE:
        availableConnections.add(c);
        break;
      default:
        availableConnections.remove(c);
        break;
    }
    if (debugEnabled) log.debug("connection=" + c + ", availableConnections=" + availableConnections);
  }

  /**
   * Get the list of available connections.
   * @return a vector of connections instances.
   */
  @Override
  public Vector<JPPFClientConnection> getAvailableConnections()
  {
    return availableConnections;
  }

  @Override
  public ClientConnectionStatusListener getClientConnectionStatusListener()
  {
    return this;
  }

  @Override
  public void close()
  {
    setStopped(true);
    wakeUp();
  }
}
