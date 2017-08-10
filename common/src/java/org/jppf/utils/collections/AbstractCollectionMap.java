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

package org.jppf.utils.collections;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

/**
 * A map whose values are collections of a given component type.
 * @param <K> the type of keys in the map.
 * @param <V> the type of values in the collections mapped to the keys.
 * @author Laurent Cohen
 */
public abstract class AbstractCollectionMap<K, V> implements CollectionMap<K, V> {
  /**
   * The underlying map to which operations are delegated.
   */
  protected Map<K, Collection<V>> map = null;

  /**
   * Default constructor.
   */
  public AbstractCollectionMap() {
  }

  @Override
  public void putValue(final K key, final V value) {
    Collection<V> coll = createOrGetCollection(key);
    coll.add(value);
  }

  @Override
  public boolean removeValue(final K key, final V value) {
    Collection<V> coll = map.get(key);
    if (coll != null) {
      boolean b = coll.remove(value);
      if (coll.isEmpty()) map.remove(key);
      return b;
    }
    return false;
  }

  @Override
  public void addValues(final K key, final Collection<V> values) {
    Collection<V> coll = createOrGetCollection(key);
    coll.addAll(values);
  }

  @Override
  public void addValues(final K key, final V...values) {
    Collection<V> coll = createOrGetCollection(key);
    for (V value: values) coll.add(value);
  }

  @Override
  public int removeValues(final K key, final V...values) {
    Collection<V> coll = map.get(key);
    if (coll != null) {
      int count = 0;
      for (V value: values) {
        if (coll.remove(value)) count++;
      }
      if (coll.isEmpty()) map.remove(key);
      return count;
    }
    return 0;
  }

  @Override
  public Collection<V> removeKey(final K key) {
    return map.remove(key);
  }

  @Override
  public Collection<V> getValues(final K key) {
    return map.get(key);
  }

  @Override
  public int size() {
    int result = 0;
    for (Map.Entry<K, Collection<V>> entry: map.entrySet()) result += entry.getValue().size();
    return result;
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(final K key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(final K key, final V value) {
    Collection<V> coll = map.get(key);
    if (coll == null) return false;
    return coll.contains(value);
  }

  @Override
  public boolean containsValue(final V value) {
    for (Map.Entry<K, Collection<V>> entry: map.entrySet()) {
      if (entry.getValue().contains(value)) return true;
    }
    return false;
  }

  @Override
  public Iterator<V> iterator() {
    return new CollectionMapIterator();
  }

  @Override
  public Iterator<V> iterator(final Lock lock) {
    return new CollectionMapIterator(lock);
  }

  @Override
  public void clear() {
    map.clear();
  }

  /**
   * Create a new  map.
   * @return a new mutable empty map.
   */
  protected abstract Map<K, Collection<V>> createMap();

  /**
   * Create a new collection of values for insertion into the map.
   * @return a new mutable empty collection.
   */
  protected abstract Collection<V> newCollection();

  @Override
  public String toString() {
    return map.toString();
  }

  /**
   * An iterator on the values in the mapped collections of this map.
   */
  private class CollectionMapIterator implements Iterator<V> {
    /**
     * Iterator over the entries in the priority map.
     */
    private Iterator<Map.Entry<K, Collection<V>>> entryIterator = null;
    /**
     * Iterator over the task bundles in the map entry specified by <code>entryIterator</code>.
     */
    private Iterator<V> listIterator = null;
    /**
     * Used for synchronized access to the queue.
     */
    private final Lock lock;

    /**
     * Initialize this iterator.
     */
    public CollectionMapIterator() {
      this(null);
    }

    /**
     * Initialize this iterator.
     * @param lock used to synchronize with the queue.
     */
    public CollectionMapIterator(final Lock lock) {
      this.lock = lock;
      lock();
      try {
        entryIterator = map.entrySet().iterator();
        if (entryIterator.hasNext()) listIterator = entryIterator.next().getValue().iterator();
      } finally {
        unlock();
      }
    }

    /**
     * Determines whether an element remains to visit.
     * @return true if there is at least one element that hasn't been visited, false otherwise.
     */
    @Override
    public boolean hasNext() {
      lock();
      try {
        return entryIterator.hasNext() || ((listIterator != null) && listIterator.hasNext());
      } finally {
        unlock();
      }
    }

    /**
     * Get the next element for this iterator.
     * @return the next element as a <code>JPPFTaskBundle</code> instance.
     */
    @Override
    public V next() {
      lock();
      try {
        if (listIterator != null) {
          if (listIterator.hasNext()) return listIterator.next();
          if (entryIterator.hasNext()) {
            listIterator = entryIterator.next().getValue().iterator();
            if (listIterator.hasNext()) return listIterator.next();
          }
        }
        throw new NoSuchElementException("no more element for this iterator");
      } finally {
        unlock();
      }
    }

    /**
     * This operation is not supported and throws an <code>UnsupportedOperationException</code>.
     * @throws UnsupportedOperationException as this operation is not supported.
     */
    @Override
    public void remove() throws UnsupportedOperationException {
      throw new UnsupportedOperationException("remove() is not supported on a CollectionMapIterator");
    }

    /**
     * Perform a lock if a lock is present.
     */
    private void lock() {
      if (lock != null) lock.lock();
    }

    /**
     * Perform an unlock if a lock is present.
     */
    private void unlock() {
      if (lock != null) lock.unlock();
    }
  }

  @Override
  public Set<K> keySet() {
    return map == null ? null : map.keySet();
  }

  @Override
  public Set<Entry<K, Collection<V>>> entrySet() {
    return map == null ? null : map.entrySet();
  }

  @Override
  public List<V> allValues() {
    List<V> list = new ArrayList<>();
    for (Map.Entry<K, Collection<V>> entry: map.entrySet()) {
      if (!entry.getValue().isEmpty()) list.addAll(entry.getValue());
    }
    return list;
  }

  /**
   * Get an exisitng collection for the specified key, or create it if it doesn't exist.
   * @param key the key for which to get a collection of values.
   * @return a collection of value for the specified keys, may be empty if newly created.
   */
  protected Collection<V> createOrGetCollectionSynchronized(final K key) {
    Collection<V> coll;
    synchronized(map) {
      coll = map.get(key);
      if (coll == null) {
        coll = newCollection();
        map.put(key, coll);
      }
    }
    return coll;
  }

  /**
   * Get an exisitng collection for the specified key, or create it if it doesn't exist.
   * @param key the key for which to get a collection of values.
   * @return a collection of value for the specified keys, may be empty if newly created.
   */
  protected Collection<V> createOrGetCollection(final K key) {
    Collection<V> coll;
    synchronized(map) {
      coll = map.get(key);
      if (coll == null) {
        coll = newCollection();
        map.put(key, coll);
      }
    }
    return coll;
  }
}
