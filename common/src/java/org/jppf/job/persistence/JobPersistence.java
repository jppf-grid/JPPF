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
import java.util.List;

/**
 * This interface must be implemnted by services that perform jobs persistence in the driver.
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
   * Store the specified job elemen.
   * @param info information on the persisted object to load.
   * @throws JobPersistenceException if any erorr occurs during the persistence operation.
   */
  void store(PersistenceInfo info) throws JobPersistenceException;

  /**
   * Load the specified job element.
   * @param info information on the persisted object to store.
   * @return an input stream providing the serialized job header.
   * @throws JobPersistenceException if any erorr occurs during the persistence operation.
   */
  InputStream load(PersistenceInfo info) throws JobPersistenceException;

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
   * Delete the persisted jobs with the psecified UUID.
   * @param jobUuid the UUID of the job to load.
   * @throws JobPersistenceException if any erorr occurs during the persistence operation.
   */
  void deleteJob(String jobUuid) throws JobPersistenceException;
}
