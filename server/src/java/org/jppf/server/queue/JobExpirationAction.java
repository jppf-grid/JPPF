/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import org.jppf.server.protocol.*;
import org.slf4j.*;

/**
 * Action triggered when a job reaches its scheduled execution date.
 */
class JobExpirationAction implements Runnable {
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
  private final ServerJob serverJob;

  /**
   * Initialize this action with the specified bundle wrapper.
   * @param serverJob the bundle wrapper encapsulating the job.
   */
  public JobExpirationAction(final ServerJob serverJob) {
    if (serverJob == null) throw new IllegalArgumentException("bundleWrapper is null");
    this.serverJob = serverJob;
  }

  /**
   * Execute this action.
   * @see Runnable#run()
   */
  @Override
  public void run() {
    String jobId = serverJob.getName();
    try {
      if (debugEnabled) log.debug("job '" + jobId + "' is expiring");
      serverJob.jobExpired();
      serverJob.cancel(true);
    } catch (Exception e) {
      log.error("Error while cancelling job id = " + jobId, e);
    }
  }
}
