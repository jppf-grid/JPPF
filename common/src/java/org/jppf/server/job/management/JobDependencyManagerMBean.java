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
import org.jppf.management.doc.*;
import org.jppf.node.protocol.graph.*;

/**
 * A simple management interface for the server-side job dependency graph.
 * @author Laurent Cohen
 */
@MBeanDescription("management interface for the server-side job dependency graphs")
public interface JobDependencyManagerMBean {
  /**
   * Name of this server-side MBean.
   */
  String MBEAN_NAME = "org.jppf:name=jobDependencyManager,type=driver";

  /**
   * Get the size - the number of nodes or vertices that represent jobs - of the job dependency grpah.
   * @return the graph size, always >= 0.
   */
  @MBeanDescription("the size - the number of nodes or vertices that represent jobs - of the job dependency grpah")
  int getGraphSize();

  /**
   * Get the ids of all the nodes currently in the graph.
   * @return a set of node ids.
   */
  @MBeanDescription("the ids of all the nodes currently in the graph")
  Set<String> getNodeIds();

  /**
   * Get all the nodes in the job dependency graph.
   * @return a collection of {@link JobDependencyNode} objects.
   */
  @MBeanDescription("all the nodes currently in the job dependency graph")
  Collection<JobDependencyNode> getAllNodes();

  /**
   * Get the nodes in the job dependency graph, whose corresponding job has arrived in the job queue.
   * @return a collection of {@link JobDependencyNode} objects.
   */
  @MBeanDescription("the nodes in the job dependency graph, whose corresponding job has arrived in the job queue")
  Collection<JobDependencyNode> getQueuedNodes();

  /**
   * Get the nodes in the job dependency graph, whose corresponding job has arrived in the job queue, filtered by the specified job selector.
   * @param selector a {@link JobSelector} to filter the returned jobs. If {@code null} then all queued jobs are returned.
   * @return a collection of {@link JobDependencyNode} objects, possibly empty.
   */
  @MBeanDescription("get the nodes in the job dependency graph, whose corresponding job has arrived in the job queue, filtered by the specified job selector")
  Collection<JobDependencyNode> getQueuedNodes(@MBeanParamName("jobSelector") JobSelector selector);

  /**
   * Get the node with the specified dependency id.
   * @param id the id of the job node to find.
   * @return a {@link JobDependencyNode}, or {@code null} if there is no node with this id in the job dependency graph.
   */
  @MBeanDescription("get the node with the specified dependency id")
  JobDependencyNode getNode(@MBeanParamName("dependencyId") final String id);

  /**
   * Get the graph of job dependencies.
   * @return an instance of {@link JobDependencyGraph}.
   */
  @MBeanDescription("the graph of job dependencies")
  JobDependencyGraph getGraph();
}
