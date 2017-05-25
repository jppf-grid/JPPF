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

import org.jppf.utils.ReflectionHelper;
import org.jppf.utils.configuration.JPPFProperties;

/**
 * Factory class which instantites persistence implementations based on the JPPF configuration.
 * @author Laurent Cohen
 * @exclude
 */
public final class JobPersistenceFactory {
  /**
   * Singleton instance of this class.
   */
  private static final JobPersistenceFactory INSTANCE = new JobPersistenceFactory();
  /**
   * The configured persistence.
   */
  private final JobPersistence persistence;

  /**
   * 
   */
  private JobPersistenceFactory() {
    persistence = ReflectionHelper.invokeDefaultOrStringArrayConstructor(JobPersistence.class, JPPFProperties.JOB_PERSISTENCE);
  }

  /**
   * @return the singleton instance of this class.
   */
  public static JobPersistenceFactory getInstance() {
    return INSTANCE;
  }

  /**
   * @return the configured persistence.
   */
  public JobPersistence getPersistence() {
    return persistence;
  }
}
