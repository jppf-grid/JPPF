/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import java.util.Map;

import org.jppf.utils.collections.SoftReferenceValuesMap;

/**
 * Cache implementation which uses a {@link SoftReferenceValuesMap} with synchronized access.
 * @param <K> the type of keys.
 * @param <V> the type of values.
 * @author Laurent Cohen
 */
public class JPPFSynchronizedSoftCache<K, V> implements JPPFMapCache<K, V> {
  /**
   * The backing map for this cache.
   */
  private final Map<K, V> map = new SoftReferenceValuesMap<>();

  @Override
  public void put(final K key, final V value) {
    synchronized(map) {
      map.put(key, value);
    }
  }

  @Override
  public V get(final K key) {
    synchronized(map) {
      return map.get(key);
    }
  }

  @Override
  public V remove(final K key) {
    synchronized(map) {
      return map.remove(key);
    }
  }

  @Override
  public void clear() {
    synchronized(map) {
      map.clear();
    }
  }

  @Override
  public boolean has(final K key) {
    synchronized(map) {
      return map.containsKey(key);
    }
  }
}
