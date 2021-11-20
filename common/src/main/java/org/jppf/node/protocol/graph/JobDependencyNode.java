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

import org.slf4j.*;

/**
 * Instances of this class represent a node in the job dependencies graph.
 * They also maintain state information about the job and its dependencies.
 * @since 6.2
 */
public class JobDependencyNode implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JobDependencyNode.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The id asociated with this node. This is the application-defined id for the corresponding job.
   */
  private final String id;
  /**
   * The JPPF uuid of the associated job.
   */
  private String jobUuid;
  /**
   * A mapping of this node's dependencies ids to the corresponding {@link JobDependencyNode} objects.
   * This is in effect a representation of the whole graph.
   */
  private final Map<String, JobDependencyNode> dependencies =  new HashMap<>();
  /**
   * The ids of the nodes that depend on this node.
   */
  private final Map<String, JobDependencyNode> dependedOn = new HashMap<>();
  /**
   * The ids of the nodes that depend on this node.
   */
  private final Set<String> pendingDependencies = new HashSet<>();
  /**
   * Whether the job represented by this node has completed.
   */
  private boolean completed;
  /**
   * Whether this node should be removed from the graph upon the corresponding job completion.
   */
  private boolean graphRoot;
  /**
   * Whether the job represented by this dependency node should be cancelled when it arrives in the server queue.
   */
  private boolean cancelled;
 
  /**
   * Create a new node with the specified id.
   * @param id the id of this node.
   * @param jobUuid the job uuid or {@code null} if the uuid is unknown.
   * @exclude
   */
  public JobDependencyNode(final String id, final String jobUuid) {
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
   * @throws JPPFJobDependencyCycleException if a cycle is detected.
   */
  void addDependency(final JobDependencyNode node) throws JPPFJobDependencyCycleException {
    final String depId = node.getId();
    final LinkedList<String> cycle = new LinkedList<>();
    if (hasCycle(node, new HashSet<String>(), cycle)) {
      final StringBuilder sb = new StringBuilder(getId()).append(" ==> ");
      for (String id: cycle) sb.append(id).append(" ==> ");
      sb.append(getId());
      final String message = sb.toString();
      log.error("cycle detected while adding dependency '{}' to '{}' : {}", depId, getId(), message);
      throw new JPPFJobDependencyCycleException(message, getId(), cycle);
    }
    dependencies.put(depId, node);
    node.addDependedOn(this);
    if (!node.isCompleted()) {
      addPendingDependency(node.getId());
    }
  }

  /**
   * Remove the specified dependency from this node.
   * @param node the dependency to remove.
   */
  void removeDependency(final JobDependencyNode node) {
    dependencies.remove(node.getId());
    pendingDependencies.remove(node.getId());
  }

  /**
   * Add the specified node to the set of nodes that depend on this node.
   * @param node the node to add.
   */
  void addDependedOn(final JobDependencyNode node) {
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
   * Determine whether this node has any pending (not completed) dependency.
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
  private boolean hasCycle(final JobDependencyNode node, final Set<String> visited, final LinkedList<String> path) {
    path.addLast(node.getId());
    if (!visited.contains(node.getId())) {
      if (node.getDependency(this.id) != null) return true;
      visited.add(node.getId());
      for (final JobDependencyNode dependency: node.getDependencies()) {
        if (hasCycle(dependency, visited, path)) return true;
      }
    }
    path.removeLast();
    return false;
  }

  /**
   * Get the dependency with the specified id.
   * @param id the id of the dependency to lookup.
   * @return a {@link JobDependencyNode} instance this node depends on, or {@code null} if there is no dependency with the specified id.
   */
  public JobDependencyNode getDependency(final String id) {
    return dependencies.get(id);
  }

  /**
   * Get the dependencies for this node.
   * @return a collection of {@link JobDependencyNode}s, possibly empty.
   */
  public Collection<JobDependencyNode> getDependencies() {
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
   * @return a collection of {@link JobDependencyNode}s, possibly empty.
   */
  public Collection<JobDependencyNode> getDependedOn() {
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
  List<JobDependencyNode> onCompleted(final boolean completed) {
    final List<JobDependencyNode> result = new ArrayList<>();
    this.completed = completed;
    for (final JobDependencyNode node: dependedOn.values()) {
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
    if (debugEnabled) log.debug("processed completion({}) on {}", completed, this);
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
   * Determine whether this node is a job dependency graph root.
   * @return {@code true} if this node is a graph root, {@code false} otherwise.
   */
  public boolean isGraphRoot() {
    return graphRoot;
  }

  /**
   * Specify whether this node is a job dependency graph root.
   * @param graphRoot {@code true} to specify that this node is a graph root, {@code false} otherwise.
   * @exclude
   */
  public void setGraphRoot(final boolean graphRoot) {
    this.graphRoot = graphRoot;
  }

  /**
   * Determine whether the job represented by this dependency node has been cancelled or should be cancelled when it arrives in the server queue.
   * @return {@code true} if the job has been marked as cancelled, {@code false} otherwise.
   */
  public boolean isCancelled() {
    return cancelled;
  }

  /**
   * @param cancelled {@code true} to cancel the job upon queueing, {@code false} otherwise.
   * @exclude
   */
  public void setCancelled(final boolean cancelled) {
    this.cancelled = cancelled;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("id=").append(id)
      .append(", removeUponCompletion=").append(graphRoot)
      .append(", completed=").append(completed)
      .append(", cancelled=").append(cancelled)
      .append(", jobUuid=").append(jobUuid)
      .append(", dependencies=").append(dependencies.keySet())
      .append(", pending=").append(pendingDependencies)
      .append(", dependedOn=").append(dependedOn.keySet())
      .append(']').toString();
  }
}
