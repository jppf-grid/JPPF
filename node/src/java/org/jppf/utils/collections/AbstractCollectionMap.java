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
 * A map whose values are collections of a given component type.
 * @param <K> the type of keys in the map.
 * @param <V> the type of values in the collections mapped to the keys.
 * @author Laurent Cohen
 */
public abstract class AbstractCollectionMap<K, V> implements Iterable<V>, CollectionMap<K, V>
{
  /**
   * The underlying map to which operations are delegated.
   */
  protected Map<K, Collection<V>> map = null;

  /**
   * Default constructor.
   */
  public AbstractCollectionMap()
  {
  }

  /**
   * Add a value for the specified key.
   * @param key the key for which to add a value.
   * @param value the value to add.
   */
  @Override
  public void putValue(final K key, final V value)
  {
    Collection<V> coll = map.get(key);
    if (coll == null)
    {
      coll = newCollection();
      map.put(key, coll);
    }
    coll.add(value);
  }

  /**
   * Remove a value from the specified key.
   * @param key the key from which to remove a value.
   * @param value the value to remove.
   * @return <code>true</code> if an element was removed, <code>false</code> otherwise.
   */
  @Override
  public boolean removeValue(final K key, final V value)
  {
    Collection<V> coll = map.get(key);
    if (coll != null)
    {
      boolean b = coll.remove(value);
      if (coll.isEmpty()) map.remove(key);
      return b;
    }
    return false;
  }

  /**
   * Add the specified values to the specified key. This is a bulk operation.
   * @param key the key to which to add the values.
   * @param values the values to add to the key.
   */
  @Override
  public void addValues(final K key, final V...values)
  {
    Collection<V> coll = map.get(key);
    if (coll == null)
    {
      coll = newCollection();
      map.put(key, coll);
    }
    for (V value: values) coll.add(value);
  }

  /**
   * Remove the specified values from the specified key. This is a bulk operation.
   * @param key the key for which to rmeove the values.
   * @param values the values to remove.
   * @return the number of values that were actually removed, possibly zero.
   */
  @Override
  public int removeValues(final K key, final V...values)
  {
    Collection<V> coll = map.get(key);
    if (coll != null)
    {
      int count = 0;
      for (V value: values)
      {
        if (coll.remove(value)) count++;
      }
      if (coll.isEmpty()) map.remove(key);
      return count;
    }
    return 0;
  }

  /**
   * Remove the specified key fromt his maap.
   * @param key the key to remove.
   * @return collection of values that were removed, possibly <code>null</code>.
   */
  @Override
  public Collection<V> removeKey(final K key)
  {
    return map.remove(key);
  }

  /**
   * Get the total number of elements in this collection map.
   * @return the number of elemets as an int value.
   */
  @Override
  public int size()
  {
    int result = 0;
    for (Map.Entry<K, Collection<V>> entry: map.entrySet()) result += entry.getValue().size();
    return result;
  }

  /**
   * Determine whether this map is empty.
   * @return <code>true</code> if the map is empty, <code>false</code> otherwise.
   */
  @Override
  public boolean isEmpty()
  {
    return map.isEmpty();
  }

  /**
   * Determine whether the collection mapped to the specified key contains the specified value.
   * @param key the key whose mapped collection is looked up^.
   * @param value the value to look in the collection.
   * @return <code>true</code> if the map contains the key and the corresponding collection contains the value, <code>false</code> otehrwise.
   */
  @Override
  public boolean contains(final K key, final V value)
  {
    Collection<V> coll = map.get(key);
    if (coll == null) return false;
    return coll.contains(value);
  }

  /**
   * Determine whether at least one of the collections in the map contains the specified value.
   * @param value the value to look up in the entire map.
   * @return <code>true</code> if the map contains the value, <code>false</code> otehrwise.
   */
  @Override
  public boolean contains(final V value)
  {
    for (Map.Entry<K, Collection<V>> entry: map.entrySet())
    {
      if (entry.getValue().contains(value)) return true;
    }
    return false;
  }

  @Override
  public Iterator<V> iterator()
  {
    return new CollectionMapIterator();
  }

  /**
   * Get an iterator which uses the specified lock.
   * @param lock the lock used to synchronize access to the map.
   * @return an iterator on the values of the map.
   */
  @Override
  public Iterator<V> iterator(final Lock lock)
  {
    return new CollectionMapIterator(lock);
  }

  /**
   * Clear the map.
   */
  @Override
  public void clear()
  {
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
  public String toString()
  {
    return map.toString();
  }

  /**
   * An itrator on the values in the mapped collections of this map.
   */
  private class CollectionMapIterator implements Iterator<V>
  {
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
    public CollectionMapIterator()
    {
      this(null);
    }

    /**
     * Initialize this iterator.
     * @param lock used to synchronize with the queue.
     */
    public CollectionMapIterator(final Lock lock)
    {
      this.lock = lock;
      lock();
      try
      {
        entryIterator = map.entrySet().iterator();
        if (entryIterator.hasNext()) listIterator = entryIterator.next().getValue().iterator();
      }
      finally
      {
        unlock();
      }
    }

    /**
     * Determines whether an element remains to visit.
     * @return true if there is at least one element that hasn't been visited, false otherwise.
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext()
    {
      lock();
      try
      {
        return entryIterator.hasNext() || ((listIterator != null) && listIterator.hasNext());
      }
      finally
      {
        unlock();
      }
    }

    /**
     * Get the next element for this iterator.
     * @return the next element as a <code>JPPFTaskBundle</code> instance.
     * @see java.util.Iterator#next()
     */
    @Override
    public V next()
    {
      lock();
      try
      {
        if (listIterator != null)
        {
          if (listIterator.hasNext()) return listIterator.next();
          if (entryIterator.hasNext())
          {
            listIterator = entryIterator.next().getValue().iterator();
            if (listIterator.hasNext()) return listIterator.next();
          }
        }
        throw new NoSuchElementException("no more element in this BundleIterator");
      }
      finally
      {
        unlock();
      }
    }

    /**
     * This operation is not supported and throws an <code>UnsupportedOperationException</code>.
     * @throws UnsupportedOperationException as this operation is not supported.
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() throws UnsupportedOperationException
    {
      throw new UnsupportedOperationException("remove() is not supported on a BundleIterator");
    }

    /**
     * Perform a lock if a lock is present.
     */
    private void lock()
    {
      if (lock != null) lock.lock();
    }

    /**
     * Perform an unlock if a lock is present.
     */
    private void unlock()
    {
      if (lock != null) lock.unlock();
    }
  }
}
