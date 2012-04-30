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

import org.jppf.client.*;
import org.jppf.client.event.SubmissionStatusListener;

import java.util.Vector;

/**
 * Interface for an asynchronous job submission manager.
 * @author Laurent Cohen
 * @exclude
 */
public interface SubmissionManager extends Runnable
{
  /**
   * Add a task submission to the execution queue.
   * @param job encapsulation of the execution data.
   * @return the unique id of the submission.
   */
  String submitJob(JPPFJob job);

  /**
   * Add a task submission to the execution queue.
   * @param job encapsulation of the execution data.
   * @param listener an optional listener to receive submission status change notifications, may be null.
   * @return the unique id of the submission.
   */
  String submitJob(JPPFJob job, SubmissionStatusListener listener);

  /**
   * Add an existing submission back into the execution queue.
   * @param job encapsulation of the execution data.
   * @return the unique id of the submission.
   */
  String resubmitJob(JPPFJob job);

  /**
   * Determine whether there is a client connection available for execution.
   * @return true if at least one connection is available, false otherwise.
   */
  boolean hasAvailableConnection();

  /**
   * Determine whether local execution is enabled on this client.
   * @return <code>true</code> if local execution is enabled, <code>false</code> otherwise.
   */
  boolean isLocalExecutionEnabled();

  /**
   * Specify whether local execution is enabled on this client.
   * @param localExecutionEnabled <code>true</code> to enable local execution, <code>false</code> otherwise
   */
  void setLocalExecutionEnabled(final boolean localExecutionEnabled);

  /**
   * Get the list of available connections.
   * @return a vector of connections instances.
   */
  Vector<JPPFClientConnection> getAvailableConnections();
}
