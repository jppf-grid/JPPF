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

import java.io.*;
import java.util.*;

import org.jppf.JPPFException;
import org.jppf.load.balancer.*;
import org.jppf.utils.*;
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
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * This enum defines the available default load balancing configurations.
   * @exclude
   */
  public enum Defaults {
    /**
     * Default load-balancing configuration for JPPF servers.
     */
    SERVER(new StringBuilder().append("jppf.load.balancing.algorithm = proportional\n")
      .append("jppf.load.balancing.profile = jppf\n")
      .append("jppf.load.balancing.profile.jppf.performanceCacheSize = 3000\n")
      .append("jppf.load.balancing.profile.jppf.proportionalityFactor = 1\n")
      .append("jppf.load.balancing.profile.jppf.initialSize = 10\n")
      .append("jppf.load.balancing.profile.jppf.initialMeanTime = 1e9\n")),
    /**
     * Default load-balancing configuration for JPPF clients.
     */
    CLIENT(new StringBuilder().append("jppf.load.balancing.algorithm = manual\n")
      .append("jppf.load.balancing.profile = jppf\n")
      .append("jppf.load.balancing.profile.jppf.size = 1000000\n"));

    /**
     * The configuration as a string.
     */
    private TypedProperties config = null;

    /**
     * Initialize with the specified configuration as a string.
     * @param config the configuration as a string.
     */
    Defaults(final CharSequence config) {
      try {
        String s = (config instanceof String) ? (String) config : config.toString();
        try (Reader reader = new StringReader(s)) {
          this.config = new TypedProperties().loadAndResolve(reader);
        }
      } catch (Exception e) {
        if (debugEnabled) log.debug("could not load default configuration", e);
      }
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
    String algorithm = config.getString("jppf.load.balancing.algorithm", null);
    if (algorithm == null) algorithm = defaultConfig.config().getString("jppf.load.balancing.algorithm");
    String profileName = config.getString("jppf.load.balancing.strategy", null);
    if (profileName == null) profileName = config.getString("jppf.load.balancing.profile", null);
    if (profileName == null) {
      String prefix = "jppf.load.balancing.profile";
      profileName = defaultConfig.config().getString(prefix, "jppf");
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
    if (providerMap == null) {
      loadProviders();
      if (providerMap == null) return null;
    }
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
    String prefix = "jppf.load.balancing.profile." + profileName + '.';
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
    String prefix2 = "jppf.load.balancing.profile." + profileName + '.';
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
}
