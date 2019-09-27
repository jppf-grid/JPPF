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
import java.util.concurrent.Callable;

import org.jppf.node.protocol.JobDependencySpec;
import org.slf4j.*;

/**
 * A representation of the jobs dependencies graph. This is a directed acyclic graph.
 * @author Laurent Cohen
 * @exclude
 */
public class JobDependencyGraph {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JobDependencyGraph.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Job custom ids to nodes.
   */
  private final Map<String, JobDependencyNode> nodes = new HashMap<>();
  /**
   * Job uuids to nodes.
   */
  private final Map<String, JobDependencyNode> nodesByUuid = new HashMap<>();

  /**
   * Default constructor.
   */
  public JobDependencyGraph() {
  }
  
  /**
   * Get the node for the specified id.
   * @param id the id of the node to retrieve.
   * @return a {@link JobDependencyNode} instance, or {@code null} if no node has the specified id.
   */
  public synchronized JobDependencyNode getNode(final String id) {
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
   * @return a {@link JobDependencyNode} instance, or {@code null} if no node as the specified job uuid.
   */
  public synchronized JobDependencyNode getNodeByJobUuid(final String jobUuid) {
    return nodesByUuid.get(jobUuid);
  }

  /**
   * Notify that the specified job has completed and update the dependencies graph accordingly.
   * @param jobUuid the uuid of the job that has completed.
   * @return the list of jobs that no longer have any dependency and may thus be resumed.
   */
  public synchronized List<JobDependencyNode> jobEnded(final String jobUuid) {
    final JobDependencyNode node = getNodeByJobUuid(jobUuid);
    final List<JobDependencyNode> toResume = (node != null) ? node.onCompleted(true) : null;
    return toResume;
  }

  /**
   * 
   * @param spec the dependencies specification for the job.
   * @param jobUuid the uuid of the associated jppf job.
   * @return the added {@link JobDependencyNode}.
   * @throws JPPFDependencyCycleException if a cycle is detected.
   */
  public synchronized JobDependencyNode addNode(final JobDependencySpec spec, final String jobUuid) throws JPPFDependencyCycleException {
    final JobDependencyNode node = addNode(spec.getId(), jobUuid, spec.getDependencies());
    node.setRemoveUponCompletion(spec.isRemoveUponCompletion());
    if (debugEnabled) log.debug("job graph: added {}", node);
    return node;
  }

  /**
   * Add a node with the specified parameters.
   * @param id the node id.
   * @param jobUuid the uuid of the associated jppf job.
   * @param dependencies the dependencies.
   * @return the added {@link JobDependencyNode}.
   * @throws JPPFDependencyCycleException if a cycle is detected.
   */
  private JobDependencyNode addNode(final String id, final String jobUuid, final List<String> dependencies) throws JPPFDependencyCycleException {
    JobDependencyNode node = getNode(id);
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
    if (node == null) node = new JobDependencyNode(id, jobUuid);
    nodes.put(id, node);
    if (jobUuid != null) {
      node.setJobUuid(jobUuid);
      nodesByUuid.put(jobUuid, node);
    }
    if ((dependencies != null) && !dependencies.isEmpty()) {
      for (final String depId: dependencies) {
        final JobDependencyNode depNode = addNode(depId, null, null);
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
    final JobDependencyNode node = getNode(id);
    if (node != null) removeNode(node);
  }

  /**
   * Recursively remove the specified node and all its dependencies from this graph.
   * @param node the node to remove.
   */
  public synchronized void removeNode(final JobDependencyNode node) {
    final List<JobDependencyNode> dependencies = new ArrayList<>(node.getDependencies());
    for (final JobDependencyNode dependency: dependencies) {
      if (nodes.containsKey(dependency.getId())) removeNode(dependency);
    }
    for (JobDependencyNode dependent: node.getDependendedOn()) dependent.removeDependency(node);
    nodes.remove(node.getId());
    if (node.getJobUuid() != null) nodesByUuid.remove(node.getJobUuid());
    if (debugEnabled) log.debug("job graph: removed '{}'", node.getId());
  }

  /**
   * Get the nodes that depend on the node with the specified dependency id.
   * @param id the id of the node whose dependents to find.
   * @return a collection of nodes that dependen on the specified node, or {@code null} if there is no node with the specified id.
   */
  public synchronized Collection<JobDependencyNode> getDependedOn(final String id) {
    final JobDependencyNode node = getNode(id);
    return (node == null) ? null : new ArrayList<>(node.getDependendedOn());
  }

  /**
   * Execute qn arbitrary action while synchronizig on this object.
   * @param action the {@link Runnqble} qction to execute.
   */
  public synchronized void executeSynchronized(final Runnable action) {
    if (action != null) action.run();
  }

  /**
   * Execute qn arbitrary action while synchronizig on this object.
   * @param <E> the type of result returned by the action.
   * @param action the {@link Runnqble} qction to execute.
   * @return the result of the action, or {@code null} if the action is {@code null}.
   * @throws Exception if the action raises an exception.
   */
  public synchronized <E> E executeSynchronized(final Callable<E> action) throws Exception {
    return (action != null) ? action.call() : null;
  }
}
