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

package org.jppf.server.queue;

import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.server.protocol.*;
import org.slf4j.*;

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
   * Proxy to the job management MBean.
   */
  private static DriverJobManagementMBean jobManagementMBean = createJobManagementProxy();
  /**
   * The bundle wrapper encapsulating the job.
   */
  private ServerJob bundleWrapper = null;

  /**
   * Initialize this action with the specified bundle wrapper.
   * @param bundleWrapper the bundle wrapper encapsulating the job.
   */
  public JobExpirationAction(final ServerJob bundleWrapper)
  {
    this.bundleWrapper = bundleWrapper;
  }

  /**
   * Execute this action.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
    String jobId = bundle.getName();
    try
    {
      if (debugEnabled) log.debug("job '" + jobId + "' is expiring");
      bundle.setParameter(BundleParameter.JOB_EXPIRED, true);
      if (bundle.getTaskCount() > 0)
      {
        if (bundle.getCompletionListener() != null) bundle.getCompletionListener().taskCompleted(bundleWrapper);
      }
      String jobUuid = bundleWrapper.getJob().getJobUuid();
      jobManagementMBean.cancelJob(jobUuid);
    }
    catch (Exception e)
    {
      log.error("Error while cancelling job id = " + jobId, e);
    }
  }

  /**
   * Create a proxy to the local job management mbean.
   * @return a {@link DriverJobManagementMBean} instance.
   */
  private static DriverJobManagementMBean createJobManagementProxy()
  {
    try
    {
      JMXDriverConnectionWrapper jmxWrapper = new JMXDriverConnectionWrapper();
      jmxWrapper.connect();
      return jmxWrapper.getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
    }
    catch(Exception e)
    {
      log.error("Could not initialize a proxy to the job management MBean", e);
    }
    return null;
  }
}