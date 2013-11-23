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
 * A  hash map whose values are sets of a specified component type.
 * @param <K> the type of the keys in this map.
 * @param <V> the type of values in each Set mapped to a key.
 * @author Laurent Cohen
 */
public class SetHashMap<K, V> extends AbstractCollectionMap<K, V>
{
  /**
   * Default constructor.
   */
  public SetHashMap()
  {
    map = createMap();
  }

  @Override
  protected Map<K, Collection<V>> createMap()
  {
    return new HashMap<>();
  }

  @Override
  protected Collection<V> newCollection()
  {
    return new HashSet<>();
  }
}
