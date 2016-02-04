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
import java.util.concurrent.ConcurrentHashMap;

/**
 * A concurrent et implementation backed by a ConcurrentHashMap.
 * @param <E> the type of objects in the set.
 * @author Laurent Cohen
 */
public class ConcurrentHashSet<E> implements Set<E>
{
  /**
   * The backing map for this concurrent hash set.
   */
  private final Map<E, Boolean> map;

  /**
   * Create a new, empty set with a default initial capacity of 16, a load factor of 0.75 and a concurrency level of 16.
   */
  public ConcurrentHashSet()
  {
    map = new ConcurrentHashMap<>();
  }

  /**
   * Create a new, empty set with the specified initial capacity, a load factor of 0.75 and a concurrency level of 16
   * @param initialCapacity  the initial capacity.
   */
  public ConcurrentHashSet(final int initialCapacity)
  {
    map = new ConcurrentHashMap<>(initialCapacity);
  }

  /**
   * Create a new, empty set with the specified initial capacity, load factor and a concurrency level of 16.
   * @param initialCapacity  the initial capacity.
   * @param loadFactor the load factor threshold, used to control resizing.
   */
  public ConcurrentHashSet(final int initialCapacity, final float loadFactor)
  {
    map = new ConcurrentHashMap<>(initialCapacity, loadFactor);
  }

  /**
   * Create a new, empty map with the specified initial capacity, load factor and concurrency level.
   * @param initialCapacity  the initial capacity.
   * @param loadFactor the load factor threshold, used to control resizing.
   * @param concurrencyLevel the estimated number of concurrently updating threads.
   */
  public ConcurrentHashSet(final int initialCapacity, final float loadFactor, final int concurrencyLevel)
  {
    map = new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
  }

  @Override
  public int size()
  {
    return map.size();
  }

  @Override
  public boolean isEmpty()
  {
    return map.isEmpty();
  }

  @Override
  public boolean contains(final Object o)
  {
    return map.containsKey(o);
  }

  @Override
  public Iterator<E> iterator()
  {
    return map.keySet().iterator();
  }

  @Override
  public Object[] toArray()
  {
    return map.keySet().toArray();
  }

  @Override
  public <T> T[] toArray(final T[] a)
  {
    return map.keySet().toArray(a);
  }

  @Override
  public boolean add(final E e)
  {
    return map.put(e, Boolean.TRUE) != null;
  }

  @Override
  public boolean remove(final Object o)
  {
    return map.remove(o) != null;
  }

  @Override
  public boolean containsAll(final Collection<?> c)
  {
    return map.keySet().containsAll(c);
  }

  @Override
  public boolean addAll(final Collection<? extends E> c)
  {
    boolean result = false;
    for (E e: c) result |= add(e);
    return result;
  }

  @Override
  public boolean retainAll(final Collection<?> c)
  {
    int count = 0;
    Iterator<E> it = iterator();
    while (it.hasNext())
    {
      E e = it.next();
      if (!c.contains(e))
      {
        it.remove();
        count++;
      }
    }
    return count > 0;
  }

  @Override
  public boolean removeAll(final Collection<?> c)
  {
    boolean result = false;
    for (Object o: c) result |= remove(o);
    return result;
  }

  @Override
  public void clear()
  {
    map.clear();
  }
}
