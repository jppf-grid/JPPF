/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
 * @author Laurent Cohen
 */
public interface JobCommonSLA extends Serializable
{
  /**
   * Get the tasks execution policy.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  ExecutionPolicy getExecutionPolicy();

  /**
   * Set the tasks execution policy.
   * @param executionPolicy an <code>ExecutionPolicy</code> instance.
   */
  void setExecutionPolicy(ExecutionPolicy executionPolicy);

  /**
   * Get the job schedule.
   * @return a <code>JPPFSchedule</code> instance.
   */
  JPPFSchedule getJobSchedule();

  /**
   * Set the job schedule.
   * @param jobSchedule a <code>JPPFSchedule</code> instance.
   */
  void setJobSchedule(JPPFSchedule jobSchedule);

  /**
   * Get the job expiration schedule configuration.
   * @return a {@link JPPFSchedule} instance.
   */
  JPPFSchedule getJobExpirationSchedule();

  /**
   * Set the job expiration schedule configuration.
   * @param jobExpirationSchedule a {@link JPPFSchedule} instance.
   */
  void setJobExpirationSchedule(JPPFSchedule jobExpirationSchedule);
}
