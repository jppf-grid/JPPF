/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.caching;

/**
 * Interface for caches implementations with a map-like API.
 * @param <K> the type of keys.
 * @param <V> the type of values.
 * @author Laurent Cohen
 */
public interface JPPFMapCache<K, V>
{
  /**
   * Put an element in the cache.
   * @param key the element's key for retrival.
   * @param value the element's value.
   */
  void put(K key, V value);

  /**
   * Determine whether an entry with the specified key is in the cache.
   * @param key the key to look up.
   * @return {@code true} if the key is in the cache, {@code false} otherwise.
   */
  boolean has(K key);

  /**
   * Get an element in the cache.
   * @param key the element's key.
   * @return the value of the element, or <code>null</code> if the element is not in the cache.
   */
  V get(K key);

  /**
   * Remove an element from the cache.
   * @param key the element's key.
   * @return the value of the removed element, or <code>null</code> if the element is not in the cache.
   */
  V remove(K key);

  /**
   * Clear the cache. This removes all elements from the cache.
   */
  void clear();
}
