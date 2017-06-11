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

package org.jppf.client;

import java.util.*;

import org.jppf.client.event.*;
import org.jppf.load.balancer.LoadBalancingInformation;

/**
 * Interface for an asynchronous job submission manager.
 * @author Laurent Cohen
 * @exclude
 */
public interface JobManager {
  /**
   * Add a job to the execution queue.
   * @param job encapsulation of the execution data.
   * @return the unique id of the job.
   */
  String submitJob(JPPFJob job);

  /**
   * Add a task job to the execution queue.
   * @param job encapsulation of the execution data.
   * @param listener an optional listener to receive job status change notifications, may be null.
   * @return the unique id of the job.
   */
  String submitJob(JPPFJob job, JobStatusListener listener);

  /**
   * Add an existing job back into the execution queue.
   * @param job encapsulation of the execution data.
   * @return the unique id of the job.
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

  /**
   * Get a listener to the status of the managed connections.
   * @return a {@link ClientConnectionStatusListener} instance.
   */
  ClientConnectionStatusListener getClientConnectionStatusListener();

  /**
   * Close this job manager and all the resources it uses.
   */
  void close();

  /**
   * Reset this job manager.
   */
  void reset();

  /**
   * Cancel the job with the specified id.
   * @param jobId the id of the job to cancel.
   * @return a <code>true</code> when cancel was successful <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  boolean cancelJob(String jobId) throws Exception;

  /**
   * Get the current load-balancer settings.
   * @return a {@link LoadBalancingInformation} instance, which encapsulates a load-balancing alfgorithm name, along with its parameters.
   */
  public LoadBalancingInformation getLoadBalancerSettings();

  /**
   * Change the load balancer settings for the client.
   * @param algorithm the name of load-balancing alogrithm to use.
   * @param parameters the algorithm's parameters, if any. The parmeter names are assumed no to be prefixed.
   * @throws Exception if any error occurs or if the algorithm name is {@code null} or not known.
   */
  public void setLoadBalancerSettings(final String algorithm, final Properties parameters) throws Exception;
}
