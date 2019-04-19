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

import org.jppf.utils.collections.*;

/**
 * A graph of the tasks in a job, representing the "depended on" relationships between tasks.
 * Each task is represented by its position in the job.
 * @author Laurent Cohen
 * @exclude
 */
public class JobTaskGraph implements Serializable {
  /**
   * Mapping of nodes to their position.
   */
  private Map<Integer, JobTaskNode> nodesMap;
  /**
   * Mapping of node positions to the positions of their dependants.
   */
  private transient CollectionMap<Integer, JobTaskNode> dependantsMap = new ArrayListHashMap<>();
  /**
   * Mapping of node positions to the positions of their remaining unexecute dependencies.
   */
  private transient CollectionMap<Integer, Integer> remainingDependenciesMap = new ArrayListHashMap<>();
  /**
   * The set of non-executed tasks that no longer have pending dependencies.
   */
  private transient Set<Integer> availableNodes = new HashSet<>();
  /**
   * The count of completed tasks.
   */
  private int doneCount;

  /**
   * Create this graph form the specified collection of nodes.
   * @param nodes the nodes that constitute the graph.
   */
  public JobTaskGraph(final Collection<JobTaskNode> nodes) {
    final Map<Integer, JobTaskNode> map = new HashMap<>(nodes.size());
    for (final JobTaskNode node: nodes) map.put(node.getPosition(), node);
    buildGraph(map);
  }

  /**
   * Create this graph form the specified collection of nodes.
   * @param nodes the nodes that constitute the graph.
   */
  public JobTaskGraph(final Map<Integer, JobTaskNode> nodes) {
    buildGraph(nodes);
  }

  /**
   * Create this graph form the specified collection of nodes.
   * @param nodes the nodes that constitute the graph.
   */
  private void buildGraph(final Map<Integer, JobTaskNode> nodes) {
    nodesMap = nodes;
    for (final Map.Entry<Integer, JobTaskNode> entry: nodesMap.entrySet()) {
      final int pos = entry.getKey();
      final JobTaskNode node = entry.getValue();
      final List<JobTaskNode> dependencies = node.getDependencies();
      int remaining = 0;
      for (final JobTaskNode dep: dependencies) {
        dependantsMap.putValue(dep.getPosition(), node);
        if (!dep.isDone()) {
          remainingDependenciesMap.putValue(pos, dep.getPosition());
          remaining++;
        } else doneCount++;
      }
      if (remaining <= 0) availableNodes.add(pos);
    }
  }

  /**
   * @param position the position of the node to lookup.
   * @return the {@link JobTaskNode} at the specified position, or {@link null} if there isn't one.
   */
  JobTaskNode nodeAt(final int position) {
    return nodesMap.get(position);
  }

  /**
   * Called when a task has completed or was cancelled.
   * @param position the position of hte task in the job.
   */
  public void nodeDone(final int position) {
    final JobTaskNode node = nodeAt(position);
    if (node != null) {
      doneCount++;
      availableNodes.remove(position);
      node.setDone(true);
      final Collection<JobTaskNode> dependants = dependantsMap.getValues(position);
      if (dependants != null) {
        for (final JobTaskNode dependant: dependants) {
          final int dependantPosition = dependant.getPosition();
          remainingDependenciesMap.removeValue(dependantPosition, position);
          if (!remainingDependenciesMap.containsKey(dependantPosition)) availableNodes.add(dependantPosition);
        }
      }
    }
  }

  /**
   * @return the set of non-executed tasks that no longer have pending dependencies.
   */
  public Set<Integer> getAvailableNodes() {
    return availableNodes;
  }

  /**
   * @return whether all task in the job graph are odne.
   */
  public boolean isDone() {
    return doneCount >= nodesMap.size();
  }

  /**
   * Start the visit of the graph.
   * @param visitor the visitor function to use.
   */
  public void startVisit(final TaskNodeVisitor visitor) {
    final Set<Integer> visitedPositions = new HashSet<>();
    for (final Map.Entry<Integer, JobTaskNode> entry: nodesMap.entrySet()) {
      if (Visit(entry.getValue(), visitor, visitedPositions) == TaskNodeVisitResult.STOP) break;
    }
  }

  /**
   * Visit the specified task node.
   * @param taskNode the node to visit.
   * @param visitor the visitor function to use.
   * @param visitedPositions the set of already visited positions.
   * @return the result of the node's visit as a {@link TaskNodeVisitResult} enum element.
   */
  private TaskNodeVisitResult Visit(final JobTaskNode taskNode, final TaskNodeVisitor visitor, final Set<Integer> visitedPositions) {
    if (visitedPositions.contains(taskNode.getPosition())) return TaskNodeVisitResult.CONTINUE;
    visitedPositions.add(taskNode.getPosition());
    final TaskNodeVisitResult result = visitor.visitTaskNode(taskNode);
    if (result == TaskNodeVisitResult.STOP) return result;
    else if (result == TaskNodeVisitResult.SKIP) return TaskNodeVisitResult.CONTINUE;
    for (final JobTaskNode child: taskNode.getDependencies()) {
      if (Visit(child, visitor, visitedPositions) == TaskNodeVisitResult.STOP) return TaskNodeVisitResult.STOP;
    }
    return result;
  }

  /**
   * Save the state of this object to a stream (i.e.,serialize it).
   * @param out the output stream to which to write this object. 
   * @throws IOException if any I/O error occurs.
   */
  private void writeObject(final ObjectOutputStream out) throws IOException {
    out.writeInt(nodesMap.size());
    for (final Map.Entry<Integer, JobTaskNode> entry: nodesMap.entrySet()) {
      final JobTaskNode node = entry.getValue();
      out.writeInt(node.getPosition());
      out.writeBoolean(node.isDone());
      final List<JobTaskNode> deps = node.getDependencies();
      out.writeInt(deps.size());
      for (final JobTaskNode dep: deps) out.writeInt(dep.getPosition());
    }
  }

  /**
   * Reconstitute this object from a stream (i.e., deserialize it).
   * @param in the input stream from which to read the object. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph could not be found.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    final int nbNodes = in.readInt();
    final Map<Integer, JobTaskNode> nodesMap = new HashMap<>(nbNodes);
    final CollectionMap<Integer, Integer> dependenciesMap = new ArrayListHashMap<>();
    for (int i=0; i<nbNodes; i++) {
      final int pos = in.readInt();
      final boolean done = in.readBoolean();
      final JobTaskNode node = new JobTaskNode(pos, done, null);
      nodesMap.put(pos, node);
      final int nbDeps = in.readInt();
      if (nbDeps > 0) {
        final List<Integer> deps = new ArrayList<>(nbDeps);
        for (int j=0; j<nbDeps; j++) deps.add(in.readInt());
        dependenciesMap.addValues(pos, deps);
      }
    }
    for (Map.Entry<Integer, Collection<Integer>> entry: dependenciesMap.entrySet()) {
      final Collection<Integer> depsPositions = entry.getValue();
      final List<JobTaskNode> deps = new ArrayList<>(depsPositions.size());
      for (final int p: depsPositions) deps.add(nodesMap.get(p));
      nodesMap.get(entry.getKey()).getDependencies().addAll(deps);
    }
    dependenciesMap.clear();
    if (remainingDependenciesMap == null) remainingDependenciesMap = new ArrayListHashMap<>();
    if (dependantsMap == null) dependantsMap = new ArrayListHashMap<>();
    if (availableNodes == null) availableNodes = new HashSet<>();
    buildGraph(nodesMap);
  }
}
