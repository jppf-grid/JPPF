/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.admin.web.settings;

import org.jppf.JPPFRuntimeException;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Factory class for persistence services based on an implementation class
 * whose name is configured in the init paremeters of the Wicket filter in the web.xml.
 * @author Laurent Cohen
 */
public class PersistenceFactory {
  
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(PersistenceFactory.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Name of the persistence class to instantiate.
   */
  private final String persistenceClassName;

  /**
   * Create a new persistence factory.
   * @param persistenceClassName the name of the persistence class to instantiate.
   */
  PersistenceFactory(final String persistenceClassName) {
    this.persistenceClassName = persistenceClassName;
  }

  /**
   * Create a new instance of this persistence factory.
   * @param persistenceClassName the name of the persistence class to instantiate.
   * @return an instance of {@link PersistenceFactory}.
   */
  public static PersistenceFactory newInstance(final String persistenceClassName) {
    return new PersistenceFactory(persistenceClassName);
  }

  /**
   * @return a new persistence instance based on the class name configured in the init paremeters of the Wicket filter in the web.xml
   */
  public Persistence newPersistence() {
    try {
      ClassLoader cl = getClass().getClassLoader();
      return (Persistence) Class.forName(persistenceClassName, true, cl).newInstance();
    } catch (Exception e) {
      if (debugEnabled) log.debug("error creating persistence for className = {}:\n{}", persistenceClassName, ExceptionUtils.getStackTrace(e));
      throw new JPPFRuntimeException(e);
    }
  }
}
