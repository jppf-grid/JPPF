/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.util.*;

/**
 * Instances of this class represent a node in the job dependencies graph.
 * They also maintain state information about the job and its dependencies.
 */
public class DependencyNode {
  /**
   * The id asociated with this node. This is the application-defined id for the corresponding job.
   */
  private final String id;
  /**
   * The JPPF uuid of the associated job.
   */
  private String jobUuid;
  /**
   * A mappings of this node's dependencies ids to the corresponding {@link DependencyNode} objects.
   * This is in effect a representation of the whole graph.
   */
  private final Map<String, DependencyNode> dependencies =  new HashMap<>();
  /**
   * The ids of the nodes that depend on this node.
   */
  private final Map<String, DependencyNode> dependedOn = new HashMap<>();
  /**
   * The ids of the nodes that depend on this node.
   */
  private final Set<String> pendingDependencies = new HashSet<>();
  /**
   * Whether the job represented by this node has completed.
   */
  private boolean completed = false;
  /**
   * Whether this node should be removed from the graph upon the corresponding job completion.
   */
  private boolean removeUponCompletion;

  /**
   * Create a new node with the specified id.
   * @param id the id of this node.
   * @param jobUuid the job uuid or {@code null} if the uuid is unknown.
   */
  public DependencyNode(final String id, final String jobUuid) {
    this.id = id;
    this.jobUuid = jobUuid;
  }

  /**
   * Get the id of this node.
   * @return the node id as a string.
   */
  public String getId() {
    return id;
  }

  /**
   * Add the specified dependency to this node.
   * @param node the node to add as a dependency.
   */
  void addDependency(final DependencyNode node) {
    String depId = node.getId();
    LinkedList<String> cycle = new LinkedList<>();
    if (hasCycle(node, new HashSet<String>(), cycle)) {
      // print the full cycle
      StringBuilder sb = new StringBuilder(getId()).append(" ==> ");
      for (String id: cycle) sb.append(id).append(" ==> ");
      sb.append(getId());
      Utils.print("cycle detected while adding dependency '%s' to '%s' : %s", depId, getId(), sb);
      return;
    }
    dependencies.put(depId, node);
    node.addDependedOn(this);
    if (!node.isCompleted()) {
      addPendingDependency(node.getId());
      onCompleted(false);
    }
  }

  /**
   * Remove the specified dependency from this node.
   * @param node the dependency to remove.
   */
  void removeDependency(final DependencyNode node) {
    dependencies.remove(node.getId());
    pendingDependencies.remove(node.getId());
  }

  /**
   * Add the specified node to the set of nodes that depend on this node.
   * @param node the node to add.
   */
  void addDependedOn(final DependencyNode node) {
    dependedOn.put(node.getId(), node);
  }

  /**
   * Add the specified pending ((i.e. not completed) dependency.
   * @param id the id of the pending dependency to add.
   */
  void addPendingDependency(final String id) {
    pendingDependencies.add(id);
  }

  /**
   * Add the specified pending ((i.e. not completed) dependency.
   * @param id the id of the pending dependency to add.
   */
  void removePendingDependency(final String id) {
    pendingDependencies.remove(id);
  }

  /**
   * Determine whether this node has any pending dependency.
   * @return {@code true} if there is any pending dependency, {@code false} otherwise.
   */
  public boolean hasPendingDependency() {
    return !pendingDependencies.isEmpty();
  }

  /**
   * Determine whther the specified node has a direct or indirect dependency on this node.
   * @param node the node to check.
   * @param visited the set of already visited nodes in the dependency graph.
   * @param path the full cycle path, if there is a cycle (for debugging purposes).
   * @return {@code true} if there is a dependency, {@code false} otherwise.
   */
  private boolean hasCycle(final DependencyNode node, final Set<String> visited, final LinkedList<String> path) {
    path.push(node.getId());
    if (!visited.contains(node.getId())) {
      if (node.getDependency(this.id) != null) return true;
      visited.add(node.getId());
      for (DependencyNode dependency: node.getDependencies()) {
        if (hasCycle(dependency, visited, path)) return true;
      }
    }
    path.pop();
    return false;
  }

  /**
   * Get the dependency with the specified id.
   * @param id the id of the dependency to lookup.
   * @return a {@link DependencyNode} instance this node depends on, or {@code null} if there is no dependency with the specified id.
   */
  public DependencyNode getDependency(final String id) {
    return dependencies.get(id);
  }

  /**
   * Get the depencies for this node.
   * @return a collection of {@link DependencyNode}s, possibly empty.
   */
  public Collection<DependencyNode> getDependencies() {
    return dependencies.values();
  }

  /**
   * Get the ids of the dependencies for this node.
   * @return a set of dpeendency ids, possibly empty.
   */
  public Set<String> getDependenciesIds() {
    return dependencies.keySet();
  }

  /**
   * Get the nodes that depend on this node.
   * @return a collection of {@link DependencyNode}s, possibly empty.
   */
  Collection<DependencyNode> getDependendedOn() {
    return dependedOn.values();
  }

  /**
   * Determine whether the job represented by this node has completed.
   * @return {@code true} if the job has completed, {@code false} otherwise.
   */
  public boolean isCompleted() {
    return completed;
  }

  /**
   * Specify whether the job represented by this node has completed.
   * @param completed {@code true} if the job has completed, {@code false} otherwise.
   * @return a list of nodes whose associated jobs should be resumed.
   */
  List<DependencyNode> onCompleted(final boolean completed) {
    List<DependencyNode> result = new ArrayList<>();
    this.completed = completed;
    for (DependencyNode node: dependedOn.values()) {
      if (node != null) {
        if (!completed) {
          node.addPendingDependency(getId());
          node.onCompleted(false);
        } else {
          node.removePendingDependency(getId());
          if (!node.hasPendingDependency()) result.add(node);
        }
      }
    }
    return result;
  }

  /**
   * Get the JPPF uuid of the associated job.
   * @return the job uuid or {@code null} if the uuid is unknown.
   */
  public String getJobUuid() {
    return jobUuid;
  }

  /**
   * Set the JPPF uuid of the associated job.
   * @param jobUuid the job uuid or {@code null} if the uuid is unknown.
   */
  void setJobUuid(final String jobUuid) {
    this.jobUuid = jobUuid;
  }

  /**
   * Determine whether this node should be removed from the graph upon the corresponding job completion.
   * @return {@code true} if this node should be removed from the graph, {@code false} otherwise.
   */
  public boolean isRemoveUponCompletion() {
    return removeUponCompletion;
  }

  /**
   * Specify whether this node should be removed from the graph upon the corresponding job completion.
   * @param removeUponCompletion {@code true} to specify that this node should be removed from the graph, {@code false} otherwise.
   */
  void setRemoveUponCompletion(final boolean removeUponCompletion) {
    this.removeUponCompletion = removeUponCompletion;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("id=").append(id)
      .append(", dependencies=").append(dependencies.keySet())
      .append(", pending=").append(pendingDependencies)
      .append(", dependedOn=").append(dependedOn.keySet())
      .append(", removeUponCompletion=").append(removeUponCompletion)
      .append(']')
      .toString();
  }
}