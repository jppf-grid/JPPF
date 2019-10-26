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

package org.jppf.load.balancer.persistence;

import java.util.List;

import org.jppf.management.doc.*;

/**
 * Management interface for the load-balancer perisstence store.
 * <p>All algorithm names used as parameter in the methods of this interface are the readable clear-text algorithm names.
 * @author Laurent Cohen
 */
public interface LoadBalancerPersistenceManagement {
  /**
   * @return {@code true} if load-balancer persistence is enabled, {@code false} otherwise.
   */
  @MBeanDescription("wether the load-balancer persistence is enabled")
  public boolean isPersistenceEnabled();

  /**
   * List all the channels that have an entry in the persistence store.
   * @return a list of channel identifiers, possibly empty, but never {@code null}.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  @MBeanDescription("list all the channels that have an entry in the persistence store")
  List<String> listAllChannels() throws LoadBalancerPersistenceException;

  /**
   * List all the algorithms for which the specified channel has an entry in the persistence store.
   * @param channelID the identifier of the channel for which to list the algorithms.
   * @return a list of algorithm names, possibly empty, but never {@code null}.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  @MBeanDescription("list all the algorithms for which the specified channel has an entry in the persistence store")
  List<String> listAlgorithms(@MBeanParamName("channelID") String channelID) throws LoadBalancerPersistenceException;
  
  /**
   * List all the channels that have an entry in the persistence store for the specified algorithm.
   * @param algorithm the name of the algorithm to lookup.
   * @return a list of node identifiers, possibly empty, but never {@code null}.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  @MBeanDescription("list all the channels that have an entry in the persistence store for the specified algorithm")
  List<String> listAllChannelsWithAlgorithm(@MBeanParamName("algorithm") String algorithm) throws LoadBalancerPersistenceException;

  /**
   * Determine whether the specified channel has an entry for the specified algorithm in the persistence store.
   * @param channelID the identifier of the channel to check.
   * @param algorithm the name of the algorithm to lookup.
   * @return {@code true} if the node has an entry for the algorithm, {@code false} otherwise.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  @MBeanDescription("determine whether the specified channel has an entry for the specified algorithm in the persistence store")
  boolean hasAlgorithm(@MBeanParamName("channelID") String channelID, @MBeanParamName("algorithm") String algorithm) throws LoadBalancerPersistenceException;
  
  /**
   * Delete all entries in the persistece store.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  @MBeanDescription("delete all entries in the persistece store")
  void deleteAll() throws LoadBalancerPersistenceException;

  /**
   * Delete all entries for the specified channel.
   * @param channelID identifier of the channel to delete.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  @MBeanDescription("delete all entries for the specified channel")
  void deleteChannel(@MBeanParamName("channelID") String channelID) throws LoadBalancerPersistenceException;

  /**
   * Delete the specified algorithm state from all the channels that have it.
   * @param algorithm the name of the algorithm to delete.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  @MBeanDescription("delete the specified algorithm state from all the channels that have it")
  void deleteAlgorithm(@MBeanParamName("algorithm") String algorithm) throws LoadBalancerPersistenceException;

  /**
   * Delete the specified algorithm state from the specified channel.
   * @param channelID identifier of the channel from which to delete the algorithm state.
   * @param algorithm the name of the algorithm to delete.
   * @throws LoadBalancerPersistenceException if any error occurs.
   */
  @MBeanDescription("delete the specified algorithm state from the specified channel")
  void delete(@MBeanParamName("channelID") String channelID, @MBeanParamName("algorithm") String algorithm) throws LoadBalancerPersistenceException;

  /**
   * Get the number of persistence operations, including load, store, delete and list, that have started but not yet completed.
   * @return the number of uncompleted operations.
   */
  @MBeanDescription("the number of persistence operations, including load, store, delete and list, that have started but not yet completed")
  int getUncompletedOperations();
}
