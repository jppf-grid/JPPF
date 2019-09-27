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

import java.io.Serializable;
import java.util.*;

/**
 * Instances of this class represent the definition of a job and its dependencies.
 * @author Laurent Cohen
 * @since 6.2
 */
public class JobDependencySpec implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The application-defined id assigned to a job.
   */
  private String id;
  /**
   * The application-defined ids of the job's dependencies.
   */
  private final List<String> dependencies = new ArrayList<>();
  /**
   * Whether the job and its dependencies should be removed from the graph upon completetion.
   */
  private boolean removeUponCompletion;
  /**
   * Whether cancelling the job triggers the cancellation of all the jobs that depend on it. 
   */
  private boolean cascadeCancellation = true;

  /**
   * Initialize this dependencies specification.
   */
  public JobDependencySpec() {
  }

  /**
   * Get the application-defined id assigned to a job.
   * @return the id string.
   */
  public String getId() {
    return id;
  }

  /**
   * Set the dependency id for the current job.
   * @param id the id of the dependency to add.
   * @return this {@code JobDependencySpec}, for method call chaining.
   */
  public JobDependencySpec setId(final String id) {
    this.id = id;
    return this;
  }

  /**
   * Get the application-defined ids of the job's dependencies.
   * @return an array of string ids.
   */
  public List<String> getDependencies() {
    return dependencies;
  }

  /**
   * Add the specified dependencies ot the job. Null dependencies in the input array are ignored.
   * @param dependencies the dependencie to add.
   * @return this {@code JobDependencySpec}, for method call chaining.
   */
  public JobDependencySpec addDependencies(final String...dependencies) {
    if ((dependencies != null) && (dependencies.length > 0)) {
      for (final String dependency: dependencies) {
        if (dependency != null) this.dependencies.add(dependency);
      }
    }
    return this;
  }

  /**
   * Add the specified dependencies ot the job. Null dependencies in the input collection are ignored.
   * @param dependencies the dependencie to add.
   * @return this {@code JobDependencySpec}, for method call chaining.
   */
  public JobDependencySpec addDependencies(final Collection<String> dependencies) {
    if ((dependencies != null) && !dependencies.isEmpty()) {
      for (final String dependency: dependencies) {
        if (dependency != null) this.dependencies.add(dependency);
      }
    }
    return this;
  }

  /**
   * Determine whether the job and its dependencies should be removed from the graph upon completetion.
   * This method returns {@code false} by default, unless {@link #setRemoveUponCompletion(boolean) setRemoveUponCompletion(true)} was called before.
   * @return {@code true} if the job should be removed from the graph after completion, {@code false} otherwise.
   */
  public boolean isRemoveUponCompletion() {
    return removeUponCompletion;
  }

  /**
   * Specify whether the job and its dependencies should be removed from the graph upon completetion.
   * For all practical purposes, this method should only be called for roots in a job dependency graph, that is, for jobs upon which no other job depends.
   * @param removeUponCompletion {@code true} if the job should be removed from the graph after completion, {@code false} otherwise.
   * @return this {@code JobDependencySpec}, for method call chaining.
   */
  public JobDependencySpec setRemoveUponCompletion(final boolean removeUponCompletion) {
    this.removeUponCompletion = removeUponCompletion;
    return this;
  }

  /**
   * Determine whether the job has at least one dependency.
   * @return {@code true} if the job has one or more dependencies, {@code false} otherwise.
   */
  public boolean hasDependency() {
    return !dependencies.isEmpty();
  }

  /**
   * Determine whether cancelling the job triggers the cancellation of all the jobs that depend on it.
   * This method returns {@code true} by default, unless {@link #setCascadeCancellation(boolean) setCascadeCancellation(false)} was called before.
   * @return {@code true} if the cancellation of the job is propagated to the jobs that depend on it, {@code false} otherwise. 
   */
  public boolean isCascadeCancellation() {
    return cascadeCancellation;
  }

  /**
   * Specify whether cancelling the job triggers the cancellation of all the jobs that depend on it. 
   * @param cascadeCancellation {@code true} to propagate the cancellation of the job to the jobs that depend on it, {@code false} otherwise.
   * @return this {@code JobDependencySpec}, for method call chaining.
   */
  public JobDependencySpec setCascadeCancellation(final boolean cascadeCancellation) {
    this.cascadeCancellation = cascadeCancellation;
    return this;
  }
}
