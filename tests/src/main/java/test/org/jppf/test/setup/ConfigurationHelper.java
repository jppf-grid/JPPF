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

package test.org.jppf.test.setup;

import java.io.*;
import java.util.*;

import org.apache.log4j.Level;
import org.jppf.scripting.ScriptDefinition;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class ConfigurationHelper {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ConfigurationHelper.class);
  /**
   * Cache of the created temp files, so they can be deleted upon cleanup.
   */
  private static final List<File> tempFiles = new Vector<>();
  /**
   * 
   */
  private static final String FOR ="$for{";

  /**
   * Create a temporary file containing the specified configuration.
   * @param config the configuration to store on file.
   * @return the path to the created file.
   */
  public static String createTempConfigFile(final TypedProperties config) {
    String path = null;
    try {
      final File file = File.createTempFile("config", ".properties", FileUtils.getJPPFTempDir());
      file.deleteOnExit();
      try (Writer writer = new BufferedWriter(new FileWriter(file))) {
        config.store(writer, null);
      }
      path = file.getCanonicalPath().replace('\\', '/');
      tempFiles.add(file);
    } catch (final Exception e) {
      e.printStackTrace();
      throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    }
    return path;
  }

  /**
   * Load and initialize a configuration based on a template configuration file.
   * @param templatePath the path to a configuration file template.
   * @param n the driver or node number to use.
   * @return a configuration object where the string "${n}" was replace with the driver or node number.
   */
  public static TypedProperties createConfigFromTemplate(final String templatePath, final int n) {
    final Map<String, Object> variables = new HashMap<>();
    variables.put("$n", n);
    return createConfigFromTemplate(templatePath, variables);
  }

  /**
   * Load and initialize a configuration based on a template configuration file.
   * @param templatePath the path to a configuration file template.
   * @param variables a map of variable names to their value, which can be used in a groovy expression.
   * @return a configuration object where the string "${n}" was replace with the driver or node number.
   */
  public static TypedProperties createConfigFromTemplate(final String templatePath, final Map<String, Object> variables) {
    final TypedProperties result = new TypedProperties();
    try (Reader reader = FileUtils.getFileReader(templatePath)) {
      final TypedProperties props = new TypedProperties();
      props.loadAndResolve(reader);
      for (final Map.Entry<Object, Object> entry: props.entrySet()) {
        if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
          parseProperty(result, (String) entry.getKey(), (String) entry.getValue(), variables);
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
      throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    }
    return result;
  }

  /**
   * Parse the specified property value, performinig templates substitutions where needed.
   * <p>format: {@code name = $for{start; end; condition} value}
   * <p>example: driver$i.jppf.server.port = $for{1; $nbDrivers; $n != $i} expr: 11100 + $i
   * @param result the resulting properties.
   * @param key a key used to cache the compiled groovy expression.
   * @param source the string to parse.
   * @param variables a map of variable names to their value, which can be used in a groovy expression.
   * @throws Exception if any error occurs.
   */
  static void parseProperty(final TypedProperties result, final String key, final String source, final Map<String, Object> variables) throws Exception {
    String value = source.trim();
    if (value.startsWith(FOR)) {
      final int idx = value.indexOf("}", FOR.length());
      final String paramsStr = value.substring(FOR.length(), idx);
      value = value.substring(idx + 1).trim();
      final String[] params = paramsStr.split(";");
      for (int i=0; i<params.length; i++) params[i] = "return " + params[i].trim();
      final int start = (Integer) new ScriptDefinition("groovy", params[0], variables).evaluate();
      final int end = (Integer) new ScriptDefinition("groovy", params[1], variables).evaluate();
      for (int i=start; i<= end; i++) {
        final String iStr = Integer.toString(i);
        final boolean proceed = (Boolean) new ScriptDefinition("groovy", params[2].replace("$i", iStr), variables).evaluate();
        if (proceed) {
          String v = value;
          if (v.startsWith("expr:")) {
            final String expr = v.substring("expr:".length()).trim().replace("$i", iStr);
            final Object o = new ScriptDefinition("groovy", expr, variables).evaluate();
            if (o != null) v = o.toString();
          }
          final String actualKey = key.replace("$i", iStr);
          result.setProperty(actualKey, v);
        }
      }
    } else {
      value = parseValue(key, value, variables);
      result.setProperty(key, value);
    }
  }

  /**
   * Parse the specified property value, performinig templates substitutions where needed.
   * @param key a key used to cache the compiled groovy expression.
   * @param source the string to parse.
   * @param variables a map of variable names to their value, which can be used in a groovy expression.
   * @return a string where tmeplate expressions have been replaced.
   * @throws Exception if any error occurs.
   */
  private static String parseValue(final String key, final String source, final Map<String, Object> variables) throws Exception {
    String value = source.trim();
    if (value.startsWith("expr:")) {
      final String expr = value.substring("expr:".length()).trim();
      try {
        final Object o = new ScriptDefinition("groovy", expr, variables).evaluate();
        if (o != null) value = o.toString();
      } catch (final Exception e) {
        log.error("error evaluating scripted property [key={}, script={}, variables={}]", key, value, variables, e);
        throw e;
      }
    }
    return value;
  }

  /**
   * Load a properties file form the specified path.
   * @param path the path to the file to load.
   * @return a {@link TypedProperties} with the properties of the specified file.
   */
  public static TypedProperties loadProperties(final File path) {
    return loadProperties(new TypedProperties(), path);
  }

  /**
   * Load a properties file from the specified path and put its entries into the specified properties set.
   * @param properties the properties object to populate.
   * @param path the path to the file to load.
   * @return a {@link TypedProperties} with the properties of the specified file.
   */
  public static TypedProperties loadProperties(final TypedProperties properties, final File path) {
    try (Reader reader = new BufferedReader(new FileReader(path))) {
      properties.loadAndResolve(reader);
    } catch (final Exception e) {
      e.printStackTrace();
      throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    }
    return properties;
  }

  /**
   * Set the override propeties onto the specified configuration, overriding values when needed.
   * @param config the config for which to override properties.
   * @param overridePath the path to the properties that will override those in the configuration.
   */
  public static void overrideConfig(final TypedProperties config, final File overridePath) {
    overrideConfig(config, loadProperties(overridePath));
  }

  /**
   * Set the override propeties onto the specified configuration, overriding values when needed.
   * @param config the config for which to override properties.
   * @param override the properties that will override those in the configuration.
   */
  public static void overrideConfig(final TypedProperties config, final TypedProperties override) {
    for (final Map.Entry<Object, Object> entry : override.entrySet()) {
      if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
        config.put(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * Cleanup the created temporary files.
   */
  public static void cleanup() {
    while (!tempFiles.isEmpty()) tempFiles.remove(0).delete();
  }

  /**
   * Set the specified Log4j logger to the specified level.
   * @param level the level to set.
   * @param names the names of the log4j loggers to configure.
   */
  public static void setLoggerLevel(final Level level, final String...names) {
    for (final String name: names) {
      final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(name);
      if (logger != null) logger.setLevel(level);
    }
  }

  /**
   * 
   */
  private static final String LOGGER_PREFIX = "log4j.logger.";

  /**
   * Set the Log4j loggers as found in the specified log4j proeprties file.
   * @param path the level to set.
   * @throw Exception if any error occurs.
   */
  public static void setLoggerLevels(final String path) throws Exception {
    final TypedProperties config = new TypedProperties();
    try (final Reader reader = new BufferedReader(new FileReader(path))) {
      config.loadAndResolve(reader);
    }
    config.forEach((key, value) -> {
      if ((key instanceof String) && (value instanceof String) && ((String) key).startsWith(LOGGER_PREFIX)) {
        final String loggerName = ((String) key).substring(LOGGER_PREFIX.length());
        final String levelName = (String) value;
        final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(loggerName);
        if (logger != null) logger.setLevel(Level.toLevel(levelName));
      }
    });
  }
}
