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

package org.jppf.utils.collections;

import java.util.*;

/**
 * A sorted map whose values are linked lists of a given component type.
 * @param <K> the type of keys in the map.
 * @param <V> the type of values in the collections mapped to the keys.
 * @author Laurent Cohen
 */
public class LinkedListSortedMap<K, V> extends AbstractCollectionSortedMap<K, V>
{
  /**
   * Default constructor.
   */
  public LinkedListSortedMap()
  {
    super();
  }

  /**
   * Initialize this collection sorted map with the specified comparator.
   * @param comparator comparator used to sort the keys.
   */
  public LinkedListSortedMap(final Comparator<K> comparator)
  {
    super(comparator);
  }

  @Override
  protected Collection<V> newCollection()
  {
    return new LinkedList<>();
  }

  /**
   * Look up the specified value in thel ist corresponding to the specified key and move it to then end of the list.
   * @param key the key for which to look up in the corresponding list.
   * @param value the value to look up.
   */
  public void moveToEndOfList(final K key, final V value)
  {
    LinkedList<V> list = (LinkedList<V>) map.get(key);
    if ((list == null) || list.isEmpty() || list.getLast().equals(value)) return;
    list.remove(value);
    list.add(value);
  }
}
