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

/**
 * A representation of the jobs dependencies graph. This is a directed acyclic graph.
 * @author Laurent Cohen
 */
public interface JobDependencyGraph extends Serializable {
  /**
   * Get the node for the specified dependency id.
   * @param id the id of the node to retrieve.
   * @return a {@link JobDependencyNode} instance, or {@code null} if no node has the specified id.
   */
  JobDependencyNode getNode(String id);

  /**
   * Get the size of this graph, that is, the number of nodes or vertices.
   * @return the size of this graph, always >= 0.
   */
  int getSize();

  /**
   * Get the dependency ids of all the nodes currently in this graph.
   * @return a Set of nodes ids, possibly empty.
   */
  Set<String> getNodeIds();

  /**
   * Get the node whose corresponding job has the specified uuid.
   * @param jobUuid the id of job associated with the node to retrieve.
   * @return a {@link JobDependencyNode} instance, or {@code null} if no node as the specified job uuid.
   */
  JobDependencyNode getNodeByJobUuid(String jobUuid);

  /**
   * Get the ids of all the nodes currently in this graph.
   * @return a Set of nodes ids, possibly empty.
   */
  Collection<JobDependencyNode> getAllNodes();

  /**
   * Get the nodes in the job dependency graph, whose corresponding job has arrived in the job queue.
   * @return a Set of nodes ids, possibly empty.
   */
  Collection<JobDependencyNode> getQueuedNodes();

  /**
   * Get the nodes that depend on the node with the specified dependency id.
   * @param id the id of the node whose dependents to find.
   * @return a collection of nodes that dependen on the specified node, or {@code null} if there is no node with the specified id.
   */
  Collection<JobDependencyNode> getDependedOn(String id);

  /**
   * Determine whether this job dependency graph is empy.
   * @return {@code true} if this graph is empty, {@code false} otherwise.
   */
  boolean isEmpty();
}