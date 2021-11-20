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
 * Interface for tasks that are part of a dependency graph of tasks within a job.<br>
 * Dependencies can be added using the {@link #dependsOn(Collection) dependsOn(Collection&lt;TaskNode&gt;)} and {@link #dependsOn(TaskNode[]) dependsOn(TaskNode...)} methods.
 * <p>Dependency cycles are checked and detected every time one or more dependencies are added to this task via the aforementioned methods.
 * When a dependency cycle is detected, a {@link JPPFDependencyCycleException} is raised, with a description of the cycle.
 * <p>It is therefore guaranteed that tasks with dependencies form a <a href="https://en.wikipedia.org/wiki/Directed_acyclic_graph">directed acyclic graph</a> (DAG).
 * This ensures that the tasks can be executed in a <a href="https://en.wikipedia.org/wiki/Topological_sorting">topological order</a> where no task will be executed before its dependencies.
 * @param <T> the type of results produced by the task.
 * @author Laurent Cohen
 * @since 6.2
 */
public interface TaskNode<T> extends Task<T> {
  /**
   * Add a set of dependencies to this task.
   * @param tasks a collection of tasks that this task depends on.
   * @return this task, for method call chaining.
   * @throws JPPFDependencyCycleException if a cycle is detected in the graph of dependencies of this task.
   */
  TaskNode<T> dependsOn(final Collection<TaskNode<?>> tasks) throws JPPFDependencyCycleException;

  /**
   * Add a set of dependencies to this task.
   * This method is equivalent to calling {@link #dependsOn(Collection) dependsOn}{@code (Arrays.asList(tasks))}.
   * @param tasks an array of tasks that this task depends on.
   * @return this task, for method call chaining.
   * @throws JPPFDependencyCycleException if a cycle is detected in the graph of dependencies of this task.
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
