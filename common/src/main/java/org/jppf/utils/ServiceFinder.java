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
import java.net.URL;
import java.util.*;

import org.slf4j.*;

/**
 * Instances of this class look for and find services implemented via the Service Provider Interface (SPI).
 * This class is an alternative for the static methods found in {@link javax.imageio.spi.ServiceRegistry}.
 * @author Laurent Cohen
 * @exclude
 */
public class ServiceFinder {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServiceFinder.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Find the classes of all providers implementing or extending the specified provider interface or class, using the specified class loader.
   * @param <T> The type of provider implementations to find.
   * @param providerClass the provider class.
   * @param loader the class loader to use for the lookup.
   * @param single determines whether only the first looked up provider should be returned, or all the providers found.
   * @return a list of concrete providers of the specified type.
   */
  @SuppressWarnings("unchecked")
  public <T> List<Class<? extends T>> findProviderClassess(final Class<T> providerClass, final ClassLoader loader, final boolean single) {
    if (providerClass == null) throw new IllegalArgumentException("Provider class cannot be null");
    ClassLoader cl = loader;
    if (cl == null) cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) cl = getClass().getClassLoader();
    final List<Class<? extends T>> list = new ArrayList<>();
    final String name = providerClass.getName();
    final List<String> lines = findServiceDefinitions("META-INF/services/" + name, cl);
    final Set<String> alreadyLoaded = new HashSet<>();
    for (String s: lines) {
      if (alreadyLoaded.contains(s)) continue;
      try {
        final Class<? extends T> clazz = (Class<? extends T>) cl.loadClass(s);
        list.add(clazz);
        if (single) break;
        alreadyLoaded.add(s);
      } catch(final Exception|NoClassDefFoundError e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
        else log.warn(ExceptionUtils.getMessage(e));
      }
    }
    return list;
  }

  /**
   * Find all providers implementing or extending the specified provider interface or class, using the specified class loader.
   * @param <T> The type of provider implementations to find.
   * @param providerClass the provider class.
   * @param cl the class loader to use for the lookup.
   * @param single determines whether only the first looked up provider should be returned, or all the providers found.
   * @return a list of concrete providers of the specified type.
   */
  public <T> List<T> findProviders(final Class<T> providerClass, final ClassLoader cl, final boolean single) {
    final List<Class<? extends T>> classes = findProviderClassess(providerClass, cl, single);
    final List<T> list = new ArrayList<>();
    for (Class<? extends T> c: classes) {
      try {
        final T t = c.newInstance();
        list.add(t);
      } catch(final Exception|NoClassDefFoundError e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
        else log.warn(ExceptionUtils.getMessage(e));
      }
    }
    return list;
  }

  /**
   * Find all providers implementing or extending the specified provider interface or class, using the specified class loader.
   * @param <T> The type of provider implementations to find.
   * @param providerClass the provider class.
   * @param cl the class loader to user for the lookup.
   * @return a list of concrete providers of the specified type.
   */
  public <T> List<T> findProviders(final Class<T> providerClass, final ClassLoader cl) {
    return findProviders(providerClass, cl, false);
  }

  /**
   * Find all providers implementing or extending the specified provider interface or class, using the context class loader.
   * @param <T> The type of provider implementations to find.
   * @param providerClass the provider class.
   * @return a list of concrete providers of the specified type.
   */
  public <T> List<T> findProviders(final Class<T> providerClass) {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) cl = getClass().getClassLoader();
    return findProviders(providerClass, cl);
  }

  /**
   * Get a list of resources with the specified path found in the classpath.
   * @param path the path of the resources to find.
   * @param cl the class loader to user for the lookup.
   * @return a list of URLs.
   */
  private static List<URL> resourcesList(final String path, final ClassLoader cl) {
    final List<URL> urls = new ArrayList<>();
    try {
      final Enumeration<URL> enu = cl.getResources(path);
      while (enu.hasMoreElements()) urls.add(enu.nextElement());
    } catch(final IOException e) {
      final String s = ExceptionUtils.getMessage(e);
      if (debugEnabled) log.debug(s, e);
      else log.warn(s);
    }
    return urls;
  }

  /**
   * Find all service definitions in the classpath, that match the specified path.
   * @param path the path to the definition files to find.
   * @param cl the class loader to use for classpath lookup.
   * @return the definitions found as a list of strings.
   */
  public List<String> findServiceDefinitions(final String path, final ClassLoader cl) {
    final List<String> lines = new ArrayList<>();
    try {
      final List<URL> urls = resourcesList(path, cl);
      for (final URL url: urls) {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
          final List<String> fileLines = FileUtils.textFileAsLines(reader);
          for (String s: fileLines) {
            final int idx = s.indexOf('#');
            if (idx > 0) s = s.substring(0, idx);
            s = s.trim();
            if (!s.startsWith("#")&& (s.length() > 0)) lines.add(s);
          }
        }
      }
    } catch(final IOException e) {
      final String s = ExceptionUtils.getMessage(e);
      if (debugEnabled) log.debug(s, e);
      else log.warn(s);
    }
    return lines;
  }

  /**
   * Find all providers implementing or extending the specified provider interface or class, using the specified class loader.
   * @param <T> The type of provider implementations to find.
   * @param providerClass the provider class.
   * @param cl the class loader to user for the lookup.
   * @param single determines whether only the first looked up provider should be returned, or all the providers found.
   * @return an iterator over concrete providers of the specified type.
   */
  public static <T> Iterator<T> lookupProviders(final Class<T> providerClass, final ClassLoader cl, final boolean single) {
    return new ServiceFinder().findProviders(providerClass, cl).iterator();
  }

  /**
   * Find all providers implementing or extending the specified provider interface or class, using the specified class loader.
   * @param <T> The type of provider implementations to find.
   * @param providerClass the provider class.
   * @param cl the class loader to user for the lookup.
   * @return an iterator over concrete providers of the specified type.
   */
  public static <T> Iterator<T> lookupProviders(final Class<T> providerClass, final ClassLoader cl) {
    return lookupProviders(providerClass, cl, false);
  }

  /**
   * Find all providers implementing or extending the specified provider interface or class, using the context class loader.
   * @param <T> The type of provider implementations to find.
   * @param providerClass the provider class.
   * @return an iterator over concrete providers of the specified type.
   */
  public static <T> Iterator<T> lookupProviders(final Class<T> providerClass) {
    return new ServiceFinder().findProviders(providerClass).iterator();
  }
}
