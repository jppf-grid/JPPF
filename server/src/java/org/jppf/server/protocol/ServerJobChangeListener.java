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

package org.jppf.server.protocol;

import org.jppf.execute.ExecutorChannel;
import org.jppf.server.submission.SubmissionStatus;

/**
 * Listener interface for job events.
 * @author Martin JANDA
 * @exclude
 */
public interface ServerJobChangeListener
{
  /**
   * Called when a job is added to the server queue.
   * @param source the updated job.
   */
  void jobUpdated(final AbstractServerJob source);
  /**
   * Called when job submission status is changed.
   * @param source the updated job.
   * @param oldValue value before change.
   * @param newValue value after change.
   */
  void jobStatusChanged(final AbstractServerJob source, final SubmissionStatus oldValue, final SubmissionStatus newValue);
  /**
   * Called when all or part of a job is dispatched to a node.
   * @param source the dispatched job.
   * @param channel the node to which the job is dispatched.
   * @param bundleNode the bundle for job event.
   */
  void jobDispatched(final AbstractServerJob source, final ExecutorChannel channel, final ServerTaskBundleNode bundleNode);
  /**
   * Called when all or part of a job has returned from a node.
   * @param source the returned job.
   * @param channel the node to which the job is dispatched.
   * @param bundleNode the bundle for job event.
   */
  void jobReturned(final AbstractServerJob source, final ExecutorChannel channel, final ServerTaskBundleNode bundleNode);
}
