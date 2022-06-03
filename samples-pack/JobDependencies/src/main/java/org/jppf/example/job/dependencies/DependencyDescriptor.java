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

package org.jppf.example.job.dependencies;

import java.io.Serializable;

/**
 * Instances of this class represent the definition of a job and its dependencies
 * and are intented to be sent with the job metadata.
 * @author Laurent Cohen
 */
public class DependencyDescriptor implements Serializable {
  /**
   * The application-defined id assigned to a job.
   */
  private final String id;
  /**
   * The application-defined ids of the job's dependencies.
   */
  private final String[] dependencies;
  /**
   * Whether the job is a dependency graph root.
   */
  private final boolean graphRoot;

  /**
   * Initialize this dependencies specification with the specified parameters.
   * @param id the application-defined id assigned to a job.
   * @param dependencies the application-defined ids of the job's dependencies.
   * @param graphRoot whether the job is a dependency graph root.
   */
  public DependencyDescriptor(final String id, final String[] dependencies, final boolean graphRoot) {
    this.id = id;
    this.dependencies = dependencies;
    this.graphRoot = graphRoot;
  }

  /**
   * Get the application-defined id assigned to a job.
   * @return the id string.
   */
  public String getId() {
    return id;
  }

  /**
   * Get the application-defined ids of the job's dependencies.
   * @return an array of string ids.
   */
  public String[] getDependencies() {
    return dependencies;
  }

  /**
   * Determine whether the job is a dependency graph root.and should be removed from the graph upon completetion.
   * @return {@code true} if the job is a dependency graph root, {@code false} otherwise.
   */
  public boolean isGraphRoot() {
    return graphRoot;
  }
}
