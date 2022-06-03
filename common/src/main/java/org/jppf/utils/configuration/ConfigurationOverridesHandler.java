/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.utils.configuration;

import java.io.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class handles the loading and saving of temporary configuration overrides files.
 * @author Laurent Cohen
 * @exclude
 */
public class ConfigurationOverridesHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ConfigurationOverridesHandler.class);
  /**
   * The node configuration.
   */
  private final TypedProperties config;

  /**
   * 
   */
  public ConfigurationOverridesHandler() {
    this(JPPFConfiguration.getProperties());
  }

  /**
   * @param config the node configuration.
   */
  public ConfigurationOverridesHandler(final TypedProperties config) {
    this.config = config;
  }

  /**
   * Save the configuration overrides file.
   * @param overrides the config overrides to save.
   */
  public void save(final TypedProperties overrides) {
    if ((overrides == null) || overrides.isEmpty()) return;
    try {
      File file = config.get(JPPFProperties.CONFIG_OVERRIDES_PATH);
      if (file != null) {
        file = file.getAbsoluteFile();
        if (file.exists()) file.delete();
        else if (file.getParentFile() != null) file.getParentFile().mkdirs();
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
          overrides.store(bos, "JPPF configuration overrides");
        }
      }
      else log.error("config overrides file is null");
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Load the config overrides file. If the file exists, it is deleted after it is loaded.
   * @param delete whether to delete the config overrides file after loading.
   * @return the overrides as a {@link TypedProperties} instance.
   */
  public TypedProperties load(final boolean delete) {
    final TypedProperties overrides = new TypedProperties();
    try {
      final File file = config.get(JPPFProperties.CONFIG_OVERRIDES_PATH);
      if ((file != null) && file.exists()) {
        try {
          try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            overrides.loadAndResolve(reader);
          }
        } finally {
          if (delete) file.delete();
        }
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    return overrides;
  }
}
