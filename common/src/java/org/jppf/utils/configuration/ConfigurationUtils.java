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

package org.jppf.utils.configuration;

import java.lang.reflect.*;
import java.util.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Extract JPPF properties defined as public constants in a given class.
 * @author Laurent Cohen
 */
public class ConfigurationUtils {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ConfigurationUtils.class);

  /**
   * Get the list of all predefined configuration properties in the specified class.
   * @param c the class containing the proeprties constants.
   * @return A list of {@link JPPFProperty} instances.
  */
  public synchronized static List<JPPFProperty<?>> allProperties(final Class<?> c) {
    final List<JPPFProperty<?>> properties = new ArrayList<>();
    try {
      // compute the base loalisation bundle name
      final String i18nBase = c.getPackage().getName() + ".i18n." + c.getSimpleName();
      final Field[] fields = c.getDeclaredFields();
      for (Field field: fields) {
        final int mod = field.getModifiers();
        if (Modifier.isStatic(mod) && Modifier.isPublic(mod) && Modifier.isFinal(mod) && JPPFProperty.class.isAssignableFrom(field.getType())) {
          if (!field.isAccessible()) field.setAccessible(true);
          final JPPFProperty<?> prop = (JPPFProperty<?>) field.get(null);
          if (prop instanceof AbstractJPPFProperty) ((AbstractJPPFProperty<?>) prop).setI18nBase(i18nBase);
          properties.add(prop);
        }
      }
    } catch (final Exception e) {
      log.error("error listing the properties in class {}:\n{}", c, ExceptionUtils.getStackTrace(e));
    }
    return properties;
  }

  /**
   * @param props the properties to clean up.
   * @return a new {@link TypedProperties} object where passwords are replaced with a dummy string.
   */
  public static Properties hidePasswords(final Properties props) {
    if (props == null) return null;
    final Properties  result = new Properties();
    final String[] keys = { "password", "pwd", "passw" };
    for (final String name: props.stringPropertyNames()) {
      if (StringUtils.hasOneOf(name, true, keys)) result.setProperty(name, "********");
      else result.setProperty(name, props.getProperty(name));
    }
    return result;
  }


  /**
   * Parse a memory size value in format [size][unit] where:
   * <ul>
   * <li>size is a positive {@code long} value</li>
   * <li>unit is one of 'g', 'm', 'k', 'b' or uppercase equivalents 'G', 'M', 'K', 'B'. If the unit string is anything else then it defaults to 'b'</li>
   * </ul>
   * Examples: 2g, 1536M, 123456k, 123456789b
   * @param source the string to parse.
   * @return parse the used memory threshold that triggers user data offloading, from the configuration.
   */
  public static long parseDataSize(final String source) {
    char unit = 0;
    int i;
    for (i=0; i<source.length(); i++) {
      final char c = source.charAt(i);
      if ((i == 0) && (c == '-')) continue;
      if (!Character.isDigit(c)) {
        unit = Character.toLowerCase(c);
        break;
      }
    }
    long threshold = 0;
    try {
      threshold = Long.valueOf(source.substring(0, i));
    } catch (@SuppressWarnings("unused") final Exception e) {
      threshold = (long) (0.8d * Runtime.getRuntime().maxMemory());
    }
    switch(unit) {
      case 'g':
        threshold *= 1024L * 1024L * 1024L;
        break;
      case 'm':
        threshold *= 1024L * 1024L;
        break;
      case 'k':
        threshold *= 1024L;
        break;
    }
    return threshold;
  }

  /**
   * Get a value from a specified environment variable, or a default value if the variable is not set.
   * @param envVariable the environment variable name.
   * @param defaultValue the default value to use.
   * @return the value of the enviornement variable if it is not null, or the default value.
   */
  public static String getFromEnv(final String envVariable, final String defaultValue) {
    final String result = StringUtils.unquote(System.getenv(envVariable));
    return (result == null) || "null".equals(result) || "".equals(result) ? defaultValue : result;
  }

  /**
   * Get a value from a first environment variable, or a second environment variable if the first is not set, or a default value if the variables are not set.
   * @param envVar1 the first environment variable name.
   * @param envVar2 the second environment variable name.
   * @param defaultValue the default value to use.
   * @return the value of the enviornement variable if it is not null, or the default value.
   */
  public static String getFromEnv(final String envVar1, final String envVar2, final String defaultValue) {
    return getFromEnv(envVar1, getFromEnv(envVar2, defaultValue));
  }

  /**
   * Get a boolean value from a specified environment variable, or a default value if the variable is not set.
   * @param envVariable the environment variable name.
   * @param defaultValue the default value to use.
   * @return the value of the enviornement variable if it is not null, or the default value.
   */
  public static boolean getBooleanFromEnv(final String envVariable, final boolean defaultValue) {
    final String result = StringUtils.unquote(System.getenv(envVariable));
    return (result == null) || "null".equals(result) || "".equals(result) ? defaultValue : Boolean.valueOf(result);
  }
}
