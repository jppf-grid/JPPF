/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
import org.slf4j.*;

/**
 * @author Martin JANDA
 */
class RemoveBundleAction implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RemoveBundleAction.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Reference to the job queue.
   */
  private final JPPFPriorityQueue queue;
  /**
   * Server job to remove from job map.
   */
  private final ServerJob serverJob;

  /**
   * Initialize this queue action with server job.
   * @param queue the reference queue to use.
   * @param serverJob the reference to job to use.
   */
  public RemoveBundleAction(final JPPFPriorityQueue queue, final ServerJob serverJob) {
    if (queue == null) throw new IllegalArgumentException("queue is null");
    if (serverJob == null) throw new IllegalArgumentException("serverJob is null");
    if (debugEnabled) log.debug("new RemoveBundleAction for job {}", serverJob);
    this.queue = queue;
    this.serverJob = serverJob;
  }

  @Override
  public void run() {
    if (debugEnabled) log.debug("removing job {}", serverJob);
    queue.removeBundle(serverJob, true);
    if (serverJob.getSLA().getPersistenceSpec().isDeleteOnCompletion()) queue.getPersistenceHandler().deleteJob(serverJob);
    serverJob.getRemovalCondition().signalAll();
  }
}
