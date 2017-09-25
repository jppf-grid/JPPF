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
 * Management interface for the load-balancer perisstence store.
 * <p>All algorithm names used as parameter in the methods of this interface are the readable clear-text algorithm names.
 * @author Laurent Cohen
 */
public interface LoadBalancerPersistenceManagement {
  /**
   * @return {@code true} if load-balancer persisted is enabled, {@code false} otherwise.
   */
  public boolean isPersistenceEnabled();

  /**
   * List all the channels that have an entry in the persistence store.
   * @return a list of channel identifiers, possibly empty, but never {@code null}.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  List<String> listAllChannels() throws LoadBalancerPersistenceException;

  /**
   * List all the algorithms for which the specified channel has an entry in the persistence store.
   * @param channelID the identifier of the channel for which to list the algorithms.
   * @return a list of algorithm names, possibly empty, but never {@code null}.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  List<String> listAlgorithms(String channelID) throws LoadBalancerPersistenceException;
  
  /**
   * List all the channels that have an entry in the persistence store for the specified algorithm.
   * @param algorithm the name of the algorithm to lookup.
   * @return a list of node identifiers, possibly empty, but never {@code null}.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  List<String> listAllChannelsWithAlgorithm(String algorithm) throws LoadBalancerPersistenceException;

  /**
   * Determine whether the specified channel has an entry for the specified algorithm in the persistence store.
   * @param channelID the identifier of the channel to check.
   * @param algorithm the name of the algorithm to lookup.
   * @return {@code true} if the node has an entry for the algorithm, {@code false} otherwise.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  boolean hasAlgorithm(String channelID, String algorithm) throws LoadBalancerPersistenceException;
  
  /**
   * Delete all entries in the persistece store.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  void deleteAll() throws LoadBalancerPersistenceException;

  /**
   * Delete all entries for the specified channel.
   * @param channelID identifier of the channel to delete.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  void deleteChannel(String channelID) throws LoadBalancerPersistenceException;

  /**
   * Delete the specified algorithm state from all the channels that have it.
   * @param algorithm the name of the algorithm to delete.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  void deleteAlgorithm(String algorithm) throws LoadBalancerPersistenceException;

  /**
   * Delete the specified algorithm state from the specified channel.
   * @param channelID identifier of the channel from which to delete the algorithm state.
   * @param algorithm the name of the algorithm to delete.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  void delete(String channelID, String algorithm) throws LoadBalancerPersistenceException;
}
