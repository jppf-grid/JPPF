/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.client.persistence;

import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;

/**
 * Interface for facilities managing the persistence of jobs and their state.
 * It is intended to enable the storage of jobs in a persistent store, so as to be able
 * to resubmit them at a later time, if the client terminated for any reason before the job completed.
 * <p>The state of a job is essentially made of the tasks initially submitted, associated with the list
 * of tasks that completed, such as captured by the job's <code>TaskResultListener</code>.
 * The class {@link org.jppf.client.JPPFResultCollector JPPFResultCollector} does that automatically,
 * and it is recommended to use it or a subclass that calls <code>super.resultsReceived()</code> in its implementation.
 * <p>The underlying physical store that is used is implementation-dependent.
 * For example: file system, database, cloud storage facility, etc.
 * @param <K> the type of the keys used to identify and locate jobs in the persistence store.
 * @author Laurent Cohen
 */
public interface JobPersistence<K>
{
  /**
   * Compute the key for the specified job. The contract for this method is that
   * it is idempotent, meaning that calling this method for the same job instance
   * should always return the same key.
   * @param job the job for which to get a key.
   * @return A key used to identify and locate the job int he persistent store.
   */
  K computeKey(JPPFJob job);

  /**
   * Get the keys of all jobs in the persistence store.
   * @return a collection of objects representing the keys.
   * @throws JobPersistenceException if any error occurs while retrieving the keys.
   */
  Collection<K> allKeys() throws JobPersistenceException;

  /**
   * Load a job from the persistence store given its key.
   * @param key the key allowing to locate the job in the persistence store.
   * @return a JPPFJob instance, retrieved from the store.
   * @throws JobPersistenceException if any error occurs while loading the job.
   */
  JPPFJob loadJob(K key) throws JobPersistenceException;

  /**
   * Store the specified tasks of the specified job with the specified key.
   * @param key the key allowing to locate the job in the persistence store.
   * @param job the job to store.
   * @param tasks the newly received completed tasks, may be used to only store the delta for better performance.
   * @throws JobPersistenceException if any error occurs while storing the job.
   */
  void storeJob(K key, JPPFJob job, List<Task<?>> tasks) throws JobPersistenceException;

  /**
   * Delete the job with the specified key from the persistence store.
   * @param key the key allowing to locate the job in the persistence store.
   * @throws JobPersistenceException if any error occurs while deleting the job.
   */
  void deleteJob(K key) throws JobPersistenceException;

  /**
   * Close this store and release any used resources.
   */
  void close();
}
