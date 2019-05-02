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

package org.jppf.node.protocol;

/**
 * This interface represents the Service Level Agreement between a JPPF job and a JPPF client.
 * It determines the state, conditions and order in which a job will be executed.
 * @author Laurent Cohen
 */
public class JobClientSLA extends JobCommonSLA<JobClientSLA> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Whether the traversal of the graph of tasks, if any, should occur on the client side.
   */
  private boolean graphTraversalInClient;

  /**
   * Default constructor.
   * @exclude
   */
  public JobClientSLA() {
    maxChannels = 1;
  }

  /**
   * Get the maximum number of channels, including the local execution channel if it is enabled, this job can be sent on.
   * @return the number of channels as an int value.
   */
  public int getMaxChannels() {
    return maxChannels;
  }

  /**
   * Set the maximum number of channels, including the local execution channel if it is enabled, this job can be sent on.
   * @param maxChannels the number of channels as an int value. A value <= 0 means no limit on the number of channels.
   * @return this SLA, for method call chaining.
   */
  public JobClientSLA setMaxChannels(final int maxChannels) {
    this.maxChannels = maxChannels > 0 ? maxChannels : Integer.MAX_VALUE;
    return this;
  }

  /**
   * Create a copy of this job SLA.
   * @return a {@link JobClientSLA} instance.
   * @exclude
   */
  public JobClientSLA copy() {
    final JobClientSLA sla = new JobClientSLA();
    copyTo(sla);
    sla.setMaxChannels(maxChannels);
    return sla;
  }

  /**
   * Determine whether the traversal of the graph of tasks, if any, should occur on the client side.
   * By default, as long as {@link #setGraphTraversalInClient(boolean) setGraphTraversalInClient(true)} hasn't been called, this method will return {@code false}.
   * @return {@code true} if the graph traversal is performed on the client side, {@code false} otherwise.
   * @since 6.2
   */
  public boolean isGraphTraversalInClient() {
    return graphTraversalInClient;
  }

  /**
   * Specify whether the traversal of the graph of tasks, if any, should occur on the client side.
   * @param graphTraversalInClient {@code true} if the graph traversal is to be performed on the client side, {@code false} otherwise.
   * @since 6.2
   */
  public void setGraphTraversalInClient(final boolean graphTraversalInClient) {
    this.graphTraversalInClient = graphTraversalInClient;
  }
}
