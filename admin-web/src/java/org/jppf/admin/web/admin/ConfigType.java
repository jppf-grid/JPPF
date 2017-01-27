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

package org.jppf.admin.web.admin;

import org.jppf.utils.CryptoUtils;

/**
 * The types of configurations handled in the admin panels.
 */
public enum ConfigType {
  /**
   * The JPPF client configuration
   */
  CLIENT("jppf_client_config", "admin.config", "jppf.properties"),
  /**
   * The SSL/TLS configuration
   */
  SSL("jppf_ssl_config", "admin.ssl", "ssl.properties");

  /**
   * A hash of the name given to the config.
   */
  private final String nameHash;
  /**
   * Prefix for component ids in the corresponding panel.
   */
  private final String prefix;
  /**
   * Default path for the configuration file.
   */
  private final String defaultPath;

  /**
   * @param name the name of the config.
   * @param prefix the prefix for component ids in the corresponding panel.
   * @param defaultPath the default path for the configuration file.
   */
  private ConfigType(final String name, final String prefix, final String defaultPath) {
    this.nameHash = CryptoUtils.computeHash(name, "SHA-256");
    this.prefix = prefix;
    this.defaultPath = defaultPath;
  }

  /**
   * @return a hash of the name given to the config.
   */
  public String getHash() {
    return nameHash;
  }

  /**
   * @return the prefix for component ids in the corresponding panel.
   * The prefix does <i>not</i> end with a dot.
   */
  public String getPrefix() {
    return prefix;
  }

  /**
   * @return the default path for the configuration file.
   */
  public String getDefaultPath() {
    return defaultPath;
  }
}
