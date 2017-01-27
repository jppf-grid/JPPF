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
   * @param persistenceClassName the name of the persistence class to instantiate.
   * @return a new persistence instance based on the class name configured in the init paremeters of the Wicket filter in the web.xml
   */
  public static Persistence newPersistence(final String persistenceClassName) {
    try {
      ClassLoader cl = PersistenceFactory.class.getClassLoader();
      return (Persistence) Class.forName(persistenceClassName, true, cl).newInstance();
    } catch (Exception e) {
      if (debugEnabled) log.debug("error creating persistence for className = {}:\n{}", persistenceClassName, ExceptionUtils.getStackTrace(e));
      throw new JPPFRuntimeException(e);
    }
  }
}
