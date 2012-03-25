/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import org.jppf.client.persistence.JobPersistence;
import org.jppf.node.protocol.*;
import org.jppf.server.protocol.*;

/**
 * Abstract implemzntation of the <code>JobConfiguration</code> interface.
 * @author Laurent Cohen
 * @exclude
 */
abstract class AbstractJobConfiguration implements JobConfiguration
{
  /**
   * The service level agreement between the job and the server.
   */
  protected JobSLA jobSLA = new JPPFJobSLA();
  /**
   * The user-defined metadata associated with this job.
   */
  protected JobMetadata jobMetadata = new JPPFJobMetadata();
  /**
   * The persistence manager that enables saving and restoring the state of this job.
   */
  protected JobPersistence<?> persistenceManager = null;

  /**
   * Default constructor.
   */
  protected AbstractJobConfiguration()
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JobSLA getSLA()
  {
    return jobSLA;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @param jobSLA an instance of <code>JPPFJobSLA</code>.
   */
  public void setSLA(final JobSLA jobSLA)
  {
    this.jobSLA = jobSLA;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JobMetadata getMetadata()
  {
    return jobMetadata;
  }

  /**
   * Set this job's metadata.
   * @param jobMetadata a {@link JPPFJobMetadata} instance.
   */
  public void setMetadata(final JobMetadata jobMetadata)
  {
    this.jobMetadata = jobMetadata;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> JobPersistence<T> getPersistenceManager()
  {
    return (JobPersistence<T>) persistenceManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> void setPersistenceManager(final JobPersistence<T> persistenceManager)
  {
    this.persistenceManager = persistenceManager;
  }
}
