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

import java.util.*;

/**
 * A sorted map whose values are sets of a given component type.
 * @param <K> the type of keys in the map.
 * @param <V> the type of values in the collections mapped to the keys.
 * @author Laurent Cohen
 */
public abstract class AbstractCollectionSortedMap<K, V> extends AbstractCollectionMap<K, V> implements CollectionSortedMap<K, V> {
  /**
   * Comparator used to sort the keys.
   */
  protected final Comparator<K> comparator;

  /**
   * Default cosntructor.
   */
  public AbstractCollectionSortedMap() {
    this(null);
  }

  /**
   * Initialize this collection sorted map with the specified comparator.
   * @param comparator comparator used to sort the keys.
   */
  public AbstractCollectionSortedMap(final Comparator<K> comparator) {
    this.comparator = comparator;
    map = createMap();
  }

  @Override
  protected Map<K, Collection<V>> createMap() {
    return comparator == null ? new TreeMap<K, Collection<V>>() : new TreeMap<K, Collection<V>>(comparator);
  }

  @Override
  @SuppressWarnings("unchecked")
  public K firstKey() {
    return ((SortedMap<K, V>) map).firstKey();
  }

  @Override
  @SuppressWarnings("unchecked")
  public K lastKey() {
    return ((SortedMap<K, V>) map).lastKey();
  }
}
