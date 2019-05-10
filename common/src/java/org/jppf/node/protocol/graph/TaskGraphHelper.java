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

import org.jppf.node.protocol.Task;

/**
 * Utility and factory methods to build and manipulate graphs of tasks.
 * @author Laurent Cohen
 * @exclude
 */
public class TaskGraphHelper {
  /**
   * Build a task graph from the specified collection of tasks.
   * @param tasks the task to covert to nodes int he graph.
   * @return a newly created {@link TaskGraph}.
   */
  public static TaskGraph graphOf(final Collection<Task<?>> tasks) {
    final Map<Integer, TaskGraph.Node> nodesMap = new HashMap<>();
    for (final Task<?> task: tasks) addNode(nodesMap, task);
    return new TaskGraph(nodesMap);
  }

  /**
   * Add the specified task to the specified graph.
   * @param nodesMap a maaping of graph nodes ot their position.
   * @param task the task to add.
   * @return  the node corrresponding to the task.
   */
  private static TaskGraph.Node addNode(final Map<Integer, TaskGraph.Node> nodesMap, final Task<?> task) {
    final int pos = task.getPosition();
    TaskGraph.Node node = nodesMap.get(pos);
    if (node == null) {
      node = new TaskGraph.Node(pos, false, null);
      nodesMap.put(pos, node);
      if (task instanceof TaskNode) {
        final TaskNode<?> taskNode = (TaskNode<?>) task;
        if (taskNode.hasDependency()) {
          for (final TaskNode<?> dep: taskNode.getDependencies()) {
            final TaskGraph.Node depNode = addNode(nodesMap, dep);
            node.getDependencies().add(depNode);
          }
        }
      }
    }
    return node;
  }
}
