/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
import java.util.Map;

import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class ConfigurationHelper
{
  /**
   * Create a temporary file containing the specified configuration.
   * @param config the configuration to store on file.
   * @return the path to the created file.
   */
  public static String createTempConfigFile(final TypedProperties config)
  {
    String path = null;
    try
    {
      File file = File.createTempFile("config", ".properties");
      file.deleteOnExit();
      Writer writer = null;
      try
      {
        writer = new BufferedWriter(new FileWriter(file));
        config.store(writer, null);
      }
      finally
      {
        if (writer != null) StreamUtils.closeSilent(writer);
      }
      path = file.getCanonicalPath().replace('\\', '/');
    }
    catch (Exception e)
    {
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
  public static TypedProperties createConfigFromTemplate(final String templatePath, final int n)
  {
    TypedProperties result = new TypedProperties();
    Reader reader = null;
    try
    {
      TypedProperties props = new TypedProperties();
      reader = FileUtils.getFileReader(templatePath);
      if (reader == null) throw new FileNotFoundException("could not load config file '" + templatePath + '\'');
      props.load(reader);
      for (Map.Entry<Object, Object> entry: props.entrySet())
      {
        if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String))
        {
          String key = (String) entry.getKey();
          String value = (String) entry.getValue();
          int idx = value.indexOf("+${n}");
          if (idx >= 0)
          {
            String s = value.substring(0, idx);
            try
            {
              Integer intValue = Integer.valueOf(s) + n;
              value = intValue.toString();
            }
            catch (Exception e)
            {
              throw new RuntimeException(e);
            }
          }
          else if ((idx = value.indexOf("${n}")) >= 0)
          {
            value = value.replace("${n}", String.valueOf(n));
          }
          result.setProperty(key, value);
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    }
    finally
    {
      if (reader != null) StreamUtils.closeSilent(reader);
    }
    return result;
  }

  /**
   * Load a properties file form the specified path.
   * @param path the path to the file to load.
   * @return a {@link TypedProperties} with the properties of the specified file.
   */
  public static TypedProperties loadProperties(final File path)
  {
    return loadProperties(new TypedProperties(), path);
  }

  /**
   * Load a properties file from the specified path and put its entries into the specified properties set.
   * @param properties the properties object to populate.
   * @param path the path to the file to load.
   * @return a {@link TypedProperties} with the properties of the specified file.
   */
  public static TypedProperties loadProperties(final TypedProperties properties, final File path)
  {
    Reader reader = null;
    try
    {
      reader = new BufferedReader(new FileReader(path));
      properties.load(reader);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    }
    finally
    {
      if (reader != null) StreamUtils.closeSilent(reader);
    }
    return properties;
  }

  /**
   * Set the override propeties onto the specified configuration, overriding values when needed.
   * @param config the config for which to override properties.
   * @param overridePath the path to the properties that will override those in the configuration.
   */
  public static void overrideConfig(final TypedProperties config, final File overridePath)
  {
    overrideConfig(config, loadProperties(overridePath));
  }

  /**
   * Set the override propeties onto the specified configuration, overriding values when needed.
   * @param config the config for which to override properties.
   * @param override the properties that will override those in the configuration.
   */
  public static void overrideConfig(final TypedProperties config, final TypedProperties override)
  {
    for (Map.Entry<Object, Object> entry: override.entrySet())
    {
      if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String))
      {
        config.put(entry.getKey(), entry.getValue());
      }
    }
  }
}
