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

package org.jppf.server.job.management;

import java.util.*;

import org.jppf.job.JobSelector;
import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.node.protocol.graph.*;
import org.jppf.server.JPPFDriver;

/**
 * Concrete implementation of the job dependency graph management interface.
 * @author Laurent Cohen
 * @exclude
 */
public class JobDependencyManager implements JobDependencyManagerMBean {
  /**
   * The JPPF driver.
   */
  final JPPFDriver driver;
  /**
   * Reference to the job dependency graph managed by the server.
   */
  private final MutableJobDependencyGraph graph;

  /**
   * Initialize this dependency manager with the specified JPPF driver.
   * @param driver the JPPF driver.
   */
  public JobDependencyManager(final JPPFDriver driver) {
    this.driver = driver;
    this.graph = driver.getQueue().getDependenciesHandler().getGraph();
  }

  @Override
  public int getGraphSize() {
    return graph.getSize();
  }

  @Override
  public Set<String> getNodeIds() {
    return graph.getNodeIds();
  }

  @Override
  public Collection<JobDependencyNode> getAllNodes() {
    return graph.getAllNodes();
  }

  @Override
  public Collection<JobDependencyNode> getQueuedNodes() {
    return graph.getQueuedNodes();
  }

  @Override
  public Collection<JobDependencyNode> getQueuedNodes(final JobSelector selector) {
    final Collection<JobDependencyNode> queuedNodes = graph.getQueuedNodes();
    if (selector == null) return queuedNodes;
    final List<JobDependencyNode> result = new ArrayList<>(queuedNodes.size());
    for (final JobDependencyNode node: queuedNodes) {
      final String uuid = node.getJobUuid();
      if (uuid == null) continue;
      final JPPFDistributedJob job = driver.getJob(uuid);
      if ((job != null) && selector.accepts(job)) result.add(node);
    }
    return result;
  }

  @Override
  public JobDependencyNode getNode(final String id) {
    return graph.getNode(id);
  }

  @Override
  public JobDependencyGraph getGraph() {
    return new JobDependencyGraphImpl(graph.getAllNodes());
  }
}
