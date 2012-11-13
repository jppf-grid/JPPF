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


/**
 * Wrapper for submitting a job.
 * @author Laurent Cohen
 * @exclude
 * @param <S> class type for submission manager implementation
 */
public abstract class AbstractJobSubmission<S extends SubmissionManager> implements JobSubmission
{
  /**
   * The job to execute.
   */
  protected JPPFJob job;
  /**
   * Flag indicating whether the job will be executed locally, at least partially.
   */
  protected boolean locallyExecuting = false;
  /**
   * The connection to execute the job on.
   */
  protected AbstractJPPFClientConnection connection;
  /**
   * The submission manager.
   */
  protected S submissionManager;

  /**
   * Initialize this job submission.
   * @param job the submitted job.
   * @param connection the connection to execute the job on.
   * @param locallyExecuting determines whether the job will be executed locally, at least partially.
   * @param submissionManager the submission manager.
   */
  protected AbstractJobSubmission(final JPPFJob job, final AbstractJPPFClientConnection connection, final boolean locallyExecuting, final S submissionManager)
  {
    this.job = job;
    this.connection = connection;
    this.locallyExecuting = locallyExecuting;
    this.submissionManager = submissionManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getId()
  {
    return job.getUuid();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AbstractJPPFClientConnection getConnection()
  {
    return connection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLocallyExecuting()
  {
    return locallyExecuting;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JPPFJob getJob()
  {
    return job;
  }
}
