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

package org.jppf.jmxremote;

import java.util.Map;

import org.jppf.utils.TypedProperties;
import org.jppf.utils.configuration.*;

/**
 *
 * @author Laurent Cohen
 */
public class JMXEnvHelper {
  /**
   * Get the value of a property of type {@code long}. The lookup is performed in this order: environment, config, system properties.
   * @param prop the property to look for.
   * @param env the environment passed to a JMX remote connector client or server).
   * @param config JPPF configuration.
   * @return the value of the proerty if the property is found, or the property's default value if it is not found.
   */
  public static long getLong(final JPPFProperty<Long> prop, final Map<String, ?> env, final TypedProperties config) {
    long value = prop.getDefaultValue();
    String name = prop.getName();
    if (env.containsKey(name)) value = (Long) env.get(name);
    else if (config.containsProperty(prop)) value = config.get(prop);
    else { 
      String s = System.getProperty(name);
      if (s != null) value = Long.valueOf(s);
    }
    if (prop instanceof LongProperty) {
      LongProperty p = (LongProperty) prop;
      if ((value < p.getMinValue()) || (value > p.getMaxValue())) value = prop.getDefaultValue();
    }
    return value;
  }

  /**
   * Get the value of a property of type {@code int}. The lookup is performed in this order: environment, config, system properties.
   * @param prop the property to look for.
   * @param env the environment passed to a JMX remote connector client or server).
   * @param config JPPF configuration.
   * @return the value of the proerty if the property is found, or the property's default value if it is not found.
   */
  public static int getInt(final JPPFProperty<Integer> prop, final Map<String, ?> env, final TypedProperties config) {
    int value = 0;
    String name = prop.getName();
    if (env.containsKey(name)) value = (Integer) env.get(name);
    else if (config.containsProperty(prop)) value = config.get(prop);
    else { 
      String s = System.getProperty(name);
      if (s != null) value = Integer.valueOf(s);
    }
    if (prop instanceof IntProperty) {
      IntProperty p = (IntProperty) prop;
      if ((value < p.getMinValue()) || (value > p.getMaxValue())) value = prop.getDefaultValue();
    }
    return value;
  }

  /**
   * Get the value of a property of type {@code boolean}. The lookup is performed in this order: environment, config, system properties.
   * @param prop the property to look for.
   * @param env the environment passed to a JMX remote connector client or server).
   * @param config JPPF configuration.
   * @return the value of the proerty if the property is found, or the property's default value if it is not found.
   */
  public static boolean getBoolean(final JPPFProperty<Boolean> prop, final Map<String, ?> env, final TypedProperties config) {
    String name = prop.getName();
    if (env.containsKey(name)) return (Boolean) env.get(name);
    if (config.containsProperty(prop)) return config.get(prop);
    String value = System.getProperty(name);
    if (value != null) return Boolean.valueOf(value);
    return prop.getDefaultValue();
  }

  /**
   * @return whether asynchronous logging is enabled.
   * @exclude
   */
  public static boolean isAsyncLoggingEnabled() {
    return false;
  }
}
