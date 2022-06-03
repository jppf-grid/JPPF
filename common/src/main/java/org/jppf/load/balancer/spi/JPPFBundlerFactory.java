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

package org.jppf.load.balancer.spi;

import java.util.*;

import org.jppf.load.balancer.*;
import org.jppf.load.balancer.impl.*;
import org.jppf.load.balancer.persistence.LoadBalancerPersistence;
import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * Factory class for JPPF load-balancing algorithms defined through the {@link org.jppf.load.balancer.spi.JPPFBundlerProvider JPPFBundlerProvider}
 * service provider interface.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFBundlerFactory {
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
  private final Map<String, JPPFBundlerProvider<? extends LoadBalancingProfile>> providerMap = new TreeMap<>();
  /**
   * Mapping of algorithm names to a hash identifier. The hashes are cached so we don't have to re-compute them each time.
   */
  private final Map<String, String> nameToHash = new HashMap<>();
  /**
   * Mapping of algorithm hashes to a their name. This is a reverse mapping of {@link #nameToHash}.
   */
  private final Map<String, String> hashToName = new HashMap<>();
  /**
   * The default values to use if nothing is specified in the JPPF configuration.
   */
  private final Defaults defaultConfig;
  /**
   * The current load-balancing configuration.
   */
  private LoadBalancingInformation currentInfo;
  /**
   * An unmodifiable list of all discovered algorithm names.
   */
  private final List<String> algorithmNames;
  /**
   * The last update timestamp.
   */
  private long lastUpdateTime;
  /**
   * The fallback bundler.
   */
  private final Bundler<?> fallback = createFallbackBundler();
  /**
   * The configuration used to extract the load-balancer settings.
   */
  private final TypedProperties config;
  /**
   * The configured persistence.
   */
  private final LoadBalancerPersistence persistence;
  /**
   * The hash function used to generate bundle state identifiers.
   */
  private final String hashAlgorithm;

  /**
   * Default constructor.
   * @param config the configuration used to extract the load-balancer settings.
   */
  public JPPFBundlerFactory(final TypedProperties config) {
    this(Defaults.SERVER, config);
  }

  /**
   * Default constructor.
   * @param def the default values to use if nothing is specified in the JPPF configuration.
   * @param config the configuration used to extract the load-balancer settings.
   */
  public JPPFBundlerFactory(final Defaults def, final TypedProperties config) {
    this.config = config;
    defaultConfig = def;
    this.hashAlgorithm = config.get(JPPFProperties.LOAD_BALANCING_PERSISTENCE_HASH);
    loadProviders();
    algorithmNames = Collections.unmodifiableList(new ArrayList<>(providerMap.keySet()));
    updateCurrentConfiguration();
    if (debugEnabled) log.debug("using default properties: " + defaultConfig);
    persistence = initPersistence();
  }

  /**
   * Create an instance of the bundler with the specified name and parameters.
   * @return a new <code>Bundler</code> instance.
   */
  @SuppressWarnings("unchecked")
  public Bundler<?> newBundler() {
    final LoadBalancingInformation info = getCurrentInfo();
    @SuppressWarnings("rawtypes")
    final JPPFBundlerProvider provider = getBundlerProvider(info.getAlgorithm());
    final LoadBalancingProfile profile = provider.createProfile(info.getParameters());
    return provider.createBundler(profile);
  }

  /**
   * Update the current load-balancer settings from the JPF configuration.
   * @return the created {@link LoadBalancingInformation} instance.
   */
  public LoadBalancingInformation updateCurrentConfiguration() {
    String algorithm = config.getString(JPPFProperties.LOAD_BALANCING_ALGORITHM.getName(), null);
    if (algorithm == null) algorithm = defaultConfig.config().get(JPPFProperties.LOAD_BALANCING_ALGORITHM);
    String profileName = config.getString(JPPFProperties.LOAD_BALANCING_PROFILE.getName(), null);
    if (profileName == null) {
      final String prefix = JPPFProperties.LOAD_BALANCING_PROFILE.getName();
      profileName = defaultConfig.config().get(JPPFProperties.LOAD_BALANCING_PROFILE);
      final String prefixDot = prefix + '.';
      for (final Map.Entry<Object, Object> entry: defaultConfig.config().entrySet()) {
        if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
          final String key = (String) entry.getKey();
          if (!config.containsKey(key) && key.startsWith(prefixDot)) config.put(key, entry.getValue());
        }
      }
    }
    final TypedProperties configuration = convertJPPFConfiguration(profileName, config);
    if (debugEnabled) log.debug("load balancing configuration using algorithm '" + algorithm +"' with parameters: " + configuration);
    return setAndGetCurrentInfo(new LoadBalancingInformation(algorithm, configuration));
  }

  /**
   * Get the bundler provider with the specified name.<br>
   * This method will trigger a lazy loading of the providers if they haven't been loaded yet.
   * @param name the name of the bundler provider to retrieve.
   * @return a {@link JPPFBundlerProvider} instance or null if the provider could not be found.
   * @param <T> the type of profile used by the bundlers the returned provider creates.  
   */
  @SuppressWarnings("unchecked")
  public <T extends LoadBalancingProfile> JPPFBundlerProvider<T> getBundlerProvider(final String name) {
    return (JPPFBundlerProvider<T>) providerMap.get(name);
  }

  /**
   * Get the names of all discovered bundler providers.
   * @return an unmodifiable list of provider names.
   */
  public List<String> getBundlerProviderNames() {
    return algorithmNames;
  }

  /**
   * Get the hashed identifier for the specified algorithm.
   * @param name the name of the algorithm for which to get an identifier.
   * @return a hash of the algorithm name.
   */
  public String getAlgorithmHash(final String name) {
    return nameToHash.get(name);
  }

  /**
   * Get the name of an algorithm given its hash.
   * @param hash the hash of the algorithm name.
   * @return a the name of the algorithm.
   */
  public String getAlgorithmNameFromHash(final String hash) {
    return hashToName.get(hash);
  }

  /**
   * Retrieve all the bundler providers configured through the service provider interface (SPI).
   */
  private void loadProviders() {
    final ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
    final ClassLoader currentCL = getClass().getClassLoader();
    if (debugEnabled) log.debug("oldCL=" + oldCL + ", currentCL=" + currentCL);
    final boolean isDiff = (oldCL != currentCL);
    final ClassLoader[] loaders = (isDiff) ? new ClassLoader[] { currentCL, oldCL } : new ClassLoader[] { oldCL };
    for (final ClassLoader cl: loaders) {
      @SuppressWarnings("rawtypes")
      final Iterator<JPPFBundlerProvider> it = ServiceFinder.lookupProviders(JPPFBundlerProvider.class, cl);
      while (it.hasNext()) {
        final JPPFBundlerProvider<?> provider = it.next();
        final String name = provider.getAlgorithmName();
        providerMap.put(name, provider);
        final String hash = CryptoUtils.computeHash(name, hashAlgorithm);
        nameToHash.put(name, hash);
        hashToName.put(hash, name);
        if (debugEnabled) log.debug("registering new load-balancing algorithm provider '" + provider.getAlgorithmName() + '\'');
      }
      if (debugEnabled) log.debug("found " + providerMap.size() + " load-balancing algorithms in the classpath");
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
    final TypedProperties profile = extractJPPFConfiguration(profileName, configuration);
    final String prefix = JPPFProperties.LOAD_BALANCING_PROFILE.getName() + '.' + profileName + '.';
    final TypedProperties result = new TypedProperties();
    for (final Map.Entry<Object, Object> entry: profile.entrySet()) {
      final String key = (String) entry.getKey();
      final String s = key.substring(prefix.length());
      result.setProperty(s, (String) entry.getValue());
    }
    return result;
  }

  /**
   * Extract the JPPF-prefixed load-balancing parameters from the specified configuration and based on the specified profile name.<br/>
   * All entries in the resulting map have a key starting with "{@code jppf.load.balancing.profile.<profileName>}"
   * @param profileName the name of the profile to extract.
   * @param configuration the JPPF configuration to extract from.
   * @return a <code>TypedProperties</code> instance containing only the profile-specific parameters.
   */
  private static TypedProperties extractJPPFConfiguration(final String profileName, final TypedProperties configuration) {
    final TypedProperties profile = new TypedProperties();
    final String prefix = "strategy." + profileName + '.';
    final String prefix2 = JPPFProperties.LOAD_BALANCING_PROFILE.getName() + '.' + profileName + '.';
    for (final Map.Entry<Object, Object> entry: configuration.entrySet()) {
      if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
        final String key = (String) entry.getKey();
        if (key.startsWith(prefix)) {
          final String propName = key.substring(prefix.length());
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
    final String prefix = JPPFProperties.LOAD_BALANCING_PROFILE.getName() + ".jppf.";
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
    final String prefix = JPPFProperties.LOAD_BALANCING_PROFILE.getName() + ".jppf.";
    return new TypedProperties().set(JPPFProperties.LOAD_BALANCING_ALGORITHM, "manual")
        .set(JPPFProperties.LOAD_BALANCING_PROFILE, "jppf")
        .setInt(prefix + "size", 1_000_000);
  }

  /**
   * Get the current load-balancing configuration.
   * @return a {@link LoadBalancingInformation} instance.
   */
  public synchronized LoadBalancingInformation getCurrentInfo() {
    return currentInfo;
  }

  /**
   * Set the current load-balancing configuration.
   * @param currentInfo a {@link LoadBalancingInformation} instance.
   * @return the updated {@link LoadBalancingInformation} instance.
   */
  public synchronized LoadBalancingInformation setAndGetCurrentInfo(final LoadBalancingInformation currentInfo) {
    this.currentInfo = currentInfo;
    lastUpdateTime = System.currentTimeMillis();
    return currentInfo;
  }

  /**
   * Get the last update time.
   * @return the last update time as a long value.
   */
  public synchronized long getLastUpdateTime() {
    return lastUpdateTime;
  }

  /**
   * Get the fallback bundler.
   * @return a {@link Bundler} instance.
   */
  public Bundler<?> getFallbackBundler() {
    return fallback;
  }

  /**
   * Create new instance of default bundler.
   * @return a new {@link Bundler} instance.
   */
  private static Bundler<?> createFallbackBundler() {
    final FixedSizeProfile profile = new FixedSizeProfile(new TypedProperties().setInt("size", 1));
    return new FixedSizeBundler(profile);
  }

  /**
   * Not instantiable from another class.
   * @return {@link LoadBalancerPersistence} implementation based on the configuration.
   */
  private LoadBalancerPersistence initPersistence() {
    try {
      final JPPFProperty<String[]> prop = JPPFProperties.LOAD_BALANCING_PERSISTENCE;
      return ReflectionHelper.invokeDefaultOrStringArrayConstructor(LoadBalancerPersistence.class, prop.getName(), config.get(prop));
    } catch (final Exception e) {
      log.error("error creating LoadBalancerPersistence configured as {} = {}, load-balancer persistence is disabled\n{}",
        JPPFProperties.LOAD_BALANCING_PERSISTENCE.getName(), JPPFConfiguration.get(JPPFProperties.JOB_PERSISTENCE), ExceptionUtils.getStackTrace(e));
    }
    return null;
  }

  /**
   * @return the configured persistence, or {@code null} if none is configured or the initialization failed.
   */
  public LoadBalancerPersistence getPersistence() {
    return persistence;
  }

  /**
   * @return the hash function used to generate bundle state identifiers.
   */
  public String getHashAlgorithm() {
    return hashAlgorithm;
  }
}
