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

package org.jppf.client.balancer.queue;

import org.jppf.client.balancer.ClientJob;
import org.jppf.client.balancer.ClientTaskBundle;
import org.jppf.client.balancer.job.JPPFJobManager;
import org.jppf.server.protocol.BundleParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action triggered when a job reaches its scheduled execution date.
 */
class JobScheduleAction implements Runnable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JobScheduleAction.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The bundle wrapper encapsulating the job.
   */
  private final ClientJob bundleWrapper;
  /**
   * The job manager.
   */
  private final JPPFJobManager jobManager;
  /**
   * Initialize this action with the specified bundle wrapper.
   * @param bundleWrapper the bundle wrapper encapsulating the job.
   * @param jobManager - the job manager that submits the events.
   */
  public JobScheduleAction(final ClientJob bundleWrapper, final JPPFJobManager jobManager)
  {
    if(bundleWrapper == null) throw new IllegalArgumentException("bundleWrapper is null");
    if(jobManager == null) throw new IllegalArgumentException("jobManager is null");

    this.bundleWrapper = bundleWrapper;
    this.jobManager = jobManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
    synchronized(bundleWrapper)
    {
      ClientTaskBundle bundle = (ClientTaskBundle) bundleWrapper.getJob();
      if (debugEnabled)
      {
        String jobId = bundle.getName();
        log.debug("job '" + jobId + "' is resuming");
      }
      bundle.setParameter(BundleParameter.JOB_PENDING, false);
      jobManager.jobUpdated(bundleWrapper);
    }
  }
}
