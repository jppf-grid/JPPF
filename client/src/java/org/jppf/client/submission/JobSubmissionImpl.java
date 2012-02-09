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

import java.io.NotSerializableException;

import org.jppf.client.*;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * This class encapsulates the results of a job submission.
 * @author Laurent Cohen
 */
public class JobSubmissionImpl extends AbstractJobSubmission
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JobSubmissionImpl.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this job submission.
   * @param job the submitted job.
   * @param connection the connection to execute the job on.
   * @param submissionManager the submission manager.
   * @param locallyExecuting determines whether the job will be executed locally, at least partially.
   */
  JobSubmissionImpl(final JPPFJob job, final AbstractJPPFClientConnection connection, final SubmissionManager submissionManager, final boolean locallyExecuting)
  {
    super(job, connection, locallyExecuting, submissionManager);
  }

  /**
   * This method executes until all partial results have been received.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    boolean error = false;
    boolean hasStatusHandler = false;
    SubmissionStatusHandler statusHandler = null;
    try
    {
      if (job.getResultListener() instanceof SubmissionStatusHandler)
      {
        hasStatusHandler = true;
        statusHandler = (SubmissionStatusHandler) job.getResultListener();
      }
      if (connection != null) connection.setCurrentJob(job);
      try
      {
        if (hasStatusHandler) statusHandler.setStatus(SubmissionStatus.EXECUTING);
        submissionManager.getClient().getLoadBalancer().execute(this, connection, locallyExecuting);
        if (hasStatusHandler) statusHandler.setStatus(SubmissionStatus.COMPLETE);
      }
      catch(Exception e)
      {
        if (hasStatusHandler) statusHandler.setStatus(SubmissionStatus.FAILED);
        if ((e instanceof NotSerializableException) || (e instanceof InterruptedException)) throw e;
        String src = (connection == null) ? "local execution" : connection.getName();
        log.error('[' + src + "] " + ExceptionUtils.getMessage(e), e);
        if (connection != null)
        {
          connection.setCurrentJob(null);
          connection.getTaskServerConnection().setStatus(JPPFClientConnectionStatus.DISCONNECTED);
        }
        submissionManager.resubmitJob(job);
        if (connection != null)
        {
          try
          {
            connection.getTaskServerConnection().init();
          }
          catch(Exception e2)
          {
            log.error(e2.getMessage(), e2);
          }
        }
      }
    }
    catch(Exception e)
    {
      error = true;
      log.error('['+connection.getName()+"] "+ ExceptionUtils.getMessage(e), e);
    }
    finally
    {
      //if (debugEnabled) log.debug("job id '" + job.getId()  + "' ended with error = " + error);
      if (!error && (connection != null)) connection.setCurrentJob(null);
    }
  }
}
