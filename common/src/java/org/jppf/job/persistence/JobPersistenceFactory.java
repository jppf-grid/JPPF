/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

import java.util.Arrays;

import org.jppf.job.persistence.impl.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
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
   * The configured persistence.
   */
  private final JobPersistence persistence;

  /**
   * Not instantiable from another class.
   * @param config the configuration to use.
   */
  private JobPersistenceFactory(final TypedProperties config) {
    JobPersistence tmp = null;
    try {
      final JPPFProperty<String[]> prop = JPPFProperties.JOB_PERSISTENCE;
      tmp = ReflectionHelper.invokeDefaultOrStringArrayConstructor(JobPersistence.class, prop.getName(), config.get(prop));
    } catch (final Exception e) {
      log.error("error creating JobPersistence configured as {} = {}, falling back to {}\n{}", JPPFProperties.JOB_PERSISTENCE.getName(),
        Arrays.toString(JPPFConfiguration.get(JPPFProperties.JOB_PERSISTENCE)), DefaultFilePersistence.class.getName(), ExceptionUtils.getStackTrace(e));
      try {
        tmp = new DefaultFilePersistence();
      } catch (final Exception e2) {
        log.error("fallback to {} failed, job persistence is disabled\n{}", DefaultFilePersistence.class.getName(), ExceptionUtils.getStackTrace(e2));
      }
    }
    persistence = tmp;
  }

  /**
   * @param config the configuration to use.
   * @return the singleton instance of this class.
   */
  public static JobPersistenceFactory newInstance(final TypedProperties config) {
    return new JobPersistenceFactory(config);
  }

  /**
   * @return the configured persistence.
   */
  public JobPersistence getPersistence() {
    return persistence;
  }
}
