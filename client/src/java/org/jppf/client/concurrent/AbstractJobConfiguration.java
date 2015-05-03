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

import java.util.*;

import org.jppf.client.event.JobListener;
import org.jppf.client.persistence.JobPersistence;
import org.jppf.node.protocol.*;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;

/**
 * Abstract implementation of the <code>JobConfiguration</code> interface.
 * @author Laurent Cohen
 * @exclude
 */
abstract class AbstractJobConfiguration implements JobConfiguration {
  /**
   * The service level agreement between the job and the server.
   */
  protected JobSLA jobSLA = new JPPFJobSLA();
  /**
   * The service level agreement between the job and the server.
   */
  protected JobClientSLA jobClientSLA = new JPPFJobClientSLA();
  /**
   * The user-defined metadata associated with this job.
   */
  protected JobMetadata jobMetadata = new JPPFJobMetadata();
  /**
   * The persistence manager that enables saving and restoring the state of this job.
   */
  protected JobPersistence<?> persistenceManager = null;
  /**
   * The data provider to set onto the job.
   */
  protected DataProvider dataProvider = null;
  /**
   * The list of listeners to register with the job.
   */
  protected List<JobListener> listeners = new LinkedList<>();
  /**
   * A list of class loaders used to load the classes needed to run the jobs.
   */
  protected final List<ClassLoader> classLoaders = new ArrayList<>();

  /**
   * Default constructor.
   */
  protected AbstractJobConfiguration() {
  }

  @Override
  public JobSLA getSLA() {
    return jobSLA;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @param jobSLA an instance of <code>JPPFJobSLA</code>.
   */
  public void setSLA(final JobSLA jobSLA) {
    this.jobSLA = jobSLA;
  }

  @Override
  public JobClientSLA getClientSLA() {
    return jobClientSLA;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @param jobClientSLA an instance of <code>JPPFJobSLA</code>.
   */
  public void setClientSLA(final JobClientSLA jobClientSLA) {
    this.jobClientSLA = jobClientSLA;
  }

  @Override
  public JobMetadata getMetadata() {
    return jobMetadata;
  }

  /**
   * Set this job's metadata.
   * @param jobMetadata a {@link JPPFJobMetadata} instance.
   */
  public void setMetadata(final JobMetadata jobMetadata) {
    this.jobMetadata = jobMetadata;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> JobPersistence<T> getPersistenceManager() {
    return (JobPersistence<T>) persistenceManager;
  }

  @Override
  public <T> void setPersistenceManager(final JobPersistence<T> persistenceManager) {
    this.persistenceManager = persistenceManager;
  }

  @Override
  public DataProvider getDataProvider() {
    return dataProvider;
  }

  @Override
  public void setDataProvider(final DataProvider dataProvider) {
    this.dataProvider = dataProvider;
  }

  @Override
  public void addJobListener(final JobListener listener)
  {
    synchronized(listeners) {
      listeners.add(listener);
    }
  }

  @Override
  public void removeJobListener(final JobListener listener) {
    synchronized(listeners) {
      listeners.remove(listener);
    }
  }

  @Override
  public List<JobListener> getAllJobListeners() {
    synchronized(listeners) {
      return listeners;
    }
  }

  @Override
  public List<ClassLoader> getClassLoaders() {
    return classLoaders;
  }
}
