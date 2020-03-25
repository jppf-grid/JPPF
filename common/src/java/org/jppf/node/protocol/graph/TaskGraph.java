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

import org.jppf.node.protocol.PositionalElement;
import org.jppf.serialization.SerializationUtils;
import org.jppf.utils.collections.*;

/**
 * A graph of the tasks in a job, representing the "depends on" relationships between tasks.
 * Each task is represented by its position in the job as an {@code Integer} value.
 * <p>Dependency cycles are not allowed, making this graph effectively a Directed Acyclic Graph (DAG).
 * This allows, among other things to compute one or more topological orders for the tasks.
 * @author Laurent Cohen
 * @exclude
 */
public class TaskGraph implements Serializable {
  /**
   * Mapping of nodes to their position.
   */
  private transient Map<Integer, Node> nodesMap;
  /**
   * Mapping of not done nodes to their position.
   */
  private transient Map<Integer, Node> notDoneMap;
  /**
   * Mapping of node positions to the positions of their dependants.
   */
  private transient CollectionMap<Integer, Node> dependantsMap = new ArrayListHashMap<>();
  /**
   * Mapping of node positions to the positions of their remaining unexecuted dependencies.
   */
  private transient CollectionMap<Integer, Integer> remainingDependenciesMap = new ArrayListHashMap<>();
  /**
   * The set of non-executed tasks that no longer have pending dependencies.
   */
  private transient Set<Integer> availableNodes = new HashSet<>();
  /**
   * The count of completed tasks.
   */
  private transient int doneCount;

  /**
   * No-arg constrcutor used for custom (de)serialization.
   */
  public TaskGraph() {
  }

  /**
   * Create this graph from the specified collection of nodes.
   * @param nodes the nodes that constitute the graph.
   */
  public TaskGraph(final Collection<Node> nodes) {
    final Map<Integer, Node> map = new HashMap<>(nodes.size());
    for (final Node node: nodes) map.put(node.getPosition(), node);
    buildGraph(map);
  }

  /**
   * Create this graph form the specified collection of nodes.
   * @param nodes the nodes that constitute the graph.
   */
  public TaskGraph(final Map<Integer, Node> nodes) {
    buildGraph(nodes);
  }

  /**
   * Create this graph form the specified collection of nodes.
   * @param nodes the nodes that constitute the graph.
   */
  private void buildGraph(final Map<Integer, Node> nodes) {
    nodesMap = nodes;
    notDoneMap = new HashMap<>(nodes.size());
    for (final Map.Entry<Integer, Node> entry: nodesMap.entrySet()) {
      final int pos = entry.getKey();
      final Node node = entry.getValue();
      if (!node.isDone()) notDoneMap.put(pos, node);
      final List<Node> dependencies = node.getDependencies();
      int remaining = 0;
      for (final Node dep: dependencies) {
        dependantsMap.putValue(dep.getPosition(), node);
        if (!dep.isDone()) {
          remainingDependenciesMap.putValue(pos, dep.getPosition());
          remaining++;
        }
      }
      if (!node.isDone()) {
        if (remaining <= 0) availableNodes.add(pos);
      }
      else doneCount++;
    }
  }

  /**
   * Perform a depth-first search topological sort.
   * @param position the position of the node to lookup.
   * @return the {@link Node} at the specified position, or {@link null} if there isn't one.
   */
  public Node nodeAt(final int position) {
    return nodesMap.get(position);
  }

