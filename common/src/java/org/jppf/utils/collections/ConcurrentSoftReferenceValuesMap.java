/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A concurrent map whose values are soft references. When a value has been garbage-collected,
 * the corresponding map entry is removed.
 * @param <K> the type of the keys.
 * @param <V> the type of the values.
 * @author Laurent Cohen
 */
public class ConcurrentSoftReferenceValuesMap<K, V> extends SoftReferenceValuesMap<K, V> {
  @Override
  Map<K, SoftReferenceValuesMap.SoftValue<K, V>> createMap() {
    return new ConcurrentHashMap<>();
  }
}
