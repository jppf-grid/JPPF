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

package org.jppf.node.protocol.graph;

import java.util.*;

import org.jppf.node.protocol.*;

/**
 * Interface for tasks that are part of a graph of tasks within a job.
 * @param <T> the type of results produced by the task.
 * @author Laurent Cohen
 */
public interface TaskNode<T> extends Task<T>, PositionalTask {
  /**
   * Add a set of dependencies to this task.
   * @param tasks a collection of tasks that this task depends on.
   * @return this task, for method call chaining.
   * @throws JPPFDependencyCycleException if a cycle is detected in the dependencies of this task.
   */
  TaskNode<T> dependsOn(final Collection<TaskNode<?>> tasks) throws JPPFDependencyCycleException;

  /**
   * Add a set of dependencies to this task.
   * This method is equivalent to calling {@link #dependsOn(Collection) dependsOn}{@code (Arrays.asList(tasks))}.
   * @param tasks an array of tasks that this task depends on.
   * @return this task, for method call chaining.
   * @throws JPPFDependencyCycleException if a cycle is detected in the dependencies of this task.
   */
  default TaskNode<T> dependsOn(final TaskNode<?>...tasks) throws JPPFDependencyCycleException {
    if ((tasks != null) && (tasks.length > 0)) return dependsOn(Arrays.asList(tasks));
    return this;
  }

  /**
   * Get the dependencies of this task, if any.
   * @return the dependencies as a collection of {@code TaskGraph} instances, or {@code null} if there is no dependency.
   */
  Collection<TaskNode<?>> getDependencies();

  /**
   * Get the tasks that depend on this task, if any.
   * @return the dependants as a collection of {@code Task} instances, or {@code null} if there is no dependant.
   */
  Collection<TaskNode<?>> getDependants();

  /**
   * Determine whether this tasks has at least one dependency.
   * @return {@code true} if there is at least one dependency, {@code false} otherwise.
   */
  default boolean hasDependency() {
    final Collection<TaskNode<?>> dependencies = getDependencies();
    return (dependencies != null) && !dependencies.isEmpty();
  }

  /**
   * Determine whether this tasks has at least one dependant, that is, another task that depends on it.
   * @return {@code true} if there is at least one dependant, {@code false} otherwise.
   */
  default boolean hasDependant() {
    final Collection<TaskNode<?>> dependants = getDependants();
    return (dependants != null) && !dependants.isEmpty();
  }
}