  /**
   * Called when a task has completed or was cancelled.
   * @param position the position of hte task in the job.
   */
  public void nodeDone(final int position) {
    final Node node = nodeAt(position);
    if (node != null) {
      doneCount++;
      availableNodes.remove(position);
      notDoneMap.remove(position);
      node.setDone(true);
      final Collection<Node> dependants = dependantsMap.getValues(position);
      if (dependants != null) {
        for (final Node dependant: dependants) {
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
   * Determine whether a task at the given position has tasks that depend on it.
   * @param position the position of the task in the job.
   * @return {@code true} if the task has other tasks that depend on it, {@code false} otherwise.
   */
  public boolean isDependendOn(final int position) {
    return dependantsMap.getValues(position) != null;
  }

  /**
   * @return a list of node positions in topological order.
   * @see <a href="https://en.wikipedia.org/wiki/Topological_sorting">topological sorting</a> on Wikipedia.
   */
  public List<Integer> topologicalSortDFS() {
    final LinkedList<Integer> result = new LinkedList<>();
    final Map<Integer, Node> p = new HashMap<>(notDoneMap);
    while (!p.isEmpty()) {
      startVisitNotDone(new TaskNodeVisitor() {
        @Override
        public TaskNodeVisitResult visitTaskNode(final Node node) {
          if (node.isDone()) return TaskNodeVisitResult.SKIP;
          return TaskNodeVisitResult.CONTINUE;
        }
  
        @Override
        public void postVisitNode(final Node node) {
          p.remove(node.getPosition());
          result.addLast(node.getPosition());
        }
      });
    }
    return result;
  }

  /**
   * Start the visit of all the nodes in the graph.
   * @param visitor the visitor function to use.
   */
  public void startVisit(final TaskNodeVisitor visitor) {
    startVisit(visitor, nodesMap, true);
  }

  /**
   * Start the visit of the graph nodes that are not done.
   * @param visitor the visitor function to use.
   */
  public void startVisitNotDone(final TaskNodeVisitor visitor) {
    startVisit(visitor, notDoneMap, false);
  }

  /**
   * Start the visit of the graph.
   * @param visitor the visitor function to use.
   * @param map the nodes to visit.
   * @param visitDoneNodes whether to visit the nodes that are done.
   */
  private void startVisit(final TaskNodeVisitor visitor, final Map<Integer, Node> map, final boolean visitDoneNodes) {
    final Set<Integer> visitedPositions = new HashSet<>();
    for (final Map.Entry<Integer, Node> entry: map.entrySet()) {
      final Node taskNode = entry.getValue();
      if (taskNode.isDone() && !visitDoneNodes) continue; 
      if (Visit(taskNode, visitor, visitDoneNodes, visitedPositions) == TaskNodeVisitResult.STOP) break;
    }
  }

  /**
   * Visit the specified task node.
   * @param taskNode the node to visit.
   * @param visitor the visitor function to use.
   * @param visitDoneNodes whether to visit the nodes that are done.
   * @param visitedPositions the set of already visited positions.
   * @return the result of the node's visit as a {@link TaskNodeVisitResult} enum element.
   */
  private TaskNodeVisitResult Visit(final Node taskNode, final TaskNodeVisitor visitor, final boolean visitDoneNodes, final Set<Integer> visitedPositions) {
    if (visitedPositions.contains(taskNode.getPosition())) return TaskNodeVisitResult.CONTINUE;
    visitor.preVisitNode(taskNode);
    visitedPositions.add(taskNode.getPosition());
    final TaskNodeVisitResult result = visitor.visitTaskNode(taskNode);
    if (result == TaskNodeVisitResult.STOP) return result;
    else if (result == TaskNodeVisitResult.SKIP) return TaskNodeVisitResult.CONTINUE;
    for (final Node child: taskNode.getDependencies()) {
      if (child.isDone() && !visitDoneNodes) continue; 
      if (Visit(child, visitor, visitDoneNodes, visitedPositions) == TaskNodeVisitResult.STOP) return TaskNodeVisitResult.STOP;
    }
    visitor.postVisitNode(taskNode);
    return result;
  }

  /**
   * Save the state of this object to a stream (i.e.,serialize it).
   * @param out the output stream to which to write this object. 
   * @throws IOException if any I/O error occurs.
   */
  private void writeObject(final ObjectOutputStream out) throws IOException {
    serialize(out);
  }

  /**
   * Reconstitute this object from a stream (i.e., deserialize it).
   * @param in the input stream from which to read the object. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph could not be found.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    deserialize(in);
  }

  /**
   * Save the state of this object to a stream (i.e.,serialize it).
   * @param out the output stream to which to write this object. 
   * @throws IOException if any I/O error occurs.
   */
  public void serialize(final OutputStream out) throws IOException {
    final byte[] buf = new byte[8];
    SerializationUtils.writeVarInt(out, nodesMap.size(), buf);
    for (final Map.Entry<Integer, Node> entry: nodesMap.entrySet()) {
      final Node node = entry.getValue();
      SerializationUtils.writeVarInt(out, node.getPosition(), buf);
      out.write(node.isDone() ? 1 : 0);
      final List<Node> deps = node.getDependencies();
      SerializationUtils.writeVarInt(out, deps.size(), buf);
      for (final Node dep: deps) SerializationUtils.writeVarInt(out, dep.getPosition(), buf);
    }
  }

  /**
   * Reconstitute this object from a stream (i.e., deserialize it).
   * @param in the input stream from which to read the object. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph could not be found.
   */
  public void deserialize(final InputStream in) throws IOException, ClassNotFoundException {
    final byte[] buf = new byte[8];
    final int nbNodes = SerializationUtils.readVarInt(in, buf);
    nodesMap = new HashMap<>(nbNodes);
    final CollectionMap<Integer, Integer> dependenciesMap = new ArrayListHashMap<>();
    for (int i=0; i<nbNodes; i++) {
      final int pos = SerializationUtils.readVarInt(in, buf);
      final boolean done = in.read() != 0;
      final Node node = new Node(pos, done, null);
      nodesMap.put(pos, node);
      final int nbDeps = SerializationUtils.readVarInt(in, buf);
      if (nbDeps > 0) {
        final List<Integer> deps = new ArrayList<>(nbDeps);
        for (int j=0; j<nbDeps; j++) deps.add(SerializationUtils.readVarInt(in, buf));
        dependenciesMap.addValues(pos, deps);
      }
    }
    for (Map.Entry<Integer, Collection<Integer>> entry: dependenciesMap.entrySet()) {
      final Collection<Integer> depsPositions = entry.getValue();
      final List<Node> deps = new ArrayList<>(depsPositions.size());
      for (final int p: depsPositions) deps.add(nodesMap.get(p));
      nodesMap.get(entry.getKey()).getDependencies().addAll(deps);
    }
    dependenciesMap.clear();
    if (remainingDependenciesMap == null) remainingDependenciesMap = new ArrayListHashMap<>();
    if (dependantsMap == null) dependantsMap = new ArrayListHashMap<>();
    if (availableNodes == null) availableNodes = new HashSet<>();
    buildGraph(nodesMap);
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("nodes=").append(nodesMap.size())
      .append(", doneCount=").append(doneCount)
      .append(", nodesWithDependant=").append(dependantsMap.keySet().size())
      .append(", nodesWithDependencies=").append(remainingDependenciesMap.keySet().size())
      .append(", availableNodes=").append(availableNodes.size())
      .append(']').toString();
  }

  /**
   * @return the count of completed tasks.
   */
  public int getDoneCount() {
    return doneCount;
  }

  /**
   * A node in the graph of the tasks in a job which represents a task and its dependants.
   * @exclude
   */
  public static class Node implements Serializable, PositionalElement<Node> {
    /**
     * The dependencies of this task, if any.
     */
    private final List<Node> dependencies = new ArrayList<>();
    /**
     * The position of this task in the job.
     */
    private final int position;
    /**
     * Whether this task is done, that is, either completed or cancelled.
     */
    private boolean done;

    /**
     * Initialize this node with the specified task.
     * @param position the position of this task.
     * @param done whether this task is done.
     * @param dependencies the dependencies of this task, may be {@code null}.
     */
    public Node(final int position, final boolean done, final List<Node> dependencies) {
      this.position = position;
      this.done = done;
      if (dependencies != null) this.dependencies.addAll(dependencies);
    }

    /**
     * Add the specified dependency.
     * @param dependency the dependency to add.
     */
    public void addDependency(final Node dependency) {
      dependencies.add(dependency);
    }

    /**
     * @return the tasks that depend on this task, if any.
     */
    public List<Node> getDependencies() {
      return dependencies;
    }

    @Override
    public int getPosition() {
      return position;
    }

    /**
     * @return whether this taks is done.
     */
    public boolean isDone() {
      return done;
    }

    /**
     * Shether this task is done, that is, either completed or cancelled.
     * @param done the done state.
     */
    public void setDone(final boolean done) {
      this.done = done;
    }

    @Override
    public String toString() {
      return new StringBuilder(getClass().getSimpleName()).append('[')
        .append("position=").append(position)
        .append(", done=").append(done)
        .append(", dependencies=").append(dependencies.size())
        .append(']').toString();
    }

    @Override
    public int hashCode() {
      return position;
    }

    @Override
    public boolean equals(final Object obj) {
      if (!(obj instanceof Node)) return false;
      return position == ((Node) obj).position;
    }
  }
}
