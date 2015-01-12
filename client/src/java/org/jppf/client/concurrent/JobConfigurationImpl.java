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

package org.jppf.client.concurrent;

import org.jppf.client.event.JobListener;
import org.jppf.client.persistence.JobPersistence;
import org.jppf.node.protocol.*;

/**
 * 
 * @author Laurent Cohen
 */
class JobConfigurationImpl extends AbstractJobConfiguration
{
  /**
   * Default constructor.
   */
  JobConfigurationImpl()
  {
    super();
  }

  /**
   * Copy constructor.
   * @param sla the sla configuration.
   * @param metadata the metadata configuration to use.
   * @param persistenceManager the persistence manager to use.
   */
  JobConfigurationImpl(final JobSLA sla, final JobMetadata metadata, final JobPersistence persistenceManager)
  {
    super(sla, metadata, persistenceManager);
  }

  /**
   * Copy constructor.
   * @param config the configuration from which ot initialize this job configuration.
   */
  JobConfigurationImpl(final JobConfiguration config)
  {
    this(config.getSLA(), config.getMetadata(), config.getPersistenceManager());
    this.setClientSLA(config.getClientSLA());
    this.setClientSLA(config.getClientSLA());
    for (JobListener listener: config.getAllJobListeners()) this.addJobListener(listener);
  }
}
