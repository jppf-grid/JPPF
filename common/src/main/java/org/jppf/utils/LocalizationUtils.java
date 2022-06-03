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

import java.io.File;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.*;

/**
 * Utility methods to localize messages in the JPPF components.
 * @author Laurent Cohen
 * @exclude
 */
public final class LocalizationUtils {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(LocalizationUtils.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Whether to log the {@link MissingResourceException}s.
   */
  private static final boolean SHOW_EXCEPTIONS = JPPFConfiguration.getProperties().getBoolean("jppf.show.localization.errors", false);
  /**
   * Cache of resources bundles that were already found missing, to avoid repeating the search and filling the log uselessly.
   */
  private static final Map<String, Boolean> notFoundBundleCache = new ConcurrentHashMap<>();

  /**
   * Get a localized property value.
   * @param baseName the base name to use, in combination with the default locale, to lookup the appropriate resource bundle.
   * @param key the key for the localized value to lookup.
   * @return the name localized through the default locale information, or the key itself if it could not be localized.
   */
  public static String getLocalized(final String baseName, final String key) {
    return getLocalized(baseName, key, key);
  }

  /**
   * Get a localized array of property values.
   * @param baseName the base name to use, in combination with the default locale, to lookup the appropriate resource bundle.
   * @param keys the keys for which to lookup a localized value.
   * @return an array of localized values looked up using the default locale. If a key could not be localized, it is returned as the value.
   */
  public static String[] getLocalized(final String baseName, final String...keys) {
    return getLocalized(baseName, Locale.getDefault(), keys);
  }

  /**
   * Get a localized array of property values.
   * @param baseName the base name to use, in combination with the default locale, to lookup the appropriate resource bundle.
   * @param locale the locale for which to lookup localized values.
   * @param keys the keys for which to lookup a localized value.
   * @return an array of localized values looked up using the default locale. If a key could not be localized, it is returned as the value.
   */
  public static String[] getLocalized(final String baseName, final Locale locale, final String...keys) {
    if (keys == null) return new String[0];
    final String[] localized = new String[keys.length];
    for (int i=0; i<keys.length; i++) localized[i] = getLocalized(baseName, keys[i], keys[i], locale);
    return localized;
  }

  /**
   * Get a localized property value.
   * @param baseName the base name to use, in combination with the default locale, to lookup the appropriate resource bundle.
   * @param key the key for the localized value to lookup.
   * @param locale the locale for which to lookup localized values.
   * @return the name localized through the default locale information, or the key itself if it could not be localized.
   */
  public static String getLocalized(final String baseName, final String key, final Locale locale) {
    return getLocalized(baseName, key, key, locale);
  }

  /**
   * Get a localized property value.
   * @param baseName the base name to use, in combination with the default locale, to lookup the appropriate resource bundle.
   * @param key the key for the localized value to lookup.
   * @param def the default value to return if no localized string could be found.
   * @return the name localized through the default locale information, or the key itself if it could not be localized.
   */
  public static String getLocalized(final String baseName, final String key, final String def) {
    return getLocalized(baseName, key, def, Locale.getDefault());
  }

  /**
   * Get a localized property value for the specified locale.
   * @param baseName the base name to use, in combination with the default locale, to lookup the appropriate resource bundle.
   * @param key the key for the localized value to lookup.
   * @param def the default value to return if no localized string could be found.
   * @param locale the locale for which to lookup a localized value.
   * @return the name localized through the default locale information, or the key itself if it could not be localized.
   */
  public static String getLocalized(final String baseName, final String key, final String def, final Locale locale) {
    if ((baseName == null) || notFoundBundleCache.containsKey(baseName)) return def;
    ResourceBundle bundle = null;
    try {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) cl = LocalizationUtils.class.getClassLoader();
      bundle = ResourceBundle.getBundle(baseName, locale, cl);
    } catch (final Exception e) {
      notFoundBundleCache.put(baseName, Boolean.TRUE);
      if (SHOW_EXCEPTIONS && debugEnabled) log.debug("Could not find resource bundle \"" + baseName +  '\"', e);
      return def;
    }
    String result = def;
    try {
      result = bundle.getString(key);
    } catch (final Exception e) {
      if (SHOW_EXCEPTIONS && debugEnabled) log.debug("Could not find key \"" + key + "\" in resource bundle \"" + baseName +  '\"', e);
    }
    return result;
  }

  /**
   * Get a localized property value for the specified locale.
   * @param baseName the base name to use, in combination with the default locale, to lookup the appropriate resource bundle.
   * @param key the key for the localized value to lookup.
   * @param def the default value to return if no localized string could be found.
   * @param locale the locale for which to lookup a localized value.
   * @param params optional parameters for parametrized messages.
   * @return the name localized through the default locale information, or the key itself if it could not be localized.
   */
  public static String getLocalized(final String baseName, final String key, final String def, final Locale locale, final Object...params) {
    String result = getLocalized(baseName, key, def, locale);
    try {
      if ((params != null) && (params.length > 0)) result = MessageFormat.format(result, params);
    } catch (final Exception e) {
      if (SHOW_EXCEPTIONS && debugEnabled) log.debug("Could not find key \"" + key + "\" in resource bundle \"" + baseName +  '\"', e);
    }
    return result;
  }

  /**
   * Compute a localisation base path from a base folder and a file name.
   * @param base the base folder path as a string.
   * @param filename the filename from which to get the resource bundle name.
   * @return the complete path to a resource bundle.
   */
  public static String getLocalisationBase(final String base, final String filename) {
    String result = null;
    try {
      final File file = new File(filename);
      result = file.getName();
      final int idx = result.lastIndexOf('.');
      if (idx >= 0) result = result.substring(0, idx);
      result = base + '/' + result;
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    return result;
  }
}
