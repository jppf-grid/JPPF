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

import org.jppf.node.protocol.JobDependencySpec;
import org.slf4j.*;

/**
 * A representation of the jobs dependencies graph. This is a directed acyclic graph.
 * @author Laurent Cohen
 * @exclude
 */
public class MutableJobDependencyGraph extends JobDependencyGraphImpl {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(MutableJobDependencyGraph.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Default constructor.
   */
  public MutableJobDependencyGraph() {
  }

  /**
   * Notify that the specified job has completed and update the dependencies graph accordingly.
   * @param jobUuid the uuid of the job that has completed.
   * @return the list of jobs that no longer have any dependency and may thus be resumed.
   */
  public synchronized List<JobDependencyNode> jobEnded(final String jobUuid) {
    final JobDependencyNode node = getNodeByJobUuid(jobUuid);
    if (debugEnabled) log.debug("processing job ended for {}", node);
    if (node != null) return node.onCompleted(true);
    return null;
  }

  /**
   * 
   * @param spec the dependencies specification for the job.
   * @param jobUuid the uuid of the associated jppf job.
   * @return the added {@link JobDependencyNode}.
   * @throws JPPFJobDependencyCycleException if a cycle is detected.
   */
  public synchronized JobDependencyNode addNode(final JobDependencySpec spec, final String jobUuid) throws JPPFJobDependencyCycleException {
    if (debugEnabled) log.debug("adding node with spec={}, uuid={}", spec, jobUuid);
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
   * @throws JPPFJobDependencyCycleException if a cycle is detected.
   */
  private JobDependencyNode addNode(final String id, final String jobUuid, final List<String> dependencies) throws JPPFJobDependencyCycleException {
    if (debugEnabled) log.debug("adding node with id={}, uuid={}, dependencies={}", id, jobUuid, dependencies);
    JobDependencyNode node = getNode(id);
    if (node == null) {
      node = new JobDependencyNode(id, jobUuid);
      nodes.put(id, node);
    }
    if (debugEnabled) log.debug("node is {}", node);
    if (jobUuid != null) {
      if (node.getJobUuid() == null) node.setJobUuid(jobUuid);
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
    if (debugEnabled) log.debug("removing node with id = {}", id);
    final JobDependencyNode node = getNode(id);
    if (node != null) removeNode(node);
  }

  /**
   * Recursively remove the specified node and all its dependencies from this graph.
   * @param node the node to remove.
   */
  public synchronized void removeNode(final JobDependencyNode node) {
    if (debugEnabled) log.debug("removing node {}", node);
    final List<JobDependencyNode> dependencies = new ArrayList<>(node.getDependencies());
    for (final JobDependencyNode dependency: dependencies) {
      if (nodes.containsKey(dependency.getId())) removeNode(dependency);
    }
    for (final JobDependencyNode dependent: node.getDependedOn()) dependent.removeDependency(node);
    nodes.remove(node.getId());
    if (node.getJobUuid() != null) nodesByUuid.remove(node.getJobUuid());
    if (debugEnabled) log.debug("job graph: removed '{}'", node.getId());
  }
}
