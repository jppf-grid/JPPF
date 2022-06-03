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

package org.jppf.utils.collections;

import java.util.*;

/**
 * A sorted map whose values are sorted sets of a given component type.
 * @param <K> the type of keys in the map.
 * @param <V> the type of values in the collections mapped to the keys.
 * @author Laurent Cohen
 */
public class SortedSetSortedMap<K, V> extends AbstractCollectionSortedMap<K, V> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Comparator used to sort the keys.
   */
  protected Comparator<V> valueComparator;

  /**
   * Default cosntructor.
   */
  public SortedSetSortedMap() {
    super();
  }

  /**
   * Default cosntructor.
   * @param comparator tomparator used to sort the keys.
   */
  public SortedSetSortedMap(final Comparator<K> comparator) {
    super(comparator);
  }

  /**
   * Default cosntructor.
   * @param keyComparator comparator used to sort the keys.
   * @param valueComparator comparator used to sort the values.
   */
  public SortedSetSortedMap(final Comparator<K> keyComparator, final Comparator<V> valueComparator) {
    super(keyComparator);
    this.valueComparator = valueComparator;
  }

  @Override
  protected Collection<V> newCollection() {
    return valueComparator == null ? new TreeSet<>() : new TreeSet<>(valueComparator);
  }
}
