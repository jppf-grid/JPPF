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

package org.jppf.node.protocol;

import java.io.Serializable;

import org.jppf.node.policy.*;
import org.jppf.scheduling.JPPFSchedule;

/**
 * This interface represents the Service Level Agreement between a JPPF job and a server.
 * It determines the state, conditions and order in which a job will be executed.
 * @param <T> the type of SLA.
 * @author Laurent Cohen
 */
@SuppressWarnings("unchecked")
public class JobCommonSLA<T extends JobCommonSLA<T>> implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The maximum number of nodes this job can run on.
   */
  int maxChannels;
  /**
   * The tasks execution policy.
   */
  ExecutionPolicy executionPolicy;
  /**
   * The job start schedule configuration.
   */
  JPPFSchedule jobSchedule;
  /**
   * The job expiration schedule configuration.
   */
  JPPFSchedule jobExpirationSchedule;
  /**
   * The maximum number of tasks allowed in a dispatch of the job.
   */
  int maxDispatchSize = Integer.MAX_VALUE;
  /**
   * Whether to allow multiple concurrent dispatches of the job to the same channel.
   */
  boolean allowMultipleDispatchesToSameChannel = true;
  /**
   * The preference policy.
   */
  Preference preferencePolicy;

  /**
   * Default constructor.
   * @exclude
   */
  public JobCommonSLA() {
  }

  /**
   * Get the tasks execution policy.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  public ExecutionPolicy getExecutionPolicy() {
    return executionPolicy;
  }

  /**
   * Set the tasks execution policy.
   * @param executionPolicy an {@link ExecutionPolicy} instance.
   * @return this SLA, for method call chaining.
   */
  public T setExecutionPolicy(final ExecutionPolicy executionPolicy) {
    this.executionPolicy = executionPolicy;
    return (T) this;
  }

  /**
   * Get the job schedule.
   * @return a {@link JPPFSchedule} instance.
   */
  public JPPFSchedule getJobSchedule() {
    return jobSchedule;
  }

  /**
   * Set the job schedule.
   * @param jobSchedule a {@link JPPFSchedule} instance.
   * @return this SLA, for method call chaining.
   */
  public T setJobSchedule(final JPPFSchedule jobSchedule) {
    this.jobSchedule = jobSchedule;
    return (T) this;
  }

  /**
   * Get the job expiration schedule configuration.
   * @return a {@link JPPFSchedule} instance.
   */
  public JPPFSchedule getJobExpirationSchedule() {
    return jobExpirationSchedule;
  }

  /**
   * Set the job expiration schedule configuration.
   * @param jobExpirationSchedule a {@link JPPFSchedule} instance.
   * @return this SLA, for method call chaining.
   */
  public T setJobExpirationSchedule(final JPPFSchedule jobExpirationSchedule) {
    this.jobExpirationSchedule = jobExpirationSchedule;
    return (T) this;
  }

  /**
   * Create a copy of this job SLA.
   * @param sla a {@link JobCommonSLA} into which to copy the attributes of this instance.
   * @return a {@link JobCommonSLA} instance.
   * @exclude
   */
  protected T copyTo(final T sla) {
    sla.setExecutionPolicy(executionPolicy);
    sla.setJobExpirationSchedule(jobExpirationSchedule);
    sla.setJobSchedule(jobSchedule);
    return sla;
  }

  /**
   * Get the maximum number of tasks allowed in a dispatch of the job.
   * This value will override the one computed by the load-balancer when {@code maxDispatchSize < loadBalancerSize}.
   * <p>By default, when {@link #setMaxDispatchSize(int)} has not yet been called, this method will return {@link Integer#MAX_VALUE}.
   * @return the maximum number of of tasks allowed in a job dispatch.
   * @since 6.1
   */
  public int getMaxDispatchSize() {
    return maxDispatchSize;
  }

  /**
   * Set the maximum number of tasks allowed in a dispatch of the job.
   * @param maxDispatchSize the maximum dispatch size to set. Values less than or equal to 0 are ignored and have no effect.
   * @return this SLA, for method call chaining.
   * @since 6.1
   */
  public T setMaxDispatchSize(final int maxDispatchSize) {
    if (maxDispatchSize > 0) this.maxDispatchSize = maxDispatchSize;
    return (T) this;
  }

  /**
   * Determine whether to allow multiple concurrent dispatches of the job to the same driver (client-side SLA) or to the same node (server-side SLA).
   * @return {@code true} (the default) if multiple dispatches of the job can be sent to the same channel, {@code false} otherwise.
   * @since 6.1
   */
  public boolean isAllowMultipleDispatchesToSameChannel() {
    return allowMultipleDispatchesToSameChannel;
  }

  /**
   * Specifiy whether to allow multiple concurrent dispatches of the job to the same driver (client-side SLA) or to the same node (server-side SLA).
   * @param allowMultipleDispatchesToSameChannel {@code true} to allow multiple dispatches of the job to be sent to the same channel, {@code false} otherwise.
   * @return this SLA, for method call chaining.
   * @since 6.1
   */
  public T setAllowMultipleDispatchesToSameChannel(final boolean allowMultipleDispatchesToSameChannel) {
    this.allowMultipleDispatchesToSameChannel = allowMultipleDispatchesToSameChannel;
    return (T) this;
  }

  /**
   * Get the preference policy.for this job SLA.
   * @return a {@link Preference} policy instance, or {@code null} if it was not {@link #setPreferencePolicy(Preference) set}.
   */
  public Preference getPreferencePolicy() {
    return preferencePolicy;
  }

  /**
   * Set the preference policy for this job SLA.
   * @param preferencePolicy a {@link Preference} policy instance, which may be {@code null}
   * @return this SLA, for method call chaining.
   * @since 6.1
   */
  public T setPreferencePolicy(final Preference preferencePolicy) {
    this.preferencePolicy = preferencePolicy;
    return (T) this;
  }
}
