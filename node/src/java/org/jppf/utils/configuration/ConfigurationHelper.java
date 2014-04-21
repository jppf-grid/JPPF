/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

/**
 * Utility methods to help access and manipulate the JPPF configuration
 * @author Laurent Cohen
 */
public class ConfigurationHelper {
  /**
   * The configuration to use.
   */
  private final TypedProperties config;

  /**
   * Initialize this helper witht he specified ocnfiguration.
   * @param config the configuration to use.
   */
  public ConfigurationHelper(final TypedProperties config) {
    this.config = config;
  }

  /**
   * Get the integer value of the specified configuration property.<br>
   * If the property is not defined or the value is not in the range <code>[min, max]</code>, the default value is returned.
   * @param name the name of the property to retrieve.
   * @param def the default value to use.
   * @param min the minimum acceptable value.
   * @param max the maximum acceptable value.
   * @return the value of the property as an int.
   */
  public int getInt(final String name, final int def, final int min, final int max) {
    if (!config.containsKey(name)) return def;
    int val = config.getInt(name);
    if ((val < min) || (val > max)) val = def;
    return val;
  }

  /**
   * Get the integer value of the specified configuration property.
   * If the property with the specified name does not exist, it is looked up using a legacy name.<br> 
   * If the property is not defined, the default value is returned.
   * @param name the name of the property whose value is to be retrieved.
   * @param oldName the legacy name of the property to retrieve.
   * @param def the default value to use.
   * @return the value of the property as an int.
   */
  public int getInt(final String name, final String oldName, final int def) {
    int val = def;
    if (!config.containsKey(name)) {
      if (!config.containsKey(oldName)) return def;
      else val = config.getInt(oldName);
    }
    else val = config.getInt(name);
    return val;
  }

  /**
   * Get the integer value of the specified configuration property.
   * If the property with the specified name does not exist, it is looked up using a legacy name.<br> 
   * If the property is not defined or the value is not in the range <code>[min, max]</code>, the default value is returned.
   * @param name the name of the property whose value is to be retrieved.
   * @param oldName the legacy name of the property to retrieve.
   * @param def the default value to use.
   * @param min the minimum acceptable value.
   * @param max the maximum acceptable value.
   * @return the value of the property as an int.
   */
  public int getInt(final String name, final String oldName, final int def, final int min, final int max) {
    int val = def;
    if (!config.containsKey(name)) {
      if (!config.containsKey(oldName)) return def;
      else val = config.getInt(oldName);
    }
    else val = config.getInt(name);
    if ((val < min) || (val > max)) val = def;
    return val;
  }

  /**
   * Get the long value of the specified configuration property.<br>
   * If the property is not defined or the value is not in the range <code>[min, max]</code>, the default value is returned.
   * @param name the name of the property to retrieve.
   * @param def the default value to use.
   * @param min the minimum acceptable value.
   * @param max the maximum acceptable value.
   * @return the value of the property as a long.
   */
  public long getLong(final String name, final long def, final long min, final long max) {
    if (!config.containsKey(name)) return def;
    long val = config.getInt(name);
    if ((val < min) || (val > max)) val = def;
    return val;
  }

  /**
   * Get the long value of the specified configuration property.
   * If the property with the specified name does not exist, it is looked up using a legacy name.<br> 
   * If the property is not defined, the default value is returned.
   * @param name the name of the property whose value is to be retrieved.
   * @param oldName the legacy name of the property to retrieve.
   * @param def the default value to use.
   * @return the value of the property as a long.
   */
  public long getLong(final String name, final String oldName, final long def) {
    long val = def;
    if (!config.containsKey(name)) {
      if (!config.containsKey(oldName)) return def;
      else val = config.getInt(oldName);
    }
    else val = config.getInt(name);
    return val;
  }

  /**
   * Load this properties objects from the specified reader.
   * This load operation will recursively process all <code>!#include</code> statements before parsing the properties,
   * then perform the properties substitutions.
   * @param reader the reader to load from.
   * @return a {@link TypedProperties} object with all includes loaded and substitutions performed.
   * @throws IOException if any I/O error occurs.
   * @exclude
   */
  public static TypedProperties loadAndResolve(final Reader reader) throws IOException {
    TypedProperties props = new TypedProperties();
    new PropertiesLoader().load(props, reader);
    props = new SubstitutionsHandler(props).resolve();
    new ScriptHandler().process(props);
    return props;
  }
}
