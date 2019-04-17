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

import java.io.Serializable;
import java.util.*;

import org.jppf.node.protocol.Task;

/**
 * A node in the graph of the tasks in a job which represents a task and its dependants.
 * @author Laurent Cohen
 * @exclude
 */
public class JobTaskNode implements Serializable {
  /**
   * The dependencies of this task, if any.
   */
  private final List<JobTaskNode> dependencies = new ArrayList<>();
  /**
   * The position of this task in the job.
   */
  private final int position;
  /**
   * Whether this task is done, that is, either completed or cancelled.
   */
  private boolean done;

  /**
   * Initialize this node with the specified task.
   * @param position the position of this task.
   * @param done whether this task is done.
   * @param dependencies the dependencies of this task, may be {@code null}.
   */
  public JobTaskNode(final int position, final boolean done, final List<JobTaskNode> dependencies) {
    this.position = position;
    this.done = done;
    if (dependencies != null) this.dependencies.addAll(dependencies);
  }

  /**
   * Initialize this node with the specified task.
   * @param task the task represented by this node.
   */
  public JobTaskNode(final Task<?> task) {
    this.position = task.getPosition();
    if (task instanceof TaskNode) {
      final TaskNode<?> taskNode = (TaskNode<?>) task;
      final Collection<TaskNode<?>> dependencies = taskNode.getDependencies();
      if (dependencies != null) {
        for (final TaskNode<?> dep: dependencies) this.dependencies.add(new JobTaskNode(dep));
      }
    }
  }

  /**
   * Add the specified dependency.
   * @param dependency the dependency to add.
   */
  public void addDependency(final JobTaskNode dependency) {
    dependencies.add(dependency);
  }

  /**
   * @return the tasks that depend on this task, if any.
   */
  public List<JobTaskNode> getDependencies() {
    return dependencies;
  }

  /**
   * @return the position of this task in the job.
   */
  public int getPosition() {
    return position;
  }

  /**
   * @return whether this taks is done.
   */
  public boolean isDone() {
    return done;
  }

  /**
   * Shether this task is done, that is, either completed or cancelled.
   * @param done the done state.
   */
  public void setDone(final boolean done) {
    this.done = done;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("position=").append(position)
      .append(", done=").append(done)
      .append(", dependencies=").append(dependencies.size())
      .append(']').toString();
  }

  @Override
  public int hashCode() {
    return position;
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof JobTaskNode)) return false;
    return position == ((JobTaskNode) obj).position;
  }
}
