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

package org.jppf.caching;

import java.util.*;

/**
 * Cache implementation which uses a {@link HashMap}.
 * @param <K> the type of keys.
 * @param <V> the type of values.
 * @author Laurent Cohen
 */
public class JPPFHashMapCache<K, V> extends AbstractJPPFMapCache<K, V> {
  /**
   * Initialize this cache.
   */
  public JPPFHashMapCache() {
    map = createMap();
  }

  @Override
  protected Map<K, V> createMap() {
    return new HashMap<>();
  }
}
