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

import java.security.MessageDigest;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 *
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
  private String userHash;
  /**
   * The settings.
   */
  private final TypedProperties properties = new TypedProperties();
  /**
   * 
   */
  private final SettingsPersistence persistence = new JPPFFileSettingsPersistence();

  /**
   * Initialize with the specified user.
   * @param user th euser name.
   */
  public UserSettings(final String user) {
    super();
    this.user = user;
    this.userHash = getUserHash();
  }

  /**
   * @return these user settings.
   */
  public UserSettings load() {
    try  {
      persistence.load(getUserHash(), properties);
    } catch(Exception e) {
      log.error("error loading settings for user {} : {}", user, ExceptionUtils.getStackTrace(e));
    }
    return this;
  }

  /**
   *
   */
  public void save() {
    try  {
      persistence.save(getUserHash(), properties);
    } catch(Exception e) {
      log.error("error saving settings for user {} : {}", user, ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * 
   * @return the user hash.
   */
  private String getUserHash() {
    if (userHash == null) {
      try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        JPPFBuffer buf = new JPPFBuffer(user);
        digest.update(buf.buffer, 0, buf.length);
        byte[] sig = digest.digest();
        userHash = StringUtils.toHexString(sig);
      } catch (Exception e) {
        log.error("error compputing hash for user {} : {}", user, ExceptionUtils.getStackTrace(e));
      }
    }
    return userHash;
  }

  /**
   * @return the user settings.
   */
  public TypedProperties getProperties() {
    return properties;
  }
}
