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

package org.jppf.node.protocol;

import java.io.Serializable;

import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.scheduling.JPPFSchedule;

/**
 * This interface represents the Service Level Agreement between a JPPF job and a server.
 * It determines the state, conditions and order in which a job will be executed.
 * @param <T> the type of SLA.
 * @author Laurent Cohen
 */
@SuppressWarnings("unchecked")
public class JobCommonSLA<T extends JobCommonSLA<?>> implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The tasks execution policy.
   */
  ExecutionPolicy executionPolicy = null;
  /**
   * The job start schedule configuration.
   */
  JPPFSchedule jobSchedule = null;
  /**
   * The job expiration schedule configuration.
   */
  JPPFSchedule jobExpirationSchedule = null;

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
   * @param executionPolicy an <code>ExecutionPolicy</code> instance.
   * @return this SLA, for mathod chaining.
   */
  public T setExecutionPolicy(final ExecutionPolicy executionPolicy) {
    this.executionPolicy = executionPolicy;
    return (T) this;
  }

  /**
   * Get the job schedule.
   * @return a <code>JPPFSchedule</code> instance.
   */
  public JPPFSchedule getJobSchedule() {
    return jobSchedule;
  }

  /**
   * Set the job schedule.
   * @param jobSchedule a <code>JPPFSchedule</code> instance.
   * @return this SLA, for mathod chaining.
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
   * @return this SLA, for mathod chaining.
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
    //sla.setPriority(priority);
    return sla;
  }
}
