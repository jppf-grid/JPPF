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

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.slf4j.*;

/**
 * This class queues and manages the jobs submitted via a JPPF clint.
 * It relies on a queue where job are first added, then submitted when a driver connection becomes available.
 * It also provides methods to check the status of a submission and retrieve the results.
 * @author Laurent Cohen
 * @exclude
 */
public class SubmissionManagerImpl extends AbstractSubmissionManager
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SubmissionManagerImpl.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this submission manager with the specified JPPF client.
   * @param client the JPPF client that manages connections to the JPPF drivers.
   * JPPF submissions.
   */
  public SubmissionManagerImpl(final JPPFClient client)
  {
    super(client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String submitJob(final JPPFJob job)
  {
    if (debugEnabled) log.debug("adding new submission: jobId=" + job.getName());
    TaskResultListener trl = job.getResultListener();
    if (trl instanceof SubmissionStatusHandler) ((SubmissionStatusHandler) trl).setStatus(SubmissionStatus.PENDING);
    execQueue.offer(job);
    wakeUp();
    return job.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String submitJob(final JPPFJob job, final SubmissionStatusListener listener)
  {
    return submitJob(job);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String resubmitJob(final JPPFJob job)
  {
    if (debugEnabled) log.debug("resubmitting job with id=" + job.getName());
    TaskResultListener trl = job.getResultListener();
    if (trl instanceof SubmissionStatusHandler) ((SubmissionStatusHandler) trl).setStatus(SubmissionStatus.PENDING);
    execQueue.offer(job);
    wakeUp();
    return job.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected JobSubmission createSubmission(final JPPFJob job, final AbstractJPPFClientConnection c, final boolean locallyExecuting)
  {
    return new JobSubmissionImpl(job, c, this, locallyExecuting);
  }
}
