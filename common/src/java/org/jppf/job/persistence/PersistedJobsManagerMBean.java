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

import java.util.*;

import org.jppf.job.JobSelector;

/**
 * This MBean manages jobs persisted by the driver.
 * @author Laurent Cohen
 * @exclude
 */
public interface PersistedJobsManagerMBean {
  /**
   * Name of the job persistence MBean.
   */
  String MBEAN_NAME = "org.jppf:name=job_persistence,type=driver";

  /**
   * Get the uuids of all persisted jobs that match the selector.
   * @param selector used to filter the jobs.
   * @return a list of job uuids, possiblu empty if no job matches the selector.
   * @throws Exception if any error occurs.
   */
  List<String> getPersistedJobUuids(JobSelector selector) throws Exception;

  /**
   * Get the specified object of the persisted job with the specified uuid.
   * @param uuid the uuid of the persisted job to lookup.
   * @param type the type of object to lookup.
   * @param position the position of the object in the job, if a position applies.
   * @return the object in serialized format.
   * @throws Exception if any error occurs.
   */
  Object getPersistedJobObject(String uuid, PersistenceObjectType type, int position) throws Exception;

  /**
   * Get the positions of all tasks and task results for the specified persisted job.
   * @param uuid the uuid of the persisted job to lookup.
   * @return an array of int arrays, where the first array represents the positions of all tasks before execution,
   * and the second array represents the positions of all task results. The second array may be empty if no task result was received.
   * @throws Exception if any error occurs.
   */
  int[][] getPersistedJobPositions(String uuid) throws Exception;

  /**
   * Delete all the persisted jobs that match the selector from the persistence store.
   * @param selector used to filter the jobs.
   * @return a list of actually deleted job uuids, possibly empty if no job matches the selector.
   * @throws Exception if any error occurs.
   */
  List<String> deletePersistedJobs(JobSelector selector) throws Exception;

  /**
   * Determine whether the specified job is persisted.
   * @param uuid the uuid of the job to check.
   * @return {@code true} if the job is persisted, {@code false} otherwise.
   * @throws Exception if any error occurs while accessing the persistence store.
   */
  boolean isJobersisted(String uuid) throws Exception;

  /**
   * Determines whether the job has completed and all execution results are available.
   * @param uuid the UUID of the jonb to check.
   * @return {@code true} if the job has completed, {@code false} otherwise.
   * @throws Exception if any error occurs while communicating with the driver. 
   */
  public boolean isJobComplete(final String uuid) throws Exception;

  /**
   * Request loading of the specified job's elements.
   * @param infos information on the elements to load.
   * @return a load request id, to reuse in later method calls.
   * @throws Exception if any error occurs.
   */
  public long requestLoad(final Collection<PersistenceInfo> infos) throws Exception;

  /**
   * Get the specified object of the already-loaded persisted job with the specified uuid.
   * @param requestId the id of the prior load request.
   * @param uuid the uuid of the persisted job to lookup.
   * @param type the type of object to lookup.
   * @param position the position of the object in the job, if a position applies.
   * @return the object in serialized format.
   * @throws Exception if any error occurs.
   */
  public Object getPersistedJobObject(final long requestId, final String uuid, final PersistenceObjectType type, final int position) throws Exception;

  /**
   * Get the specified object of the already-loaded persisted job with the specified uuid.
   * @param requestId the id of the prior load request to delete.
   * @return {@code true} if the delete succeeded, {@code false} otherwise.
   * @throws Exception if any error occurs.
   */
  public boolean deleteLoadRequest(final long requestId) throws Exception;
}
