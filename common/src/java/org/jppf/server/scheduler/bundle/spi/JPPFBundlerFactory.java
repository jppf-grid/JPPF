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

package org.jppf.server.scheduler.bundle.spi;

import java.util.*;

import org.jppf.JPPFException;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Factory class for JPPF load-balancing algorithms defined through the {@link org.jppf.server.scheduler.bundle.spi.JPPFBundlerProvider JPPFBundlerProvider}
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
   * Map of all registered providers.
   */
  private Map<String, JPPFBundlerProvider> providerMap = null;

  /**
   * Create an instance of the bundler with the specified name and parameters.
   * @param name the name of the bundler's algorithm, such as specified in the bundler provider and in the configuration.
   * @param configuration a map of algorithm parameters to their value.
   * @return a new <code>Bundler</code> instance.
   * @throws Exception if the bundler could not be created.
   */
  public Bundler createBundler(final String name, final TypedProperties configuration) throws Exception
  {
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
  public Bundler createBundlerFromJPPFConfiguration() throws Exception
  {
    TypedProperties props = JPPFConfiguration.getProperties();
    String algorithm = props.getString("jppf.load.balancing.algorithm", null);
    // for compatibility with v1.x configuration files
    if (algorithm == null) algorithm = props.getString("task.bundle.strategy", "manual");
    String profileName = props.getString("jppf.load.balancing.strategy", null);
    // for compatibility with v1.x configuration files
    if (profileName == null) profileName = props.getString("task.bundle.autotuned.strategy", "jppf");
    TypedProperties configuration = convertJPPFConfiguration(profileName, props);
    return createBundler(algorithm, configuration);
  }

  /**
   * Get the bundler provider with the specified name.<br>
   * This method will trigger a lazy loading of the providers if they haven't been loaded yet.
   * @param name the name of the bundler provider to retrieve.
   * @return a <code>JPPFBundlerProvider</code> instance or null if the provider could not be found.
   * @throws Exception if any error occurs while loading the providers.
   */
  public JPPFBundlerProvider getBundlerProvider(final String name) throws Exception
  {
    if (providerMap == null) loadProviders();
    return providerMap.get(name);
  }

  /**
   * Get the names of all discovered bundler providers.
   * @return a list of provider names.
   * @throws Exception if any error occurs while loading the providers.
   */
  public List<String> getBundlerProviderNames() throws Exception
  {
    if (providerMap == null) loadProviders();
    return new ArrayList<String>(providerMap.keySet());
  }

  /**
   * Retrieve all the bundler providers configured through the service provider interface (SPI).
   * @throws Exception if any error occurs while loading the providers.
   */
  private void loadProviders() throws Exception
  {
    Map<String, JPPFBundlerProvider> map = new Hashtable<String, JPPFBundlerProvider>();
    Iterator<JPPFBundlerProvider> it = ServiceFinder.lookupProviders(JPPFBundlerProvider.class);
    while (it.hasNext())
    {
      JPPFBundlerProvider provider = it.next();
      map.put(provider.getAlgorithmName(), provider);
      if (debugEnabled) log.debug("registering new load-balancing algorithm provider '" + provider.getAlgorithmName() + '\'');
    }
    providerMap = map;
  }

  /**
   * Convert a JPPF configuration map to a profile configuration by extracting the properties related to the specified profile
   * and removing the JPPF-specific prefix from their name.
   * @param profileName the name of the profile to extract.
   * @param configuration the JPPF configuration to extract from.
   * @return a <code>TypedProperties</code> instance containing only the profile-specific parameters.
   */
  public TypedProperties convertJPPFConfiguration(final String profileName, final TypedProperties configuration)
  {
    TypedProperties profile = extractJPPFConfiguration(profileName, configuration);
    String prefix = "strategy." + profileName + '.';
    TypedProperties result = new TypedProperties();
    for (Map.Entry<Object, Object> entry: profile.entrySet())
    {
      String key = (String) entry.getKey();
      String s = key.substring(prefix.length());
      result.setProperty(s, (String) entry.getValue());
    }
    return result;
  }

  /**
   * Extract the JPPF-prefixed load-balancing parameters from the specified configuration and based on the specified profile name.
   * @param profileName - the name of the profile to extract.
   * @param configuration - the JPPF configuration to extract from.
   * @return a <code>TypedProperties</code> instance containing only the profile-specific parameters.
   */
  public TypedProperties extractJPPFConfiguration(final String profileName, final TypedProperties configuration)
  {
    TypedProperties profile = new TypedProperties();
    String prefix = "strategy." + profileName + '.';
    Set<Map.Entry<Object, Object>> entries = configuration.entrySet();
    for (Map.Entry<Object, Object> entry: entries)
    {
      if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String))
      {
        String key = (String) entry.getKey();
        if (key.startsWith(prefix)) profile.setProperty(key, (String) entry.getValue());
      }
    }
    return profile;
  }
}
