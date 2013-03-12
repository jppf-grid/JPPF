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

package org.jppf.utils.collections;

import java.util.*;
import java.util.concurrent.locks.Lock;

/**
 * Interface for maps whose values are collections of a given component type.
 * @param <K> the type of keys in the map.
 * @param <V> the type of values in the collections mapped to the keys.
 * @author Laurent Cohen
 */
public interface CollectionMap<K, V> extends Iterable<V>
{
  /**
   * Add a value for the specified key.
   * @param key the key for which to add a value.
   * @param value the value to add.
   */
  void putValue(final K key, final V value);

  /**
   * Remove a value from the specified key.
   * @param key the key from which to remove a value.
   * @param value the value to remove.
   * @return <code>true</code> if an element was removed, <code>false</code> otherwise.
   */
  boolean removeValue(final K key, final V value);

  /**
   * Add the specified values to the specified key. This is a bulk operation.
   * @param key the key to which to add the values.
   * @param values the values to add to the key.
   */
  void addValues(final K key, final V... values);

  /**
   * Remove the specified values from the specified key. This is a bulk operation.
   * @param key the key for which to rmeove the values.
   * @param values the values to remove.
   * @return the number of values that were actually removed, possibly zero.
   */
  int removeValues(final K key, final V... values);

  /**
   * Remove the specified key fromt his maap.
   * @param key the key to remove.
   * @return collection of values that were removed, possibly <code>null</code>.
   */
  Collection<V> removeKey(final K key);

  /**
   * Get the total number of elements in this collection map.
   * @return the number of elemets as an int value.
   */
  int size();

  /**
   * Determine whether this map is empty.
   * @return <code>true</code> if the map is empty, <code>false</code> otherwise.
   */
  boolean isEmpty();

  /**
   * Determine whether the collection mapped to the specified key contains the specified value.
   * @param key the key whose mapped collection is looked up^.
   * @param value the value to look in the collection.
   * @return <code>true</code> if the map contains the key and the corresponding collection contains the value, <code>false</code> otehrwise.
   */
  boolean contains(final K key, final V value);

  /**
   * Determine whether at least one of the collections in the map contains the specified value.
   * @param value the value to look up in the entire map.
   * @return <code>true</code> if the map contains the value, <code>false</code> otehrwise.
   */
  boolean contains(final V value);

  /**
   * Get an iterator which uses the specified lock.
   * @param lock the lock used to synchronize access to the map.
   * @return an iterator on the values of the map.
   */
  Iterator<V> iterator(final Lock lock);

  /**
   * Get the set of keys in the map.
   * @return a {@link Set} of all keys.
   */
  Set<K> keySet();

  /**
   * Get the set map entries.
   * @return a {@link Set} of all map entries.
   */
  Set<Map.Entry<K, Collection<V>>> entrySet();

  /**
   * Clear the map.
   */
  void clear();
}