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

package org.jppf.jmx;

import java.util.*;

import org.jppf.utils.TypedProperties;
import org.jppf.utils.configuration.*;

/**
 *
 * @author Laurent Cohen
 * @exclude
 */
public class JMXEnvHelper {
  /**
   * Get the value of a property of type {@code long}. The lookup is performed in this order: environment, config, system properties.
   * @param prop the property to look for.
   * @param env the environment passed to a JMX remote connector client or server, may be {@code null}.
   * @param config JPPF configuration, may be {@code null}.
   * @return the value of the property if the property is found, or the property's default value if it is not found.
   */
  public static long getLong(final JPPFProperty<Long> prop, final Map<String, ?> env, final TypedProperties config) {
    final List<String> names = getAllNames(prop);
    Long value = null;
    if (env != null) {
      for (final String name: names) {
        final Object o = env.get(name);
        if (o != null) {
          value = (o instanceof Number) ? ((Number) o).longValue() : Long.valueOf(o.toString());
          break;
        }
      }
    }
    if ((value == null) && (config != null)) value = config.get(prop);
    if (value == null) {
      for (final String name: names) {
        value = Long.getLong(name);
        if (value != null) break;
      }
    }
    if (value == null) value = prop.getDefaultValue();
    else if (prop instanceof LongProperty) {
      final LongProperty p = (LongProperty) prop;
      if (p.hasMinAndMax() && ((value < p.getMinValue()) || (value > p.getMaxValue()))) value = prop.getDefaultValue();
    }
    return value;
  }

  /**
   * Get the value of a property of type {@code int}. The lookup is performed in this order: environment, config, system properties.
   * @param prop the property to look for.
   * @param env the environment passed to a JMX remote connector client or server, may be {@code null}.
   * @param config JPPF configuration, may be {@code null}.
   * @return the value of the property if the property is found, or the property's default value if it is not found.
   */
  public static int getInt(final JPPFProperty<Integer> prop, final Map<String, ?> env, final TypedProperties config) {
    final List<String> names = getAllNames(prop);
    Integer value = null;
    if (env != null) {
      for (final String name: names) {
        final Object o = env.get(name);
        if (o != null) {
          value = (o instanceof Number) ? ((Number) o).intValue() : Integer.valueOf(o.toString());
          break;
        }
      }
    }
    if ((value == null) && (config != null)) value = config.get(prop);
    if (value == null) {
      for (final String name: names) {
        value = Integer.getInteger(name);
        if (value != null) break;
      }
    }
    if (value == null) value = prop.getDefaultValue();
    else if (prop instanceof IntProperty) {
      final IntProperty p = (IntProperty) prop;
      if (p.hasMinAndMax() && (value < p.getMinValue()) || (value > p.getMaxValue())) value = prop.getDefaultValue();
    }
    return value;
  }

  /**
   * Get the value of a property of type {@code boolean}. The lookup is performed in this order: environment, config, system properties.
   * @param prop the property to look for.
   * @param env the environment passed to a JMX remote connector client or server, may be {@code null}.
   * @param config JPPF configuration, may be {@code null}.
   * @return the value of the property if the property is found, or the property's default value if it is not found.
   */
  public static boolean getBoolean(final JPPFProperty<Boolean> prop, final Map<String, ?> env, final TypedProperties config) {
    final List<String> names = getAllNames(prop);
    if (env != null) {
      for (final String name: names) {
        final Object o = env.get(name);
        if (o != null) return (o instanceof Boolean) ? (Boolean) o : Boolean.valueOf(o.toString());
      }
    }
    if ((config != null) && config.containsProperty(prop)) return config.get(prop);
    for (final String name: names) {
      final String s = System.getProperty(name);
      if (s != null) return Boolean.valueOf(s);
    }
    return prop.getDefaultValue();
  }

  /**
   * Get the value of a property of type {@code String}. The lookup is performed in this order: environment, config, system properties.
   * @param prop the property to look for.
   * @param env the environment passed to a JMX remote connector client or server, may be {@code null}.
   * @param config JPPF configuration, may be {@code null}.
   * @return the value of the property if the property is found, or the property's default value if it is not found.
   */
  public static String getString(final JPPFProperty<String> prop, final Map<String, ?> env, final TypedProperties config) {
    final List<String> names = getAllNames(prop);
    String value = null;
    if (env != null) {
      for (final String name: names) {
        final Object o = env.get(name);
        if (o != null) return o.toString();
      }
    }
    if ((config != null) && ((value = config.get(prop)) != null)) return value;
    for (final String name: names) {
      value = System.getProperty(name);
      if (value != null) return value;
    }
    return prop.getDefaultValue();
  }

  /**
   * @return whether asynchronous logging is enabled.
   * @exclude
   */
  public static boolean isAsyncLoggingEnabled() {
    return false;
  }

  /**
   * Get a list made of a prperty's names and all its aliases.
   * @param prop the property to process.
   * @return a list of names for the property.
   */
  private static List<String> getAllNames(final JPPFProperty<?> prop) {
    final List<String> names = new ArrayList<>(1 + prop.getAliases().length);
    names.add(prop.getName());
    for (final String alias: prop.getAliases()) names.add(alias);
    return names;
  }
}
