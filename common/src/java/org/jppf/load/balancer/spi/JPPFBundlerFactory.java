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

package org.jppf.load.balancer.spi;

import java.util.*;

import org.jppf.JPPFException;
import org.jppf.load.balancer.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Factory class for JPPF load-balancing algorithms defined through the {@link org.jppf.load.balancer.spi.JPPFBundlerProvider JPPFBundlerProvider}
 * service provider interface.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFBundlerFactory
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFBundlerFactory.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * This enum defines the available default load balancing configurations.
   * @exclude
   */
  public enum Defaults {
    /**
     * Default load-balancing configuration for JPPF servers.
     */
    SERVER(createServerDefaults()),
    /**
     * Default load-balancing configuration for JPPF clients.
     */
    CLIENT(createClientDefaults());

    /**
     * The configuration as a string.
     */
    private TypedProperties config = null;

    /**
     * Initialize with the specified configuration as a string.
     * @param config the configuration as a string.
     */
    Defaults(final TypedProperties config) {
      this.config = config;
    }
 
    /**
     * Get the value of this configuration default.
     * @return the value as a {@link TypedProperties} object.
     */
    public TypedProperties config() {
      return config;
    }

    @Override
    public String toString() {
      return name() + (config == null ? ":null" : config);
    }
  }
  /**
   * Map of all registered providers.
   */
  private Map<String, JPPFBundlerProvider> providerMap = null;
  /**
   * The default values to use if nothing is specified in the JPPF configuration.
   */
  private final Defaults defaultConfig;

  /**
   * Default constructor.
   */
  public JPPFBundlerFactory() {
    this(Defaults.SERVER);
  }

  /**
   * Default constructor.
   * @param def the default values to use if nothing is specified in the JPPF configuration.
   */
  public JPPFBundlerFactory(final Defaults def) {
    defaultConfig = def;
    if (debugEnabled) log.debug("using default properties: " + defaultConfig);
  }

  /**
   * Create an instance of the bundler with the specified name and parameters.
   * @param name the name of the bundler's algorithm, such as specified in the bundler provider and in the configuration.
   * @param configuration a map of algorithm parameters to their value.
   * @return a new <code>Bundler</code> instance.
   * @throws Exception if the bundler could not be created.
   */
  public Bundler createBundler(final String name, final TypedProperties configuration) throws Exception {
    JPPFBundlerProvider provider = getBundlerProvider(name);
    if (provider == null) throw new JPPFException("Provider '" + name + "' could not be found");
    LoadBalancingProfile profile = provider.createProfile(configuration);
    return provider.createBundler(profile);
  }

  /**
   * Create an instance of the bundler such as specified in the JPPF configuration file.
   * @return a new <code>Bundler</code> instance.
   * @throws Exception if the bundler could not be created.
   */
  public Bundler createBundlerFromJPPFConfiguration() throws Exception {
    TypedProperties config = JPPFConfiguration.getProperties();
    String algorithm = config.getString(JPPFProperties.LOAD_BALANCING_ALGORITHM.getName(), null);
    if (algorithm == null) algorithm = defaultConfig.config().get(JPPFProperties.LOAD_BALANCING_ALGORITHM);
    String profileName = config.getString(JPPFProperties.LOAD_BALANCING_PROFILE.getName(), null);
    if (profileName == null) {
      String prefix = JPPFProperties.LOAD_BALANCING_PROFILE.getName();
      profileName = defaultConfig.config().get(JPPFProperties.LOAD_BALANCING_PROFILE);
      String prefixDot = prefix + '.';
      for (Map.Entry<Object, Object> entry: defaultConfig.config().entrySet()) {
        if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
          String key = (String) entry.getKey();
          if (!config.containsKey(key) && key.startsWith(prefixDot)) config.put(key, entry.getValue());
        }
      }
    }
    TypedProperties configuration = convertJPPFConfiguration(profileName, config);
    if (debugEnabled) log.debug("load balancing configuration using algorithm '" + algorithm +"' with parameters: " + configuration);
    return createBundler(algorithm, configuration);
  }

  /**
   * Get the bundler provider with the specified name.<br>
   * This method will trigger a lazy loading of the providers if they haven't been loaded yet.
   * @param name the name of the bundler provider to retrieve.
   * @return a <code>JPPFBundlerProvider</code> instance or null if the provider could not be found.
   * @throws Exception if any error occurs while loading the providers.
   */
  public JPPFBundlerProvider getBundlerProvider(final String name) throws Exception {
    if (providerMap == null) loadProviders();
    return providerMap.get(name);
  }

  /**
   * Get the names of all discovered bundler providers.
   * @return a list of provider names.
   * @throws Exception if any error occurs while loading the providers.
   */
  public List<String> getBundlerProviderNames() throws Exception {
    if (providerMap == null) loadProviders();
    return new ArrayList<>(providerMap.keySet());
  }

  /**
   * Retrieve all the bundler providers configured through the service provider interface (SPI).
   * @throws Exception if any error occurs while loading the providers.
   */
  private void loadProviders() throws Exception {
    Map<String, JPPFBundlerProvider> map = new Hashtable<>();
    ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
    ClassLoader currentCL = getClass().getClassLoader();
    if (debugEnabled) log.debug("oldCL=" + oldCL + ", currentCL=" + currentCL);
    final boolean isDiff = (oldCL != currentCL);
    ClassLoader[] loaders = (isDiff) ? new ClassLoader[] { currentCL, oldCL } : new ClassLoader[] { oldCL };
    for (ClassLoader cl: loaders) {
      Iterator<JPPFBundlerProvider> it = ServiceFinder.lookupProviders(JPPFBundlerProvider.class, cl);
      while (it.hasNext()) {
        JPPFBundlerProvider provider = it.next();
        map.put(provider.getAlgorithmName(), provider);
        if (debugEnabled) log.debug("registering new load-balancing algorithm provider '" + provider.getAlgorithmName() + '\'');
      }
      if (debugEnabled) log.debug("found " + map.size() + " load-balancing algorithms in the classpath");
      if (!map.isEmpty()) {
        providerMap = map;
        break;
      }
    }
  }

  /**
   * Convert a JPPF configuration map to a profile configuration by extracting the properties related to the specified profile
   * and removing the JPPF-specific prefix from their name.
   * @param profileName the name of the profile to extract.
   * @param configuration the JPPF configuration to extract from.
   * @return a <code>TypedProperties</code> instance containing only the profile-specific parameters.
   */
  public TypedProperties convertJPPFConfiguration(final String profileName, final TypedProperties configuration) {
    TypedProperties profile = extractJPPFConfiguration(profileName, configuration);
    String prefix = JPPFProperties.LOAD_BALANCING_PROFILE.getName() + '.' + profileName + '.';
    TypedProperties result = new TypedProperties();
    for (Map.Entry<Object, Object> entry: profile.entrySet()) {
      String key = (String) entry.getKey();
      String s = key.substring(prefix.length());
      result.setProperty(s, (String) entry.getValue());
    }
    return result;
  }

  /**
   * Extract the JPPF-prefixed load-balancing parameters from the specified configuration and based on the specified profile name.<br/>
   * All entries in the resulting map have a key starting with "<code>jppf.load.balancing.profile.&lt;profileName&gt;</code>"
   * @param profileName the name of the profile to extract.
   * @param configuration the JPPF configuration to extract from.
   * @return a <code>TypedProperties</code> instance containing only the profile-specific parameters.
   */
  private TypedProperties extractJPPFConfiguration(final String profileName, final TypedProperties configuration) {
    TypedProperties profile = new TypedProperties();
    String prefix = "strategy." + profileName + '.';
    String prefix2 = JPPFProperties.LOAD_BALANCING_PROFILE.getName() + '.' + profileName + '.';
    for (Map.Entry<Object, Object> entry: configuration.entrySet()) {
      if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
        String key = (String) entry.getKey();
        if (key.startsWith(prefix)) {
          String propName = key.substring(prefix.length());
          profile.setProperty(prefix2 + propName, (String) entry.getValue());
        }
        else if (key.startsWith(prefix2)) profile.setProperty(key, (String) entry.getValue());
      }
    }
    return profile;
  }

  /**
   * Create the default server load-balancing settings. 
   * @return the settings as a {@link TypedProperties} instance.
   */
  private static TypedProperties createServerDefaults() {
    String prefix = JPPFProperties.LOAD_BALANCING_PROFILE.getName() + ".jppf.";
    return new TypedProperties().set(JPPFProperties.LOAD_BALANCING_ALGORITHM, "proportional")
      .set(JPPFProperties.LOAD_BALANCING_PROFILE, "jppf")
      .setInt(prefix + "performanceCacheSize", 3000)
      .setInt(prefix + "proportionalityFactor", 1)
      .setInt(prefix + "initialSize = 10", 1)
      .setDouble(prefix + "initialMeanTime", 1e9);
  }

  /**
   * Create the default server load-balancing settings. 
   * @return the settings as a {@link TypedProperties} instance.
   */
  private static TypedProperties createClientDefaults() {
    String prefix = JPPFProperties.LOAD_BALANCING_PROFILE.getName() + ".jppf.";
    return new TypedProperties().set(JPPFProperties.LOAD_BALANCING_ALGORITHM, "manual")
      .set(JPPFProperties.LOAD_BALANCING_PROFILE, "jppf")
      .setInt(prefix + "size", 1_000_000);
  }
}
