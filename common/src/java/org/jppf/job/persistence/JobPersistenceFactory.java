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

import org.jppf.job.persistence.impl.DefaultFilePersistence;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Factory class which instantites persistence implementations based on the JPPF configuration.
 * @author Laurent Cohen
 * @exclude
 */
public final class JobPersistenceFactory {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JobPersistenceFactory.class);
  /**
   * Singleton instance of this class.
   */
  private static final JobPersistenceFactory INSTANCE = new JobPersistenceFactory();
  /**
   * The configured persistence.
   */
  private final JobPersistence persistence;

  /**
   * Not instantiable from another class.
   */
  private JobPersistenceFactory() {
    JobPersistence tmp = null;
    try {
      tmp = ReflectionHelper.invokeDefaultOrStringArrayConstructor(JobPersistence.class, JPPFProperties.JOB_PERSISTENCE);
    } catch (Exception e) {
      log.error(String.format("error creating JobPersisitence configured as %s = %s, falling back to %s%n%s", JPPFProperties.JOB_PERSISTENCE.getName(),
        JPPFConfiguration.get(JPPFProperties.JOB_PERSISTENCE), DefaultFilePersistence.class.getName(), ExceptionUtils.getStackTrace(e)));
      try {
        tmp = new DefaultFilePersistence();
      } catch (Exception e2) {
        log.error(String.format("fallback to %s failed, job persistence is disabled%n%s", DefaultFilePersistence.class.getName(), ExceptionUtils.getStackTrace(e2)));
      }
    }
    persistence = tmp;
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
