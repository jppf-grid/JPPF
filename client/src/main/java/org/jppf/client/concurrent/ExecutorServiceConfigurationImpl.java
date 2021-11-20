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

package org.jppf.client.concurrent;

/**
 * Concrete implementation of the {@link ExecutorServiceConfiguration} interface.
 * @author Laurent Cohen
 * @exclude
 */
class ExecutorServiceConfigurationImpl implements ExecutorServiceConfiguration {
  /**
   * The configuration to use for the jobs submitted by the executor service.
   */
  private final JobConfiguration jobConfiguration;
  /**
   * The configuration to use for the tasks submitted by the executor service.
   */
  private final TaskConfiguration taskConfiguration;

  /**
   * Initialize this executor service configuration.
   */
  ExecutorServiceConfigurationImpl() {
    this.jobConfiguration = new JobConfigurationImpl();
    this.taskConfiguration = new TaskConfigurationImpl();
  }

  @Override
  public JobConfiguration getJobConfiguration() {
    return jobConfiguration;
  }

  @Override
  public TaskConfiguration getTaskConfiguration() {
    return taskConfiguration;
  }
}
