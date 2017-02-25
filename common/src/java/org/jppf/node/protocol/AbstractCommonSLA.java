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

import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.scheduling.JPPFSchedule;

/**
 * This class represents the Service Level Agreement Between a JPPF job and a server.
 * It determines the state, conditions and order in which a job will be executed.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractCommonSLA implements JobCommonSLA {
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
   */
  public AbstractCommonSLA() {
  }

  @Override
  public ExecutionPolicy getExecutionPolicy() {
    return executionPolicy;
  }

  @Override
  public JobCommonSLA setExecutionPolicy(final ExecutionPolicy executionPolicy) {
    this.executionPolicy = executionPolicy;
    return this;
  }

  @Override
  public JPPFSchedule getJobSchedule() {
    return jobSchedule;
  }

  @Override
  public JobCommonSLA setJobSchedule(final JPPFSchedule jobSchedule) {
    this.jobSchedule = jobSchedule;
    return this;
  }

  @Override
  public JPPFSchedule getJobExpirationSchedule() {
    return jobExpirationSchedule;
  }

  @Override
  public JobCommonSLA setJobExpirationSchedule(final JPPFSchedule jobExpirationSchedule) {
    this.jobExpirationSchedule = jobExpirationSchedule;
    return this;
  }

  /**
   * Create a copy of this job SLA.
   * @param sla a {@link AbstractCommonSLA} into which to copy the attributes of this instance.
   * @return a {@link AbstractCommonSLA} instance.
   */
  protected AbstractCommonSLA copyTo(final AbstractCommonSLA sla) {
    sla.setExecutionPolicy(executionPolicy);
    sla.setJobExpirationSchedule(jobExpirationSchedule);
    sla.setJobSchedule(jobSchedule);
    //sla.setPriority(priority);
    return sla;
  }
}
