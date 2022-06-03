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
import org.jppf.utils.SystemUtils;

/**
 * Instances of this class represent tasks that are part of a dependency graph of tasks within a job.<br>
 * Dependencies can be added using the {@link #dependsOn(Collection) dependsOn(Collection&lt;TaskNode&gt;)} and {@link TaskNode#dependsOn(TaskNode[]) dependsOn(TaskNode...)} methods.
 * <p>Dependency cycles are checked and detected every time one or more dependencies are added to this task via the aforementioned methods.
 * When a dependency cycle is detected, a {@link JPPFDependencyCycleException} is raised, with a description of the cycle.
 * <p>It is therefore guaranteed that tasks with dependencies form a <a href="https://en.wikipedia.org/wiki/Directed_acyclic_graph">directed acyclic graph</a> (DAG).
 * This ensures that the tasks can be executed in a <a href="https://en.wikipedia.org/wiki/Topological_sorting">topological order</a> where no task will be executed before its dependencies.
 * @author Laurent Cohen
 * @param <T> the type of results returned by this task.
 * @since 6.2
 */
public class AbstractTaskNode<T> extends AbstractTask<T> implements TaskNode<T> {
  /**
   * The dependenices of this task, if any.
   */
  private transient Set<TaskNode<?>> dependencies;
  /**
   * The tasks that depend on this task, if any.
   */
  transient Set<TaskNode<?>> dependants;

  @Override
  public TaskNode<T> dependsOn(final Collection<TaskNode<?>> tasks) throws JPPFDependencyCycleException {
    if ((tasks != null) && !tasks.isEmpty()) {
      if (dependencies == null) dependencies = new HashSet<>();
      for (final TaskNode<?> task: tasks) {
        if (task != null) {
          final LinkedList<TaskNode<?>> path = new LinkedList<>();
          checkDependencyCycle(task, path);
          dependencies.add(task);
          if (task instanceof AbstractTaskNode) ((AbstractTaskNode<?>) task).addDependant(this);
        }
      }
    }
    return this;
  }

  /**
   * Check whether the specified dependency introduces a cycle.
   * @param dependency the dependency to check.
   * @param path the dependency path that leads to the cycle.
   * @throws JPPFDependencyCycleException if a cycle is detected.
   */
  private void checkDependencyCycle(final TaskNode<?> dependency, final LinkedList<TaskNode<?>> path) throws JPPFDependencyCycleException {
     path.addLast(dependency);
     if (dependency == this) {
       final StringBuilder sb = new StringBuilder(dependencyString(this));
       for (final TaskNode<?> task: path) sb.append(" ==> ").append(dependencyString(task));
       throw new JPPFDependencyCycleException(sb.toString()); 
    } else {
      final Collection<TaskNode<?>> deps = dependency.getDependencies();
      if ((deps != null) && !deps.isEmpty()) {
        for (final TaskNode<?> dep: deps) checkDependencyCycle(dep, path);
      }
     }
     path.removeLast();
  }

  /**
   * Builds a string representation of a task, to use in a {@link JPPFDependencyCycleException} message.
   * @param task the task from which to build a string.
   * @return the create string.
   */
  private static String dependencyString(final TaskNode<?> task) {
    return String.format("%s (%s)", SystemUtils.getSystemIdentityName(task), task.toString());
  }

  @Override
  public Collection<TaskNode<?>> getDependencies() {
    return dependencies;
  }

  /**
   * Add a task that depends on this task to the list of dependant tasks. 
   * @param dependant the task to add.
   */
  private void addDependant(final TaskNode<?> dependant) {
    if (dependant == null) return;
    if (dependants == null) dependants = new HashSet<>();
    dependants.add(dependant);
  }

  @Override
  public Collection<TaskNode<?>> getDependants() {
    return dependants;
  }
}
