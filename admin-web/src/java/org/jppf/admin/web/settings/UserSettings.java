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

import org.jppf.admin.web.JPPFWebConsoleApplication;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Persistent user settings. The settings are simple properties, persisted via a persistence handler,
 * which is an instance of an implementation of {@link Persistence}.  
 * @author Laurent Cohen
 */
public class UserSettings {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(UserSettings.class);
  /**
   * The user name.
   */
  private final String user;
  /**
   * The user name.
   */
  private final String userHash;
  /**
   * The settings.
   */
  private final TypedProperties properties = new TypedProperties();
  /**
   * The perisstence handler for these settings.
   */
  private final Persistence persistence;

  /**
   * Initialize with the specified user.
   * @param user th euser name.
   */
  public UserSettings(final String user) {
    this.user = user;
    this.userHash = CryptoUtils.computeHash(user, "SHA-256");
    this.persistence = JPPFWebConsoleApplication.get().getPersistenceFactory().newPersistence();
  }

  /**
   * Load the settings  with the persistence handler.
   * @return these user settings.
   */
  public UserSettings load() {
    try  {
      TypedProperties props = persistence.loadProperties(userHash);
      if (!props.isEmpty()) {
        properties.clear();
        properties.putAll(props);
      }
    } catch(Exception e) {
      log.error("error loading settings for user {} : {}", user, ExceptionUtils.getStackTrace(e));
    }
    return this;
  }

  /**
   * Save the settings  with the persistence handler.
   */
  public void save() {
    try  {
      persistence.saveProperties(userHash, properties);
    } catch(Exception e) {
      log.error("error saving settings for user {} : {}", user, ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * @return the user settings.
   */
  public TypedProperties getProperties() {
    return properties;
  }
}
