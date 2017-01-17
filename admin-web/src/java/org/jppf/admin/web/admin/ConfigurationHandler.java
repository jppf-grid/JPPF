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

import java.io.Reader;

import org.jppf.admin.web.JPPFWebConsoleApplication;
import org.jppf.admin.web.settings.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class holds the administrative data, such as JPPF client config, SSL settings and server discovery settings.
 * @author Laurent Cohen
 */
public class ConfigurationHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ConfigurationHandler.class);
  /**
   * A hash of the name assigned to this configuration.
   */
  private final ConfigType type;
  /**
   * The persistence manager.
   */
  private final Persistence persistence;
  /**
   * The JPPF client configuration.
   */
  private TypedProperties config;

  /**
   * @param type the type of this config.
   */
  public ConfigurationHandler(final ConfigType type) {
    this.type = type;
    this.persistence = JPPFWebConsoleApplication.get().getPersistence();
    load();
  }

  /**
   * @return the JPPF client configuration.
   */
  public synchronized TypedProperties getProperties() {
    return config;
  }

  /**
   * Set the JPPF client configuration.
   * @param config the onifguration to set.
   * @return {@code this}, for method call chaining.
   */
  public synchronized ConfigurationHandler setProperties(final TypedProperties config) {
    this.config = config;
    return this;
  }

  /**
   * Load the configuration.
   * @return {@code this}, for method call chaining.
   */
  public synchronized ConfigurationHandler load() {
    try {
      TypedProperties props = persistence.loadProperties(type.getHash());
      if (props.isEmpty()) {
        try (Reader reader = FileUtils.getFileReader(type.getDefaultPath())) {
          props.load(reader);
        }
      }
      config = props;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return this;
  }

  /**
   * Save the configuration.
   * @return {@code this}, for method call chaining.
   */
  public synchronized ConfigurationHandler save() {
    try {
      persistence.saveProperties(type.getHash(), config);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return this;
  }
}
