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

package org.jppf.client.submission;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.jppf.client.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Abstract super class for J2SE and JCA submission managers.
 * @author Laurent Cohen
 */
public abstract class AbstractSubmissionManager extends ThreadSynchronization implements SubmissionManager
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
   * Maximum wait time in milliseconds in the the submission manager loop.
   */
  private static final long MAX_WAIT_MILLIS = JPPFConfiguration.getProperties().getLong("jppf.submission.manager.maxwait.millis", 0L);
  /**
   * Maximum wait time in milliseconds in the the submission manager loop.
   */
  private static final int MAX_WAIT_NANOS = JPPFConfiguration.getProperties().getInt("jppf.submission.manager.maxwait.nanos", 100000);
  /**
   * The queue of submissions pending execution.
   */
  protected ConcurrentLinkedQueue<JPPFJob> execQueue = new ConcurrentLinkedQueue<JPPFJob>();
  /**
   * The JPPF client that manages connections to the JPPF drivers.
   */
  protected AbstractGenericClient client = null;

  /**
   * Run the loop of this submission manager, watching for the queue and starting a job
   * when the queue has one and a connection is available.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    while (!isStopped())
    {
      Pair<Boolean, Boolean> execFlags = null;
      while ((execQueue.isEmpty() || !(execFlags = client.handleAvailableConnection()).first()) && !isStopped())
      {
        goToSleep(MAX_WAIT_MILLIS, MAX_WAIT_NANOS);
      }
      if (isStopped()) break;
      synchronized(this)
      {
        JPPFJob job = execQueue.peek();
        AbstractJPPFClientConnection c = (AbstractJPPFClientConnection) client.getClientConnection(true);
        if ((c == null) && job.getSLA().isBroadcastJob())
        {
          if (execFlags.second()) client.getLoadBalancer().setLocallyExecuting(false);
          continue;
        }
        job = execQueue.poll();
        if (debugEnabled) log.debug("submitting jobId=" + job.getName());
        if (c != null) c.getTaskServerConnection().setStatus(JPPFClientConnectionStatus.EXECUTING);
        JobSubmission submission = createSubmission(job, c, job.getSLA().isBroadcastJob() ? false : execFlags.second());
        client.getExecutor().submit(submission);
      }
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
}
