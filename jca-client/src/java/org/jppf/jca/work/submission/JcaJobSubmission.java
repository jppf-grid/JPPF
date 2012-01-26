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
package org.jppf.jca.work.submission;

import static org.jppf.client.submission.SubmissionStatus.*;

import org.jppf.client.*;
import org.jppf.client.submission.*;
import org.slf4j.*;

/**
 * Wrapper for submitting a job.
 */
public class JcaJobSubmission extends AbstractJobSubmission
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JcaJobSubmission.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this job submission.
   * @param job the submitted job.
   * @param connection the connection to execute the job on.
   * @param submissionManager the submission manager.
   * @param locallyExecuting determines whether the job will be executed locally, at least partially.
   */
  JcaJobSubmission(final JPPFJob job, final AbstractJPPFClientConnection connection, final boolean locallyExecuting, final SubmissionManager submissionManager)
  {
    super(job, connection, locallyExecuting, submissionManager);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
    JPPFResultCollector result = (JPPFResultCollector) job.getResultListener();
    try
    {
      result.setStatus(EXECUTING);
      submissionManager.getClient().getLoadBalancer().execute(this, connection, locallyExecuting);
      result.setStatus(COMPLETE);
    }
    catch (Exception e)
    {
      result.setStatus(FAILED);
      log.error(e.getMessage(), e);
      submissionManager.resubmitJob(job);
    }
    finally
    {
      if (connection != null) connection.setStatus(JPPFClientConnectionStatus.ACTIVE);
    }
  }
}
