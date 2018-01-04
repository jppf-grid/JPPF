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
import java.util.concurrent.*;

/**
 * A {@link ConcurrentHashMap} whose values are {@link CopyOnWriteArrayList} instances.
 * @param <K> the type of the keys.
 * @param <V> the type of the objects in the map"'s collection values.
 * @author Laurent Cohen
 */
public class SynchronizedLinkedListConcurrentMap<K, V> extends AbstractCollectionConcurrentMap<K, V> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Default constructor.
   */
  public SynchronizedLinkedListConcurrentMap() {
    this.map = createMap();
  }

  @Override
  protected Map<K, Collection<V>> createMap() {
    return new ConcurrentHashMap<>();
  }

  @Override
  protected Collection<V> newCollection() {
    return Collections.synchronizedList(new LinkedList<V>());
  }
}
