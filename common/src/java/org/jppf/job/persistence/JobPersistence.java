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

package org.jppf.job.persistence;

import java.io.InputStream;
import java.util.*;

/**
 * This interface must be implemented by services that perform jobs persistence in the driver.
 * The configuration of an implementation is performed in the JPPF configuration as follows:<br/>
 * {@code jppf.job.persistence = <persistence_implementation_class_name> [param1 ... paramN]}
 * <p>The implementation class must declare either a no-arg constructor or a constructor that takes a {@code String[]} or {@code String...} parameter, or both.
 * The space-separated optional parameters allow setting up the persistence implementation from the JPPF configuration.
 * They could be used for instance to specify the root directory for a file-based implementation, or JDBC connection parameters, but are not limited to these.
 * <p>An implementation that only declares a no-args constructor may still receive parameters if it declares a {@code void setParameters(String...params)} method.
 * @author Laurent Cohen
 * @since 6.0
 */
public interface JobPersistence {
  /**
   * Store the specified job elements. All elements are assumed to be part of the same job.
   * @param infos collection of information objects on the job elements to store.
   * @throws JobPersistenceException if any erorr occurs during the persistence operation.
   */
  void store(Collection<PersistenceInfo> infos) throws JobPersistenceException;

  /**
   * Load the specified job elements. All elements are assumed to be part of the same job.
   * @param infos information on the persisted job elements to load.
   * @return an input stream providing the serialized job header.
   * @throws JobPersistenceException if any erorr occurs during the persistence operation.
   */
  List<InputStream> load(Collection<PersistenceInfo> infos) throws JobPersistenceException;

  /**
   * Get the UUIDs of all persisted job.
   * @return a list of strings reprensenting job UUIDs.
   * @throws JobPersistenceException if any erorr occurs during the persistence operation.
   */
  List<String> getPersistedJobUuids() throws JobPersistenceException;

  /**
   * Get the positions of all the tasks in the specified job.
   * @param jobUuid the uuid of the job for which to find the task positions.
   * @return an array of int representing the positions.
   * @throws JobPersistenceException if any erorr occurs during the persistence operation.
   */
  int[] getTaskPositions(String jobUuid) throws JobPersistenceException;

  /**
   * Get the positions of all the task results in the specified job.
   * @param jobUuid the uuid of the job for which to find the task result positions.
   * @return an array of int representing the positions.
   * @throws JobPersistenceException if any erorr occurs during the persistence operation.
   */
  int[] getTaskResultPositions(String jobUuid) throws JobPersistenceException;

  /**
   * Delete the persisted job with the psecified UUID.
   * @param jobUuid the UUID of the job to load.
   * @throws JobPersistenceException if any erorr occurs during the persistence operation.
   */
  void deleteJob(String jobUuid) throws JobPersistenceException;

  /**
   * Determine whether a job is persisted, that is, present in the persistence store.
   * @param jobUuid the UUID of the job to check.
   * @return {@code true} if the job is in the persistence store, {@code false} otherwise.
   * @throws JobPersistenceException if any error occurs while accessing the persistence store.
   */
  boolean isJobPersisted(String jobUuid) throws JobPersistenceException;
}
