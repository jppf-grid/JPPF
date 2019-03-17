/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.server.nio.client;

import org.jppf.server.protocol.ServerTaskBundleClient;

/**
 * 
 * @author Laurent Cohen
 */
class JobEntry {
  /**
   * The task bundle to send or receive.
   */
  private final ServerTaskBundleClient clientBundle;
  /**
   * The number of tasks remaining to send.
   */
  int nbTasksToSend;
  /**
   * The job uuid.
   */
  final String jobUuid;

  /**
   * @param clientBundle the task bundle to send or receive.
   */
  JobEntry(final ServerTaskBundleClient clientBundle) {
    this.clientBundle = clientBundle;
    jobUuid = clientBundle.getUuid();
    nbTasksToSend = clientBundle.getPendingTasksCount();
  }

  /**
   * Get the number of tasks that remain to be sent to the client.
   * @return the number of tasks as an int.
   */
  public int getPendingTasksCount() {
    if (clientBundle == null) throw new IllegalStateException("initialBundleWrapper is null");
    return clientBundle.getPendingTasksCount();
  }

  /**
   * Get the job submitted by the client.
   * @return a <code>ServerTaskBundleClient</code> instance.
   */
  public ServerTaskBundleClient getBundle() {
    return clientBundle;
  }

  /**
   * Send the job ended notification.
   */
  void jobEnded() {
    final ServerTaskBundleClient bundle;
    if ((bundle = getBundle()) != null) {
      bundle.bundleEnded();
    }
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("jobUuid=").append(jobUuid)
      .append(", jobName=").append(clientBundle.getJob().getName())
      .append(", nbTasksToSend=").append(nbTasksToSend)
      .append(", bundleId=").append(clientBundle.getId())
      .append(']').toString();
  }
}
