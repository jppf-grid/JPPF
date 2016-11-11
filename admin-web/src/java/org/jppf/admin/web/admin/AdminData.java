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

package org.jppf.admin.web.admin;

import org.jppf.admin.web.settings.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class holds the administrative data, such as JPPF client config, SSL settings and server discovery settings.
 * @author Laurent Cohen
 */
public class AdminData {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AdminData.class);
  /**
   * The name from which the config properties file is derived.
   */
  public static final String CONFIG_NAME = "jppf_client_config";
  /**
   * The name from which the config properties file is derived.
   */
  private static final String CONFIG_NAME_HASH = CryptoUtils.computeHash(CONFIG_NAME, "SHA-256");
  /**
   * The persistence manager.
   */
  private final SettingsPersistence persistence = new JPPFFileSettingsPersistence();
  /**
   * The JPPF client configuration.
   */
  private TypedProperties config;

  /**
   * 
   */
  public AdminData() {
    loadConfig();
  }

  /**
   * @return the JPPF client configuration.
   */
  public synchronized TypedProperties getConfig() {
    return config;
  }

  /**
   * Set the JPPF client configuration.
   * @param config the onifguration to set.
   * @return {@code this}, for method call chaining.
   */
  public synchronized AdminData setConfig(final TypedProperties config) {
    this.config = config;
    return this;
  }

  /**
   * Load the client configuration.
   * @return {@code this}, for method call chaining.
   */
  public synchronized AdminData loadConfig() {
    TypedProperties props = new TypedProperties();
    try {
      persistence.load(CONFIG_NAME_HASH, props);
      config = (props.isEmpty()) ? JPPFConfiguration.getProperties() : props;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return this;
  }

  /**
   * Save the client configuration.
   * @return {@code this}, for method call chaining.
   */
  public synchronized AdminData saveConfig() {
    try {
      persistence.save(CONFIG_NAME_HASH, config);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return this;
  }
}
