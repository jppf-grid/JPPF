/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import org.jppf.server.protocol.ServerJob;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Action triggered when a job reaches its scheduled execution date.
 */
class JobScheduleAction implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JobScheduleAction.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The bundle wrapper encapsulating the job.
   */
  private final ServerJob bundleWrapper;

  /**
   * Initialize this action with the specified bundle wrapper.
   * @param bundleWrapper the bundle wrapper encapsulating the job.
   */
  public JobScheduleAction(final ServerJob bundleWrapper) {
    if (bundleWrapper == null) throw new IllegalArgumentException("bundleWrapper is null");
    this.bundleWrapper = bundleWrapper;
  }

  @Override
  public void run() {
    synchronized (bundleWrapper) {
      if (debugEnabled) {
        String jobId = bundleWrapper.getName();
        log.debug("job '" + jobId + "' is resuming");
      }
      bundleWrapper.setPending(false);
    }
  }
}
