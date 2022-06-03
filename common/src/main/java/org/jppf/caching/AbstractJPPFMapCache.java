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

package org.jppf.caching;

import java.util.Map;

/**
 * A map cache with a LRU eviction policy.
 * @param <K> the type of keys.
 * @param <V> the type of values.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFMapCache<K, V> implements JPPFMapCache<K, V> {
  /**
   * The backing map for this cache.
   */
  protected Map<K, V> map;

  /**
   * Create the backing map.
   * @return a map for this cache.
   */
  protected abstract Map<K, V> createMap();

  @Override
  public void put(final K key, final V value) {
    map.put(key, value);
  }

  @Override
  public V get(final K key) {
    return map.get(key);
  }

  @Override
  public V remove(final K key) {
    return map.remove(key);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public boolean has(final K key) {
    return map.containsKey(key);
  }
}
