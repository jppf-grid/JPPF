/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.server.nio.client.async;

import org.jppf.server.protocol.ServerTaskBundleClient;

/**
 * 
 * @author Laurent Cohen
 */
class JobEntry {
  /**
   * The task bundle to send or receive.
   */
  ServerTaskBundleClient clientBundle;
  /**
   * List of completed bundles to send to the client.
   */
  //protected final LinkedList<ServerTaskBundleClient> completedBundles = new LinkedList<>();
  //protected final Queue<ServerTaskBundleClient> completedBundles = new ConcurrentLinkedQueue<>();
  /**
   * The job as initially submitted by the client.
   */
  ServerTaskBundleClient initialBundleWrapper;
  /**
   * The number of tasks remaining to send.
   */
  int nbTasksToSend = 0;
  /**
   * The job uuid.
   */
  String jobUuid;

  /**
   * 
   * @param clientBundle the task bundle to send or receive.
   */
  JobEntry(final ServerTaskBundleClient clientBundle) {
    this.clientBundle = clientBundle;
    jobUuid = clientBundle.getUuid();
    initialBundleWrapper = clientBundle;
    nbTasksToSend = initialBundleWrapper.getPendingTasksCount();
  }

  /**
   * Get the number of tasks that remain to be sent to the client.
   * @return the number of tasks as an int.
   */
  public int getPendingTasksCount() {
    if (initialBundleWrapper == null) throw new IllegalStateException("initialBundleWrapper is null");
    return initialBundleWrapper.getPendingTasksCount();
  }

  /**
   * Get the job as initially submitted by the client.
   * @return a <code>ServerTaskBundleClient</code> instance.
   */
  public ServerTaskBundleClient getInitialBundleWrapper() {
    return initialBundleWrapper;
  }

  /**
   * Get the next bundle in the queue.
   * @return A {@link ServerJob} instance, or null if the queue is empty.
  public ServerTaskBundleClient pollCompletedBundle() {
    return completedBundles.poll();
  }
   */

  /**
   * Determine whether list of completed bundles is empty.
   * @return whether list of <code>ServerJob</code> instances is empty.
  public boolean isCompletedBundlesEmpty() {
    return completedBundles.isEmpty();
  }
   */

  /**
   * @return the uuid of the current job, if any.
   */
  synchronized String getJobUuid() {
    return jobUuid;
  }

  /**
   * Send the job ended notification.
   */
  void jobEnded() {
    final ServerTaskBundleClient bundle;
    if ((bundle = getInitialBundleWrapper()) != null) {
      bundle.bundleEnded();
      initialBundleWrapper = null;
    }
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("jobUuid=").append(jobUuid)
      .append(", jobName=").append(clientBundle.getJob().getName())
      .append(", nbTasksToSend=").append(nbTasksToSend)
      //.append(", nb completedBundles=").append(completedBundles.size())
      .append(']').toString();
  }
}