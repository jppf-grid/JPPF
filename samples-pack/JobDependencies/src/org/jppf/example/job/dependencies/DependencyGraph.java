/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
 * A representation of the jobs dependencies graph. This is a directed acyclic graph implemented as a singleton.
 * @author Laurent Cohen
 */
public final class DependencyGraph {
  /**
   * The singleton instance of this class.
   */
  private static final DependencyGraph INSTANCE = new DependencyGraph();
  /**
   * Job custom ids to nodes.
   */
  private final Map<String, DependencyNode> nodes = new HashMap<>();
  /**
   * Job uuids to nodes.
   */
  private final Map<String, DependencyNode> nodesByUuid = new HashMap<>();

  /**
   * Get the singleton instance of this class.
   * @return a {@link DependencyGraph} object.
   */
  public static DependencyGraph getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor: direct instantiation from other classes is not permitted.
   */
  private DependencyGraph() {
  }
  
  /**
   * Get the node for the specified id.
   * @param id the id of the node to retrieve.
   * @return a {@link DependencyNode} instance, or {@code null} if no node as the specified id.
   */
  public synchronized DependencyNode getNode(final String id) {
    return nodes.get(id);
  }

  /**
   * Get the ids of all trhe nodes currently in this graph.
   * @return a Set of nodes ids, possibly empty.
   */
  public synchronized Set<String> getNodeIds() {
    return nodes.keySet();
  }

  /**
   * Get the node for the specified id.
   * @param jobUuid the id of job associated with the node to retrieve.
   * @return a {@link DependencyNode} instance, or {@code null} if no node as the specified job uuid.
   */
  public synchronized DependencyNode getNodeByJobUuid(final String jobUuid) {
    return nodesByUuid.get(jobUuid);
  }

  /**
   * Notify that the specified job has completed and update the dependencies graph accordingly.
   * @param jobUuid the uuid of the job that has completed.
   * @return the list of jobs that no longer have any dependency and may thus be resumed.
   */
  synchronized List<DependencyNode> jobEnded(final String jobUuid) {
    DependencyNode node = getNodeByJobUuid(jobUuid);
    List<DependencyNode> toResume = node.onCompleted(true);
    return toResume;
  }

  /**
   * 
   * @param spec the dependencies specification for the job.
   * @param jobUuid the uuid of the associated jppf job.
   * @return the added {@link DependencyNode}.
   */
  synchronized DependencyNode addNode(final DependencySpec spec, final String jobUuid) {
    DependencyNode node = addNode(spec.getId(), jobUuid, spec.getDependencies());
    node.setRemoveUponCompletion(spec.isRemoveUponCompletion());
    //Utils.print("graph: added '%s' with dependencies %s", node.getId(), node.getDependenciesIds());
    Utils.print("graph: added %s", node);
    return node;
  }

  /**
   * Add a node with the specified parameters.
   * @param id the node id.
   * @param jobUuid the uuid of the associated jppf job.
   * @param dependencies the dependencies.
   * @return the added {@link DependencyNode}.
   */
  private DependencyNode addNode(final String id, final String jobUuid, final String[] dependencies) {
    DependencyNode node = getNode(id);
    if (node != null) {
      if (node.getJobUuid() != null) {
        if ((jobUuid != null) && !jobUuid.equals(node.getJobUuid())) {
          nodes.remove(id);
          nodesByUuid.remove(node.getJobUuid());
          node = null;
        }
      } else {
        node.setJobUuid(jobUuid);
      }
    }
    if (node == null) node = new DependencyNode(id, jobUuid);
    nodes.put(id, node);
    if (jobUuid != null) {
      node.setJobUuid(jobUuid);
      nodesByUuid.put(jobUuid, node);
    }
    if ((dependencies != null) && (dependencies.length > 0)) {
      for (String depId: dependencies) {
        DependencyNode depNode = addNode(depId, null, null);
        node.addDependency(depNode);
      }
    }
    return node;
  }

  /**
   * Remove the node with the specified id from this graph.
   * @param id the id of the node to remove.
   */
  public synchronized void removeNode(final String id) {
    DependencyNode node = getNode(id);
    if (node != null) removeNode(node);
  }

  /**
   * Recursively remove the specified node and all its dependencies from this graph.
   * @param node the node to remove.
   */
  synchronized void removeNode(final DependencyNode node) {
    // to avoid COncurrentModificationException
    List<DependencyNode> dependencies = new ArrayList<>(node.getDependencies());
    for (DependencyNode dependency: dependencies) {
      if (nodes.containsKey(dependency.getId())) removeNode(dependency);
    }
    for (DependencyNode dependent: node.getDependendedOn()) dependent.removeDependency(node);
    nodes.remove(node.getId());
    if (node.getJobUuid() != null) nodesByUuid.remove(node.getJobUuid());
    Utils.print("graph: removed '%s'", node.getId());
  }
}
