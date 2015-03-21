/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
package org.jppf.utils;

import java.io.*;

import org.jppf.serialization.JPPFSerialization;
import org.jppf.ssl.SSLHelper;
import org.slf4j.*;

/**
 * Utility class for loading and accessing the JPPF configuration properties.
 * <p>The configuration file path is set through the system property {@link org.jppf.utils#CONFIG_PROPERTY CONFIG_PROPERTY},
 * whose value is &quot;jppf.config&quot;.<br>
 * As an example, it can be configured by adding the JVM argument &quot;<i>-Djppf.config=jppf-config.properties</i>&quot;.
 * <p>Modified to allow users to get configuration properties from an alternate source. Any user-provided class that
 * implements {@link ConfigurationSource} and returns a stream with the same configuration values provided in the properties file.
 * <br>The configuration source must then configured via the system property <i>-Djppf.config.plugin = mypackage.MyConfigurationSource</i>&quot;
 * @author Laurent Cohen
 * @author Jonathan Newbrough
 */
public final class JPPFConfiguration {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFConfiguration.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Name of the system property holding the location of the JPPF configuration file.
   */
  public static final String CONFIG_PROPERTY = "jppf.config";
  /**
   * The name of the system property used to specified an alternate JPPF configuration source.
   */
  public static final String CONFIG_PLUGIN_PROPERTY = "jppf.config.plugin";
  /**
   * Default location of the JPPF configuration file.
   */
  public static final String DEFAULT_FILE = "jppf.properties";
  /**
   * Holds the JPPF configuration properties.
   */
  private static TypedProperties props = null;

  /**
   * Prevent instantiation from another class.
   */
  private JPPFConfiguration() {
  }

  /**
   * Get the configuration properties.
   * @return a TypedProperties instance.
   */
  public static TypedProperties getProperties() {
    if (props == null) loadProperties();
    return props;
  }

  /**
   * Reset and reload the JPPF configuration.
   * This allows reloading the configuration from a different source or file
   * (after changing the values of the related system properties for instance).
   */
  public static void reset() {
    SSLHelper.resetConfig();
    loadProperties();
    JPPFSerialization.Factory.reset();
  }

  /**
   * Load the JPPF configuration properties from a file.
   */
  private static void loadProperties() {
    props = new TypedProperties();
    try (Reader reader = getReader()) {
      if (reader != null) props.loadAndResolve(reader);
    } catch(Exception e) {
      log.error("error reading the configuration", e);
    }
  }

  /**
   * Get an input stream from which to read the configuration properties.
   * @return an {@link InputStream} instance.
   * @throws Exception if any error occurs while trying to obtain the stream.
   */
  private static Reader getReader() throws Exception {
    String altSource = System.getProperty(CONFIG_PLUGIN_PROPERTY);
    if ((altSource != null) && "".equals(altSource.trim())) altSource = null;
    String filename = System.getProperty(CONFIG_PROPERTY, DEFAULT_FILE);
    return getConfigurationReader(filename, altSource);
  }

  /**
   * Get an inputStream for a properties file located either by the specified filename or configuration source.
   * @param filename specifies the loaction of the properties file in the file system or classpath.
   * @param configurationSourceName fully qualified name of a class implementating {@link JPPFConfiguration.ConfigurationSource}.
   * @return an input stream that can be used to load the properties.
   * @throws Exception if any error occurs while trying to obtain the stream.
   */
  private static Reader getConfigurationReader(final String filename, final String configurationSourceName) throws Exception {
    Reader reader = null;
    if (configurationSourceName != null) {
      reader = getConfigurationSourceReader(configurationSourceName);
    } else {
      if (log.isDebugEnabled()) log.debug("reading JPPF configuration file: " + filename);
      reader = FileUtils.getFileReader(filename);
    }
    return reader;
  }

  /**
   * Get an inputStream for a properties file located by the specified configuration source.
   * @param configurationSourceName fully qualified name of a class implementing {@link JPPFConfiguration.ConfigurationSource}
   * or {@link JPPFConfiguration.ConfigurationSourceReader}.
   * @return an input stream that can be used to load the properties.
   * @throws Exception if any error occurs while trying to obtain the stream.
   * @exclude
   */
  public static Reader getConfigurationSourceReader(final String configurationSourceName) throws Exception {
    Reader reader = null;
    if (log.isDebugEnabled()) log.debug("reading JPPF configuration from config source: " + configurationSourceName);
    Class<?> clazz = Class.forName(configurationSourceName);
    if (ConfigurationSourceReader.class.isAssignableFrom(clazz)) {
      ConfigurationSourceReader source = (ConfigurationSourceReader) clazz.newInstance();
      reader = source.getPropertyReader();
    } else if (ConfigurationSource.class.isAssignableFrom(clazz)) {
      ConfigurationSource source = (ConfigurationSource) clazz.newInstance();
      InputStream is = source.getPropertyStream();
      reader = new InputStreamReader(is);
    }
    else throw new IllegalArgumentException("the type '" + configurationSourceName + "' is neither a JPPFConfiguration.ConfigurationSource " + 
      "nor a JPPFConfiguration.ConfigurationSourceReader");
    return reader;
  }

  /**
   * Implement this interface to provide an alternate configuration source via an {@link InputStream}.
   * <p>WARNING: not shown in the interface but also required: implementations must have a public no-arg constructor.
   */
  public interface ConfigurationSource {
    /**
     * Obtain the JPPF configuration properties from an input stream.
     * The returned stream content must conform to the properties file's specifications
     * (i.e. it must be usable as the argument to <code>Properties.load(InputStream)</code>).
     * @return an {@link InputStream} instance.
     * @throws IOException if the stream cannot be created.
     */
    InputStream getPropertyStream() throws IOException;
  }

  /**
   * Implement this interface to provide an alternate configuration source via a {@link Reader}.
   * <p>WARNING: not shown in the interface but also required: implementations must have a public no-arg constructor.
   */
  public interface ConfigurationSourceReader {
    /**
     * Obtain the JPPF configuration properties from a {@link Reader}.
     * The returned reader content must conform to the properties file's specifications
     * (i.e. it must be usable as the argument to <code>Properties.load(InputStream)</code>).
     * @return an {@link Reader} instance.
     * @throws IOException if the stream cannot be created.
     */
    Reader getPropertyReader() throws IOException;
  }
}
