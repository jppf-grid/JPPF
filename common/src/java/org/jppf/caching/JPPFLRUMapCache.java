/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.util.*;
import java.util.Map.Entry;

/**
 * A map cache with a LRU eviction policy based on a specified capacity.
 * @param <K> the type of keys.
 * @param <V> the type of values.
 * @author Laurent Cohen
 */
public class JPPFLRUMapCache<K, V> extends AbstractJPPFMapCache<K, V> {
  /**
   * The capacity of this cache.
   */
  private int capacity = 1024;

  /**
   * Initialize this cache with an initial capacity of 1024.
   */
  public JPPFLRUMapCache() {
    this(1024);
  }

  /**
   * Initialize this cache with the specified capacity.
   * @param capacity the capacity of this cache.
   */
  public JPPFLRUMapCache(final int capacity) {
    this.capacity = capacity;
    map = createMap();
  }

  @Override
  protected Map<K, V> createMap() {
    return new LinkedHashMap<K, V>(capacity, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(final Entry<K, V> eldest) {
        return size() > capacity;
      }
    };
  }

  /**
   * Get the capacity of this cache.
   * @return the capacity as an {@code int}.
   */
  public int getCapacity() {
    return capacity;
  }

  /**
   * Set the capacity of this cache.
   * @param capacity the capacity as an {@code int}.
   */
  public void setCapacity(final int capacity) {
    this.capacity = capacity;
  }
}
