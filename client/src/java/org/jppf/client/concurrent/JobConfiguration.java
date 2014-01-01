/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.client.concurrent;

import java.util.List;

import org.jppf.client.event.JobListener;
import org.jppf.client.persistence.JobPersistence;
import org.jppf.node.protocol.*;
import org.jppf.task.storage.DataProvider;

/**
 * This interface defines the properties that can be provided to a task submitted by a {@link JPPFExecutorService}..
 * These properties include:
 * <ul>
 * <li>the job's {@link org.jppf.node.protocol.JobSLA SLA}</li>
 * <li>the job's {@link org.jppf.node.protocol.JobMetadata metadata}</li>
 * <li>the job's {@link org.jppf.client.persistence.JobPersistence persistence manager}</li>
 * </ul>
 * @author Laurent Cohen
 */
public interface JobConfiguration
{
  /**
   * Get the service level agreement between the job and the server.
   * @return an instance of {@link JobSLA}.
   */
  JobSLA getSLA();

  /**
   * Get the service level agreement between the job and the client.
   * @return an instance of {@link JobClientSLA}.
   */
  JobClientSLA getClientSLA();

  /**
   * Get the user-defined metadata associated with this job.
   * @return a {@link JobMetadata} instance.
   */
  JobMetadata getMetadata();

  /**
   * Get the persistence manager that enables saving and restoring the state of this job.
   * @return a {@link JobPersistence} instance.
   * @param <T> the type of the keys used by the persistence manager.
   */
  <T> JobPersistence<T> getPersistenceManager();

  /**
   * Set the persistence manager that enables saving and restoring the state of this job.
   * @param persistenceManager a {@link JobPersistence} instance.
   * @param <T> the type of the keys used by the persistence manager.
   */
  <T> void setPersistenceManager(final JobPersistence<T> persistenceManager);

  /**
   * Get the job's data provider.
   * @return a {@link DataProvider} instance.
   */
  DataProvider getDataProvider();

  /**
   * Set the job's data provider.
   * @param dataProvider a {@link DataProvider} instance.
   */
  void setDataProvider(DataProvider dataProvider);

  /**
   * Add a listener to the list of job listeners.
   * @param listener a {@link JobListener} instance.
   */
  void addJobListener(JobListener listener);

  /**
   * Remove a listener from the list of job listeners.
   * @param listener a {@link JobListener} instance.
   */
  void removeJobListener(JobListener listener);

  /**
   * Get all the job listeners added to this job configuration. 
   * @return a list of {@link JobListener} instances.
   */
  List<JobListener> getAllJobListeners();
}
