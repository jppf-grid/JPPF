/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.util.*;

/**
 * A sorted map whose values are sets of a given component type.
 * @param <K> the type of keys in the map.
 * @param <V> the type of values in the collections mapped to the keys.
 * @author Laurent Cohen
 */
public class SetSortedMap<K, V> extends AbstractCollectionMap<K, V>
{
  @Override
  protected Map<K, Collection<V>> createMap()
  {
    return new TreeMap<K, Collection<V>>();
  }

  @Override
  protected Collection<V> newCollection()
  {
    return new HashSet<V>();
  }

  /**
   * Get the last key in this sorted map.
   * @return the last key based on the order of the map.
   */
  public K lastKey()
  {
    return ((SortedMap<K, ?>) map).lastKey();
  }
}
