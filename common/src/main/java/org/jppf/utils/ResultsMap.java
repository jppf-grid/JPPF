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

package org.jppf.utils;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * This class provides convenient methods to inspect and access a mapping of {@link InvocationResult} instances to specified keys.
 * @param <K> the type of the keys in this map.
 * @param <V> the type of non-exceptional values in this map.
 * @author Laurent Cohen
 */
public class ResultsMap<K, V> implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The backing map.
   */
  private final Map<K, InvocationResult<V>> map = new HashMap<>();

  /**
   * Add an entry to this map.
   * @param key the entry key.
   * @param value the entry's value.
   * @return the value previously mapped to the key, or {@code null} if there wasn't any. 
   */
  public InvocationResult<V> put(final K key, final V value) {
    return map.put(key, new  InvocationResult<>(value));
  }

  /**
   * Add an entry with an exceptional value to this map.
   * @param key the entry key.
   * @param exception the value as an {@link Exception}.
   * @return the value previously mapped to the key, or {@code null} if there wasn't any. 
   */
  public InvocationResult<V> put(final K key, final Exception exception) {
    return map.put(key, new  InvocationResult<>(exception));
  }

  /**
   * Add an entry to this map.
   * @param key the entry key.
   * @param value the entry's value.
   * @return the value previously mapped to the key, or {@code null} if there wasn't any. 
   */
  public InvocationResult<V> put(final K key, final InvocationResult<V> value) {
    return map.put(key, value);
  }

  /**
   * Get the value of the entry with the specified key.
   * @param key the key to look for.
   * @return an {@link InvocationResult} if one is mapped to the key, {@code null} otherwise.
   */
  public InvocationResult<V> get(final K key) {
    return map.get(key);
  }

  /**
   * Get the unwrapped value of the entry with the specified key.
   * @param key the key to look for.
   * @return the value for the key, if an entry is mapped to the key, or {@code null} if there is no entry for the key or if the entry has an exception result.
   */
  public V getResult(final K key) {
    final InvocationResult<V> ir = map.get(key);
    return (ir == null) ? null : ir.result();
  }

  /**
   * Get the unwrapped exception of the entry with the specified key.
   * @param key the key to look for.
   * @return the exception for the key, if an entry is mapped to the key, or {@code null} if there is no entry for the key or if the entry has a non-exception result.
   */
  public Exception getException(final K key) {
    final InvocationResult<V> ir = map.get(key);
    return (ir == null) ? null : ir.exception();
  }

  /**
   * Determines whether this map contains an entry for the specified key.
   * @param key the key to look for.
   * @return {@code true} if there is an entry for the key, {@code false} otherwise.
   */
  public boolean containsKey(final K key) {
    return map.containsKey(key);
  }

  /**
   * Get the entry set for all wrapped entries in this map.
   * @return a set of entries mapping the keys in this map to the corresponding {@link InvocationResult}s.
   */
  public Set<Map.Entry<K, InvocationResult<V>>> entrySet() {
    return map.entrySet();
  }

  /**
   * Get the entry set for all unwrapped non-exception results in this map.
   * @return a set of entries mapping the keys in this map to the corresponding unwrapped result.
   */
  public Set<Map.Entry<K, V>> resultsEntrySet() {
    return map.entrySet().stream()
      .filter(entry -> !entry.getValue().isException())
      .map(entry -> new MapEntry<>(entry.getKey(), entry.getValue().result()))
      .collect(Collectors.toSet());
  }

  /**
   * Get the entry set for all unwrapped exceptions in this map.
   * @return a set of entries mapping the keys in this map to the corresponding exception.
   */
  public Set<Map.Entry<K, Exception>> exceptionsEntrySet() {
    return map.entrySet().stream()
      .filter(entry -> entry.getValue().isException())
      .map(entry -> new MapEntry<>(entry.getKey(), entry.getValue().exception()))
      .collect(Collectors.toSet());
  }

  /**
   * Performs the given action for each entry in this map until all entries have been processed or the action throws an exception.
   * @param action the action to perform for each entry
   */
  public void forEach(final BiConsumer<K, InvocationResult<V>> action) {
    map.forEach(action);
  }

  /**
   * Get the set of all keys in this map.
   * @return the set of keys in this map.
   */
  public Set<K> keySet() {
    return map.keySet();
  }

  /**
   * Get the values of all entries in this map.
   * @return the set of values in the map.
   */
  public Collection<InvocationResult<V>> values() {
    return map.values();
  }

  /**
   * Get the unwrapped results of all entries in this map with a non-exception result.
   * @return the set of all results in the map.
   */
  public Set<V> results() {
    return map.entrySet().stream().filter(entry -> !entry.getValue().isException()).map(entry -> entry.getValue().result()).collect(Collectors.toSet());
  }

  /**
   * Get the unwrapped exceptions of all entries in this map with an exceptional result.
   * @return the set of all exceptions in the map.
   */
  public Set<Exception> exceptions() {
    return map.entrySet().stream().filter(entry -> entry.getValue().isException()).map(entry -> entry.getValue().exception()).collect(Collectors.toSet());
  }

  /**
   * @return the size of this map.
   */
  public int size() {
    return map.size();
  }

  /**
   * Determine whether this map is empty.
   * @return {@code ture} if this map is empty, {@code false} otherwise.
   */
  public boolean isEmpty() {
    return map.isEmpty();
  }

  /**
   * A basinc implementation of {@link Map.Entry} which does not allow to modify an entry. 
   * @param <K> the type of the key.
   * @param <V> the type of the value.
   */
  private static class MapEntry<K, V> implements Map.Entry<K, V> {
    /**
     * The key.
     */
    private final K key;
    /**
     * The value.
     */
    private V value;

    /**
     * Initialize this entry with the specified key and value.
     * @param key the key to use.
     * @param value the value to use.
     */
    public MapEntry(final K key, final V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    /**
     * This method does nothing and merely returns {@code null}.
     * @param value not used.
     * @return {@code null}.
     */
    @Override
    public V setValue(final V value) {
      return null;
    }
  }
}
