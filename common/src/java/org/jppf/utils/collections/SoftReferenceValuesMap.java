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

import java.lang.ref.*;
import java.util.*;

import org.slf4j.*;

/**
 * Map whose values are soft references. When a value has been garbage-collected,
 * the corresponding map entry is removed.
 * @param <K> the type of the keys.
 * @param <V> the type of the values.
 * @author Laurent Cohen
 */
public class SoftReferenceValuesMap<K, V> extends AbstractMap<K, V> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SoftReferenceValuesMap.class);
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The soft references queue.
   */
  private ReferenceQueue<V> refQueue;
  /**
   * The underlying map that backs this soft map.
   */
  private Map<K, SoftValue<K, V>> map;

  /**
   * Default constructor.
   */
  public SoftReferenceValuesMap() {
    refQueue = new ReferenceQueue<>();
    map = createMap();
  }

  /**
   * Create the underlying map of soft references.
   * @return a new {@code Map} instance.
   */
  Map<K, SoftValue<K, V>> createMap() {
    return new HashMap<>();
  }

  @Override
  public int size() {
    cleanup();
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    cleanup();
    return map.isEmpty();
  }

  @Override
  public V get(final Object key) {
    cleanup();
    SoftReference<V> ref = map.get(key);
    return ref == null ? null : ref.get();
  }

  @Override
  @SuppressWarnings("unchecked")
  public V put(final K key, final V value) {
    cleanup();
    SoftReference<V> ref = map.put(key, new SoftValue(key, value, refQueue));
    return ref == null ? null : ref.get();
  }

  @Override
  public V remove(final Object key) {
    cleanup();
    SoftReference<V> ref = map.remove(key);
    return ref == null ? null : ref.get();
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    throw new UnsupportedOperationException("This operation is not implemented");
  }

  @Override
  public void clear() {
    cleanup();
    map.clear();
  }

  /**
   * Cleanup the reference queue, by removing entries whose value was garbage collected.
   */
  @SuppressWarnings("unchecked")
  private void cleanup() {
    SoftValue<K, V> ref;
    while ((ref = (SoftValue) refQueue.poll()) != null) {
      // NPE on this line ==>
      K key = ref.key;
      if (key == null) continue;
      if (traceEnabled) log.trace("removing entry for key=" + key);
      map.remove(key);
      ref.key = null;
    }
  }

  /**
   * Extension of SoftReference that holds the map key, so the corresponding entry
   * can be removed from the map.
   * @param <K> the type of the key.
   * @param <V> the type of the value.
   */
  static class SoftValue<K, V> extends SoftReference<V> {
    /**
     * The associated key.
     */
    private K key;

    /**
     * Initialize this reference with the specified key and value.
     * @param key the key.
     * @param value the value.
     * @param queue the reference queue to use.
     */
    public SoftValue(final K key, final V value, final ReferenceQueue<V> queue) {
      super(value, queue);
      this.key = key;
    }
  }
}
