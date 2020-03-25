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

import org.jppf.node.protocol.PositionalElement;
import org.jppf.utils.collections.CollectionMap;

/**
 * Information about the task graph, if any, for a job.
 * Instances of this class are intentded to be transported as part of the communication protocol between clients, servers and nodes. 
 * @author Laurent Cohen
 * @exclude
 */
public class TaskGraphInfo implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The total number of dependencies for all the tasks.
   */
  private final int nbDependencies;
  /**
   * Mapping of each task position to the positions of its dependencies.
   */
  private final CollectionMap<Integer, Integer> dependenciesMap;
  /**
   * An ordered set of positions of the dependencies.
   */
  private final int[] dependenciesPositions;
  /**
   * The actual dpeendencies, if available.
   */
  private transient List<? extends PositionalElement<?>> dependencies;
  /**
   * The actual dpeendencies, if available.
   */
  private transient Map<Integer, PositionalElement<?>> dependenciesbyPosition;

  /**
   * 
   * @param nbDependencies the total number of dependencies for all the tasks.
   * @param dependenciesMap mapping of each task position to the positions of its dependencies.
   * @param dependenciesPositions an ordered set of positions of the dependencies.
   */
  public TaskGraphInfo(final int nbDependencies, final CollectionMap<Integer, Integer> dependenciesMap, final int[] dependenciesPositions) {
    this.nbDependencies = nbDependencies;
    this.dependenciesMap = dependenciesMap;
    this.dependenciesPositions = dependenciesPositions;
  }

  /**
   * @return the total number of dependencies for all the tasks. 
   */
  public int getNbDependencies() {
    return nbDependencies;
  }

  /**
   * @return a mapping of each task position to the positions of its dependencies.
   */
  public CollectionMap<Integer, Integer> getDependenciesMap() {
    return dependenciesMap;
  }

  /**
   * @return an ordered set of positions of the dependencies.
   */
  public int[] getDependenciesPositions() {
    return dependenciesPositions;
  }

  /**
   * @return the actual dpeendencies, if available.
   */
  public List<? extends PositionalElement<?>> getDependencies() {
    return dependencies;
  }

  /**
   * Set the actual dpeendencies, if available.
   * @param dependencies the dependencies to set.
   */
  public void setDependencies(final List<? extends PositionalElement<?>> dependencies) {
    this.dependencies = dependencies;
    dependenciesbyPosition = new HashMap<>(dependencies.size());
    dependencies.forEach(dep -> dependenciesbyPosition.put(dep.getPosition(), dep));
  }

  /**
   * Get the dependency at the specified position.
   * @param position the position of the depe,de,cy tyo lookup.
   * @return a {@link PositionalElement}, or {@code null} if there is o dependency at the psecified position.
   */
  public PositionalElement<?> getDependencyAt(final int position) {
    return (dependenciesbyPosition == null) ? null : dependenciesbyPosition.get(position);
  }
}
