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

package test.org.jppf.test.setup;

import java.io.*;
import java.util.*;

import org.jppf.scripting.*;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class ConfigurationHelper {
  /**
   * Cache of the created temp files, so they can be deleted upon cleanup.
   */
  private static List<File> tempFiles = new Vector<>();

  /**
   * Create a temporary file containing the specified configuration.
   * @param config the configuration to store on file.
   * @return the path to the created file.
   */
  public static String createTempConfigFile(final TypedProperties config) {
    String path = null;
    try {
      System.err.println("jppf temp dir: " + FileUtils.getJPPFTempDir());
      File file = File.createTempFile("config", ".properties", FileUtils.getJPPFTempDir());
      file.deleteOnExit();
      Writer writer = null;
      try {
        writer = new BufferedWriter(new FileWriter(file));
        config.store(writer, null);
      } finally {
        if (writer != null) StreamUtils.closeSilent(writer);
      }
      path = file.getCanonicalPath().replace('\\', '/');
      tempFiles.add(file);
    } catch (Exception e) {
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
    Map<String, Object> variables = new HashMap<>();
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
    TypedProperties result = new TypedProperties();
    Reader reader = null;
    try {
      TypedProperties props = new TypedProperties();
      reader = FileUtils.getFileReader(templatePath);
      if (reader == null) throw new FileNotFoundException("could not load config file '" + templatePath + '\'');
      props.load(reader);
      for (Map.Entry<Object, Object> entry : props.entrySet()) {
        if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
          String key = (String) entry.getKey();
          String value = (String) entry.getValue();
          try {
            String s = "[" + templatePath + ", " + key + "]";
            value = parseValue(s, value, variables);
          } catch (Exception e) {
            throw new RuntimeException("Invalid expression for template file: '" + templatePath + "', property: '" + key + " = " + value + '\'', e);
          }
          result.setProperty(key, value);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    } finally {
      if (reader != null) StreamUtils.closeSilent(reader);
    }
    return result;
  }

  /**
   * Parse the specified property value, performinig templates substitutions where eneded.
   * @param key a key used to cache the compiled groovy expression.
   * @param source the string to parse.
   * @param variables a map of variable names to their value, which can be used in a groovy expression.
   * @return a string where tmeplate expressions have been replaced.
   * @throws Exception if any error occurs.
   */
  private static String parseValue(final String key, final String source, final Map<String, Object> variables) throws Exception {
    String value = source.trim();
    if (value.startsWith("expr:")) {
      String expr = value.substring("expr:".length()).trim();
      ScriptRunner runner = null;
      try {
        runner = ScriptRunnerFactory.getScriptRunner("groovy");
        Object o = runner.evaluate(key, expr, variables);
        if (o != null) value = o.toString();
      } finally {
        ScriptRunnerFactory.releaseScriptRunner(runner);
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
    Reader reader = null;
    try {
      reader = new BufferedReader(new FileReader(path));
      properties.load(reader);
    } catch (Exception e) {
      e.printStackTrace();
      throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    } finally {
      if (reader != null) StreamUtils.closeSilent(reader);
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
    for (Map.Entry<Object, Object> entry : override.entrySet()) {
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
}
