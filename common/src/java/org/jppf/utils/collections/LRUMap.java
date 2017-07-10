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

package org.jppf.utils.collections;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * A cache with an LRU eviction policy.
 * @param <K> the type fo the keys.
 * @param <V> the type fo the values.
 * @author Laurent Cohen
 */
public class LRUMap<K, V> extends LinkedHashMap<K, V> {
  /**
   * The maximum cache capacity
   */
  private final int maxCapacity;

  /**
   * Initialize this cache with a default max capacity of 1024.
   */
  public LRUMap() {
    this(1024);
  }

  /**
   * Initialize this cache with the specified max capacity.
   * @param maxCapacity the maximum capacity of this cache.
   */
  public LRUMap(final int maxCapacity) {
    this.maxCapacity = maxCapacity;
  }

  @Override
  protected boolean removeEldestEntry(final Entry<K, V> eldest) {
    return size() >= maxCapacity;
  }
}
