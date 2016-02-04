/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.util.Set;
import java.util.regex.*;

import org.jppf.utils.TypedProperties;
import org.slf4j.*;

/**
 * Handles property substitutions in a properties file, that is resolve all
 * references of the following form in properties values:
 * <pre> property.name = ${other.property.name}
 * some.property.name = ${env.&lt;environment_variable_name&gt;}
 * other.property.name = ${sys.&lt;system_property_name&gt;}</pre>
 * @author Laurent Cohen
 * @exclude
 */
public class SubstitutionsHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SubstitutionsHandler.class);
  /**
   * Determines whether trace log statements are enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Provider for environment variables.
   */
  private static final PropertyProvider ENV_PROVIDER = new PropertyProvider("env.", "System.getEnv()") {
    @Override public String getValue(final String key) {
      return System.getenv(key);
    }
  };
  /**
   * Provider for system properties.
   */
  private static final PropertyProvider SYS_PROVIDER = new PropertyProvider("sys.", "System.getProperties()") {
    @Override public String getValue(final String key) {
      return System.getProperty(key);
    }
  };
  /**
   * The known property providers.
   */
  private static final PropertyProvider[] PROPERTY_PROVIDERS = { ENV_PROVIDER, SYS_PROVIDER };
  /**
   * The regex pattern for identifying substitutable property references. This pattern uses explicit reluctant quantifiers, as opposed
   * to the default greedy quantifiers, to avoid problems when multiple property references are found in a single property value.
   */
  private static final Pattern SUBST_PATTERN = Pattern.compile("(?:\\$\\{){1}?(.*?)\\}+?");
  /**
   * Stores the properties whose values are fully resolved.
   */
  private final TypedProperties resolvedProps = new TypedProperties();
  /**
   * Number of properties resolved at each iteration, used as a
   * stop conidition for the resolution loop.
   */
  private int resolutionCount;
  /**
   *
   */
  private Matcher matcher = null;

  /**
   * Initialize this substitution handler.
   */
  public SubstitutionsHandler() {
  }

  /**
   * Resolve the substitutions in the input {@link TypedProperties} object.
   * This method actually changes the property values in the input, i.e. it mutates the input {@link TypedProperties} object.
   * @param props the properties where substitutions must be resolved.
   * @return TypedProperties object in which substitutions have been resolved whenever possible.
   */
  public TypedProperties resolve(final TypedProperties props) {
    int i = 0;
    if (traceEnabled) log.trace("starting substitution handling");
    Set<String> set = props.stringPropertyNames();
    resolutionCount = 1;
    while (resolutionCount > 0) {
      resolutionCount = 0;
      i++;
      for (String key: set) {
        String value = evaluateProp(key, props.getProperty(key));
        props.setProperty(key, value);
      }
      if (traceEnabled) log.trace("iteration {} : resolutionCount = {}", i, resolutionCount);
    }
    resolvedProps.clear();
    return props;
  }

  /**
   * Resolve the substitutions for the specified property.
   * @param key the name of the property.
   * @param value the current value of the property.
   * @return the new value of the property after 0 or more substitutions have been handled.
   */
  private String evaluateProp(final String key, final String value) {
    matcher = SUBST_PATTERN.matcher(value);
    StringBuilder sb = new StringBuilder();
    int pos = 0;
    int matches = 0;
    int resolvedRefCount = 0;
    if (traceEnabled) log.trace("evaluating [key={}, value={}]", key, value);
    while (matcher.find()) {
      matches++;
      String resolvedValue = null;
      sb.append(value.substring(pos, matcher.start()));
      String name = matcher.group(1);
      if (traceEnabled) log.trace("  found match [name={}]", name);
      if (name == null) name = "";
      boolean done = false;
      for (PropertyProvider provider: PROPERTY_PROVIDERS) {
        if (name.startsWith(provider.prefix)) {
          resolvedRefCount += resolveSpecialProperty(provider, name, value, sb);
          done = true;
          break;
        }
      }
      if (!done) {
        if (resolvedProps.containsKey(name)) {
          resolvedValue = resolvedProps.getProperty(name);
          if (!"".equals(name.trim())) resolvedRefCount++;
          if (traceEnabled) log.trace("  property already resolved [name={}, value={}]", name, resolvedValue);
        } else {
          resolvedValue = value.substring(matcher.start(), matcher.end());
          if ("".equals(name.trim())) {
            if (traceEnabled) log.trace("  empty property name [name={}]", name);
            resolvedRefCount++;
            resolvedProps.put(name, resolvedValue);
          } else {
            if (traceEnabled) log.trace("  unresolved property [name={}, value={}]", name, resolvedValue);
          }
        }
        sb.append(resolvedValue);
      }
      pos = matcher.end();
    }
    if (pos < value.length()) sb.append(value.substring(pos, value.length()));
    String s = sb.toString();
    if (resolvedRefCount > 0) resolutionCount++;
    if ((matches <= 0) || (resolvedRefCount >= matches)) resolvedProps.put(key, s);
    if (traceEnabled) log.trace("final value [key={}, value={}]", key, s);
    return s;
  }

  /**
   *
   * @param provider used to lookup the property or variable name.
   * @param name the name of the property or variables to look for.
   * @param value the raw, unresolved value.
   * @param valueBuilder an appendable string for the property value being computed.
   * @return 0 if the variable or property had already been resolved previously, 1 otherwise.
   */
  private int resolveSpecialProperty(final PropertyProvider provider, final String name, final String value, final StringBuilder valueBuilder) {
    String resolvedValue = null;
    int resolvedRefCount = 0;
    if (resolvedProps.containsKey(name)) {
      resolvedValue = resolvedProps.getProperty(name);
      if (traceEnabled) log.trace(String.format("  property from %s already resolved [name=%s, value=%s]", provider.mapName, name, resolvedValue));
    } else {
      resolvedRefCount++;
      String var = name.substring(provider.prefix.length());
      if (var == null) var = "";
      if (!"".equals(var)) resolvedValue = provider.getValue(var);
      if (resolvedValue != null) {
        if (traceEnabled) log.trace(String.format("  got property from %s : [envVar=%s, value=%s]", provider.mapName, var, resolvedValue));
      } else {
        resolvedValue = value.substring(matcher.start(), matcher.end());
        if (traceEnabled) log.trace(String.format("  property not found in %s : [envVar=%s, value=%s]", provider.mapName, var, resolvedValue));
      }
      resolvedProps.put(name, resolvedValue);
    }
    valueBuilder.append(resolvedValue);
    return resolvedRefCount;
  }

  /**
   * A provider for properties or variables to substitute in the configuration.
   */
  private static abstract class PropertyProvider {
    /**
     * The prefix of the properties to substitute.
     */
    public final String prefix;
    /**
     * Only used in trace logging.
     */
    public final String mapName;

    /**
     * Initialize this provider.
     * @param prefix the prefix of the properties to substitute.
     * @param mapName only used in trace logging.
     */
    PropertyProvider(final String prefix, final String mapName) {
      this.prefix = prefix;
      this.mapName = mapName;
    }

    /**
     * Get the value for the spciefied key.
     * @param key the name of the property or variable to lookup^.
     * @return the vakue for th espeicifed key or {@code null} if the value could not be found.
     */
    public abstract String getValue(final String key);
  }
}
