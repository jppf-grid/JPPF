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

package org.jppf.load.balancer.persistence;

import java.util.List;

/**
 * This interface must be implemented by services that perform persistence of load-balancers state.
 * The configuration of an implementation is performed in the JPPF configuration as follows:<br/>
 * {@code jppf.load.balancing.persistence = <persistence_implementation_class_name> [param1 ... paramN]}
 * <p>The implementation class must declare either a no-arg constructor or a constructor that takes a {@code String[]} or {@code String...} parameter, or both.
 * The space-separated optional parameters allow setting up the persistence implementation from the JPPF configuration.
 * They could be used for instance to specify the root directory for a file-based implementation, or JDBC connection parameters, but are not limited to these.
 * <p>An implementation that only declares a no-args constructor may still receive parameters if it declares a {@code void setParameters(String...params)} method.
 * @author Laurent Cohen
 * @since 6.0
 */
public interface LoadBalancerPersistence {
  /**
   * Load the state of a load balancer from the persistence sstore.
   * @param info a {@link LoadBalancerPersistenceInfo} object representing the load balancer and its state.
   * @return an object representing the load balancer state, or {@code null} if no entry exists in the persistence store for the specified node identifier.
   * @throws LoadBalancerPersistenceException if any erorr occurs during the persistence operation.
   */
  Object load(LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException;

  /**
   * Store a load balancer to the persistence sstore.
   * @param info a {@link LoadBalancerPersistenceInfo} object representing the load balancer and its state.
   * @throws LoadBalancerPersistenceException if any erorr occurs during the persistence operation.
   */
  void store(LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException;

  /**
   * Delete the specified load-balancer state(s) from the persistence store.
   * <p>The {@code info} parameter embeds both the scope and identifiers for the artifacts to delete:
   * <ul>
   * <li>if {@code info} is {@code null} or both {@code info.getChannelID()} and {@code info.getAlgorithmID()} are {@code null}, then all entries in the persistence store are deleted</li>
   * <li>if only {@code info.getAlgorithmID()} is {@code null}, then the states of all algorithm for the specified channel are deleted</li>
   * <li>if only {@code info.getChannelID()} is {@code null}, then the states of the specified algorithm are deleted for all the channels</li>
   * <li>if neither {@code info.getChannelID()} nor {@code info.getAlgorithmID()} are {@code null}, then only the state of the specified algorithm for the specified channel is deleted</li>
   * </ul> 
   * @param info encapsulates information about the artifacts to delete.
   * @throws LoadBalancerPersistenceException if any erorr occurs during the persistence operation.
   */
  void delete(LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException;
  
  /**
   * List all entries in the persistence store.
   * <p>The {@code info} parameter embeds both the scope and identifiers for the artifacts to list:
   * <ul>
   * <li>if {@code info} is {@code null} or both {@code info.getChannelID()} and {@code info.getAlgorithmID()} are {@code null}, then all channelIDs in the persistence store are returned</li>
   * <li>if only {@code info.getAlgorithmID()} is {@code null}, then all the algorithm IDs for the specified channel are returned</li>
   * <li>if only {@code info.getChannelID()} is {@code null}, then the the IDs of the channels that have a persisted state for the algorithm are returned</li>
   * <li>if neither {@code info.getChannelID()} nor {@code info.getAlgorithmID()} are {@code null}, then the specified algorithmID is returned (list with a single entry)
   * if the specified channel has an entry for it, otherwise an empty list must be returned</li>
   * </ul> 
   * @param info encapsulates information about the artifacts to delete.
   * @return a list of nodeIDs or algorithmIDs, depending on the input parameter.
   * @throws LoadBalancerPersistenceException if any erorr occurs during the persistence operation.
   */
  List<String> list(LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException;
}
