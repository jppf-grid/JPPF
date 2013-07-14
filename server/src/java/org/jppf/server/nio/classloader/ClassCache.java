/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.server.nio.classloader;

import java.util.Map;

import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.collections.SoftReferenceValuesMap;
import org.slf4j.*;

/**
 * A cache for resources loaded by client class loaders.
 * @author Laurent Cohen
 */
public class ClassCache
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClassCache.class);
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The cache of class definition, this is done to not flood the provider when it dispatch many tasks. it uses
   * a soft map to minimize the OutOfMemory.
   */
  private final Map<CacheClassKey, CacheClassContent> classCache = new SoftReferenceValuesMap<CacheClassKey, CacheClassContent>();
  /**
   * Determines whether this cache is enabled. 
   */
  private boolean enabled = JPPFConfiguration.getProperties().getBoolean("jppf.server.class.cache.enabled", true);

  /**
   * Add a resource content to the class cache.
   * @param uuid uuid of the resource provider.
   * @param name name of the resource.
   * @param content content of the resource.
   */
  public void setCacheContent(final String uuid, final String name, final byte[] content)
  {
    if (!enabled) return;
    if (traceEnabled) log.trace("adding cache entry with key=[" + uuid + ", " + name + ']');
    CacheClassContent cacheContent = new CacheClassContent(content);
    CacheClassKey cacheKey = new CacheClassKey(uuid, name);
    synchronized(classCache)
    {
      classCache.put(cacheKey, cacheContent);
    }
  }

  /**
   * Get a resource definition from the resource cache.
   * @param uuid uuid of the resource provider.
   * @param name name of the resource.
   * @return the content of the resource as an array of bytes.
   */
  public byte[] getCacheContent(final String uuid, final String name)
  {
    if (!enabled) return null;
    if (traceEnabled) log.trace("looking up key=[" + uuid + ", " + name + ']');
    CacheClassContent content;
    synchronized(classCache)
    {
      content = classCache.get(new CacheClassKey(uuid, name));
    }
    return content != null ? content.getContent() : null;
  }
}
