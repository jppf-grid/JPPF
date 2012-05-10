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

package org.jppf.client.concurrent;

/**
 * Interface for the configuration properties of a {@link JPPFExecutorService}.
 * Instances of this interface hold the properties to associate to each job submitted by
 * the executor service, along with te properties to associate with each individual task.
 * This configuration interface represents a way to provide JPPF-specific properties without
 * explicitly breaking the semantics of the {@link java.util.concurrent.ExecutorService ExecutorService} interface.
 * @author Laurent Cohen
 */
public interface ExecutorServiceConfiguration
{
  /**
   * Get the configuration to use for the jobs submitted by the executor service.
   * @return a {@link JobConfiguration} instance.
   */
  JobConfiguration getJobConfiguration();
  /**
   * Get the configuration to use for the tasks submitted by the executor service.
   * @return a {@link TaskConfiguration} instance.
   */
  TaskConfiguration getTaskConfiguration();
}
