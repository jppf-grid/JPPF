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

package org.jppf.client;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.utils.collections.SoftReferenceValuesMap;
import org.slf4j.*;

/**
 * A cache of claspath resources on the client side.
 * @author Laurent Cohen
 * @exclude
 */
public final class ClasspathCache {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ClasspathCache.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Singleton instance of this cache.
   */
  private static final ClasspathCache instance = new ClasspathCache();
  /**
   * The cache.
   */
  private final Map<ClassLoader, Map<String, byte[]>> cache = new SoftReferenceValuesMap<>();
  /**
   * Cache statistics.
   */
  private final AtomicLong nbRequests = new AtomicLong(0L), nbHits = new AtomicLong(0L);

  /**
   * Instantiation is not permitted outside this class.
   */
  private ClasspathCache() {
  }

  /**
   * Get a resource at the specified path of the specified class loader's classpath.
   * @param loader the class loader to lookup.
   * @param path the resource path.
   * @return an array of bytes containing the rsource content.
   */
  public byte[] get(final ClassLoader loader, final String path) {
    try {
      nbRequests.incrementAndGet();
      if ((loader == null) || (path == null)) return null;
      final Map<String, byte[]> resources;
      synchronized(cache) {
        resources = cache.get(loader);
      }
      if (resources != null) {
        synchronized(resources) {
          final byte[] result = resources.get(path);
          if (result != null) nbHits.incrementAndGet();
          return result;
        }
      }
      return null;
    } finally {
      if (debugEnabled) log.debug(toString());
    }
  }

  /**
   * Add a classpath resource to the cache..
   * @param loader the class loader to lookup.
   * @param path the resource path.
   * @param resource the byte content of the resource.
   */
  public void put(final ClassLoader loader, final String path, final byte[] resource) {
    if ((loader == null) || (path == null) || (resource == null)) return;
    Map<String, byte[]> resources;
    synchronized(cache) {
      resources = cache.get(loader);
      if (resources == null) {
        resources = new SoftReferenceValuesMap<>();
        cache.put(loader, resources);
      }
    }
    synchronized(resources) {
      resources.put(path, resource);
    }
  }

  /**
   * Clear the cache.
   */
  public void clear() {
    synchronized(cache) {
      cache.clear();
    }
  }

  /**
   * Compute the cache size.
   * @return the total number of esources in the cache.
   */
  public long getCacheSize() {
    long size = 0L;
    synchronized(cache) {
      for (final ClassLoader cl: cache.keySet()) {
        final Map<String, byte[]> resources = cache.get(cl);
        if (resources != null) size += resources.size();
      }
    }
    return size;
  }

  /**
   * @return the signleton instance of this cache.
   */
  public static ClasspathCache getInstance() {
    return instance;
  }

  @Override
  public String toString() {
    final long total = nbRequests.get(), hits = nbHits.get();
    return String.format("%s[requests=%,d; misses=%,d; hits=%,d; size=%,d]", getClass().getSimpleName(), total, hits, total - hits, getCacheSize());
  }
}
