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
package org.jppf.utils;

import java.io.*;
import java.util.concurrent.locks.*;

import org.jppf.serialization.JPPFSerialization;
import org.jppf.utils.configuration.JPPFProperty;
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
  private static final Logger log = LoggerFactory.getLogger(JPPFConfiguration.class);
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
  private static TypedProperties props;
  /**
   * Used to synchronize propertie loading.
   */
  private static final Lock propertiesLock = new ReentrantLock();

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
    propertiesLock.lock();
    try {
      if (props == null) loadProperties();
      return props;
    } finally {
      propertiesLock.unlock();
    }
  }

  /**
   * Get the value of a predefined property.
   * This is the same as calling {@code JPPFConfiguration.getProperties().get(property)}.
   * @param <T> the type of the property.
   * @param property the property whose value to retrieve.
   * @return the value of the property according to its type.
   */
  public static <T> T get(final JPPFProperty<T> property) {
    return getProperties().get(property);
  }

  /**
   * Get the value of a predefined parametrized property.
   * This is the same as calling {@code JPPFConfiguration.getProperties().get(property, parameters)}.
   * @param <T> the type of the property.
   * @param property the property whose value to retrieve.
   * @param parameters the values to replace the property's parameters with.
   * @return the value of the property according to its type.
   * @since 6.0
   */
  public static <T> T get(final JPPFProperty<T> property, final String...parameters) {
    return getProperties().get(property, parameters);
  }

  /**
   * Set the value of a predefined property.
   * This is the same as calling {@code JPPFConfiguration.getProperties().set(property, value)}.
   * @param <T> the type of the property.
   * @param property the property whose value to set.
   * @param value the value to set.
   * @return the {@link TypedProperties} instance in which the value is set.
   */
  public static <T> TypedProperties set(final JPPFProperty<T> property, final T value) {
    return getProperties().set(property, value);
  }

  /**
   * Set the value of a predefined parametrized property.
   * This is the same as calling {@code JPPFConfiguration.getProperties().set(property, value, parameters)}.
   * @param <T> the type of the property.
   * @param property the property whose value to set.
   * @param value the value to set.
   * @param parameters the values to replace the property's parameters with.
   * @return the {@link TypedProperties} instance in which the value is set.
   * @since 6.0
   */
  public static <T> TypedProperties set(final JPPFProperty<T> property, final T value, final String...parameters) {
    return getProperties().set(property, value, parameters);
  }

  /**
   * Remove the predefined property.
   * This is the same as calling {@code JPPFConfiguration.getProperties().remove(property)}.
   * @param <T> the type of the property.
   * @param property the property whose value to retrieve.
   * @return the value of the property according to its type.
   */
  public static <T> T remove(final JPPFProperty<T> property) {
    return getProperties().remove(property);
  }

  /**
   * Remove the predefined parametrized property.
   * This is the same as calling {@code JPPFConfiguration.getProperties().remove(property)}.
   * @param <T> the type of the property.
   * @param property the property whose value to retrieve.
   * @param parameters the values to replace the property's parameters with.
   * @return the value of the property according to its type.
   * @since 6.0
   */
  public static <T> T remove(final JPPFProperty<T> property, final String...parameters) {
    return getProperties().removeProperty(property, parameters);
  }

  /**
   * Determine whether the JPPF configuration contains the specified property.
   * The lookkup is performed on the property's name first, then on its aliases if the name is not found.
   * @param property the property to look for.
   * @return {@code true} if the property or one of its aliases is found, {@code false} otherwise.
   * @since 6.0
   */
  public static boolean containsProperty(final JPPFProperty<?> property) {
    return getProperties().containsProperty(property);
  }

  /**
   * Reset and reload the JPPF configuration.
   * This allows reloading the configuration from a different source or file
   * (after changing the values of the related system properties for instance).
   */
  public static void reset() {
    propertiesLock.lock();
    try {
      //SSLHelper.resetConfig();
      loadProperties();
      JPPFSerialization.Factory.reset();
    } finally {
      propertiesLock.unlock();
    }
  }

  /**
   * Reset the JPPF configuration with the specified properties.
   * @param newConfig the properties to use as the new configuration.
   * @since 6.0
   */
  public static void reset(final TypedProperties newConfig) {
    propertiesLock.lock();
    try {
      //SSLHelper.resetConfig();
      loadProperties(newConfig);
      JPPFSerialization.Factory.reset();
    } finally {
      propertiesLock.unlock();
    }
  }

  /**
   * Load the JPPF configuration properties from a file.
   * @param newConfig the properties to use as the new configuration.
   * @since 6.0
   */
  private static void loadProperties(final TypedProperties newConfig) {
    props = new TypedProperties();
    try (Reader reader = new StringReader(newConfig.asString())) {
      props.loadAndResolve(reader);
    } catch(final Exception e) {
      log.error("error reading the configuration", e);
    }
  }

  /**
   * Load the JPPF configuration properties from a file.
   */
  private static void loadProperties() {
    props = new TypedProperties();
    try (Reader reader = getReader()) {
      if (reader != null) props.loadAndResolve(reader);
    } catch(final Exception e) {
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
    final String filename = System.getProperty(CONFIG_PROPERTY, DEFAULT_FILE);
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
    final Class<?> clazz = Class.forName(configurationSourceName);
    if (ConfigurationSourceReader.class.isAssignableFrom(clazz)) {
      final ConfigurationSourceReader source = (ConfigurationSourceReader) clazz.newInstance();
      reader = source.getPropertyReader();
    } else if (ConfigurationSource.class.isAssignableFrom(clazz)) {
      final ConfigurationSource source = (ConfigurationSource) clazz.newInstance();
      final InputStream is = source.getPropertyStream();
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
