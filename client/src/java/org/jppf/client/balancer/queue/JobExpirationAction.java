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

package org.jppf.client.balancer.queue;

import org.jppf.client.balancer.ClientJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action triggered when a job reaches its scheduled execution date.
 */
class JobExpirationAction implements Runnable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JobExpirationAction.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The bundle wrapper encapsulating the job.
   */
  private final ClientJob bundleWrapper;

  /**
   * Initialize this action with the specified bundle wrapper.
   * @param bundleWrapper the bundle wrapper encapsulating the job.
   */
  public JobExpirationAction(final ClientJob bundleWrapper)
  {
    if (bundleWrapper == null) throw new IllegalArgumentException("bundleWrapper is null");

    this.bundleWrapper = bundleWrapper;
  }

  /**
   * Execute this action.
   * @see Runnable#run()
   */
  @Override
  public void run()
  {
    String jobId = bundleWrapper.getName();
    try
    {
      if (debugEnabled) log.debug("job '" + jobId + "' is expiring");
      bundleWrapper.jobExpired();
    }
    catch (Exception e)
    {
      log.error("Error while cancelling job id = " + jobId, e);
    }
  }
}
