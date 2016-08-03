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

/**
 * A {@link Hashtable} whose values are {@link Collection} instances (specialized in concrete subclasses).
 * @param <K> the type of the keys.
 * @param <V> the type of the objects in the map's collection values.
 * @author Laurent Cohen
 */
public abstract class AbstractCollectionHashtable<K, V> extends AbstractCollectionMap<K, V>
{
  /**
   * Default constructor.
   */
  public AbstractCollectionHashtable()
  {
    this.map = createMap();
  }

  @Override
  protected Map<K, Collection<V>> createMap()
  {
    return new Hashtable<>();
  }

  @Override
  public void putValue(final K key, final V value)
  {
    Collection<V> coll = createOrGetCollectionSynchronized(key);
    coll.add(value);
  }

  @Override
  public void addValues(final K key, @SuppressWarnings("unchecked") final V... values)
  {
    Collection<V> coll = createOrGetCollectionSynchronized(key);
    for (V value: values) coll.add(value);
  }
}
