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

import java.io.*;
import java.util.*;

/**
 * A representation of the jobs dependencies graph. This is a directed acyclic graph.
 * @author Laurent Cohen
 * @exclude
 */
public class JobDependencyGraphImpl implements JobDependencyGraph {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Job custom ids to nodes.
   */
  Map<String, JobDependencyNode> nodes = new HashMap<>();
  /**
   * Job uuids to nodes.
   */
  Map<String, JobDependencyNode> nodesByUuid = new HashMap<>();

  /**
   * Default constructor.
   */
  public JobDependencyGraphImpl() {
  }

  /**
   * Construct this graph from an existing collection of nodes.
   * @param nodes the nodes constituting the graph.
   */
  public JobDependencyGraphImpl(final Collection<JobDependencyNode> nodes) {
    for (final JobDependencyNode node: nodes) {
      this.nodes.put(node.getId(), node);
      if (node.getJobUuid() != null) nodesByUuid.put(node.getJobUuid(), node);
    }
  }
  
  @Override
  public synchronized JobDependencyNode getNode(final String id) {
    return nodes.get(id);
  }

  @Override
  public synchronized int getSize() {
    return nodes.size();
  }

  @Override
  public synchronized Set<String> getNodeIds() {
    return new HashSet<>(nodes.keySet());
  }

  @Override
  public synchronized JobDependencyNode getNodeByJobUuid(final String jobUuid) {
    return nodesByUuid.get(jobUuid);
  }

  @Override
  public synchronized Collection<JobDependencyNode> getAllNodes() {
    return new ArrayList<>(nodes.values());
  }

  @Override
  public synchronized Collection<JobDependencyNode> getQueuedNodes() {
    return new ArrayList<>(nodesByUuid.values());
  }

  @Override
  public synchronized Collection<JobDependencyNode> getDependedOn(final String id) {
    final JobDependencyNode node = getNode(id);
    return (node == null) ? null : new ArrayList<>(node.getDependedOn());
  }

  /**
   * Save the state of this object to a stream (i.e.,serialize it).
   * @param out the output stream to which to write the object. 
   * @throws IOException if any I/O error occurs.
   */
  private synchronized void writeObject(final ObjectOutputStream out) throws IOException {
    out.writeObject(nodes);
  }

  /**
   * Reconstitute the obejct from a stream (i.e., deserialize it).
   * @param in the input stream from which to read the object. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph can not be found.
   */
  @SuppressWarnings("unchecked")
  private synchronized void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    nodes = (Map<String, JobDependencyNode>) in.readObject();
    nodesByUuid = new HashMap<>();
    for (final Map.Entry<String, JobDependencyNode> entry: nodes.entrySet()) {
      final JobDependencyNode node = entry.getValue();
      if (node.getJobUuid() != null) nodesByUuid.put(node.getJobUuid(), node);
    }
  }
}
