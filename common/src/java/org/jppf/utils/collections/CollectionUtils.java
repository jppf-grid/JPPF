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


import java.lang.reflect.Array;
import java.util.*;

/**
 * This class provides a set of utility methods for manipulating and converting
 * collections and arrays.
 * @author Laurent Cohen
 */
public final class CollectionUtils
{
  /**
   * Instantiation of this class is not permitted.
   */
  private CollectionUtils()
  {
  }

  /**
   * Convert an array into a <code>Set</code>.
   * @param <T> the type of the elements in the array.
   * @param array the array to convert.
   * @return a set of elements with the same type as that of the array element type.
   */
  public static <T> Set<T> set(final T...array)
  {
    Set<T> newSet = new HashSet<>(array.length);
    for (T element: array) newSet.add(element);
    return newSet;
  }

  /**
   * Convert an array into a <code>List</code>.
   * @param <T> the type of the elements in the array.
   * @param array the array to convert.
   * @return a list of elements with the same type as that of the array element type.
   */
  public static <T> List<T> list(final T...array)
  {
    List<T> list = new ArrayList<>(array.length);
    for (T element: array) list.add(element);
    return list;
  }

  /**
   * Concatenate a set of array into a single array.
   * @param <T> the element type of the arrays to concatenate.
   * @param arrays the arrays to concatenate.
   * @return an array whose size is the sum of the sizes of all the input arrays, and whose elements are all the
   * elements found in all the input arrays.
   */
  @SuppressWarnings("unchecked")
  public static <T> T[] concatArrays(final T[]...arrays)
  {
    if (arrays == null) return null;
    int size = 0;
    for (T[] array: arrays) size += array.length;
    List<T> result = new ArrayList<>(size);
    T[] tmp = null;
    for (T[] array: arrays)
    {
      if (array.length > 0)
      {
        tmp = array;
        break;
      }
    }
    if (tmp == null) return Arrays.copyOf(arrays[0], 0);
    for (T[] array: arrays)
    {
      for (T t: array) result.add(t);
    }
    return result.toArray((T[]) Array.newInstance(tmp[0].getClass(), 0));
  }

  /**
   * Add an element in a map whose values are lists of elements.
   * @param <T> the type of the keys in the map.
   * @param <U> the type of the elements in the lists.
   * @param key the key for the value to add.
   * @param value the value to add.
   * @param map the map in which ot add the key/value pair.
   */
  public static <T, U> void putInListMap(final T key, final U value, final Map<T, List<U>> map)
  {
    List<U> list = map.get(key);
    if (list == null)
    {
      list = new ArrayList<>();
      map.put(key, list);
    }
    list.add(value);
  }

  /**
   * Remove an element from a map whose values are lists of elements.
   * @param <T> the type of the keys in the map.
   * @param <U> the type of the elements in the lists.
   * @param key the key for the value to remove.
   * @param value the value to remove.
   * @param map the map from which to remove the key/value pair.
   */
  public static <T, U> void removeFromListMap(final T key, final U value, final Map<T, List<U>> map)
  {
    List<U> list = map.get(key);
    if (list == null) return;
    list.remove(value);
    if (list.isEmpty()) map.remove(key);
  }

  /**
   * Get the total number of elements in a map whose values are lists of elements.
   * @param <T> the type of the keys in the map.
   * @param <U> the type of the elements in the lists.
   * @param map the map of which to get the size.
   * @return the size of the map as an int value.
   */
  public static <T, U> int sizeOfListMap(final Map<T, List<U>> map)
  {
    int result = 0;
    for (Map.Entry<T, List<U>> entry: map.entrySet()) result += entry.getValue().size();
    return result;
  }

  /**
   * Format a string with size information about a map whose values are lists of elements.
   * @param <T> the type of the keys in the map.
   * @param <U> the type of the values in the map.
   * @param name an arbitrary name given to the map.
   * @param map the map from which to get size information.
   * @return a string containing information about the number of elements in the map.
   */
  public static <T, U> String formatSizeMapInfo(final String name, final Map<T, List<U>> map)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(name).append("[shallow size=").append(map.size());
    sb.append(", total elements=").append(sizeOfListMap(map)).append(']');
    return sb.toString();
  }

  /**
   * Format a string with size information about a map whose values are lists of elements.
   * @param <T> the type of the keys in the map.
   * @param <U> the type of the values in the map.
   * @param name an arbitrary name given to the map.
   * @param map the map from which to get size information.
   * @return a string containing information about the number of elements in the map.
   */
  public static <T, U> String formatSizeMapInfo(final String name, final AbstractCollectionMap<T, U> map)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(name).append("[shallow size=").append(map.size());
    sb.append(", total elements=").append(map.size()).append(']');
    return sb.toString();
  }

  /**
   * Generate a list that contains the specified number of elements of the specified list,
   * starting at the specified position in the specified list.
   * @param <T> the type of the elements in the list.
   * @param source the list from which to get the elements.
   * @param start the start position in the source list.
   * @param size the number of elements to get from the source list.
   * @return the resulting list.
   */
  public static <T> List<T> getAllElements(final List<T> source, final int start, final int size)
  {
    List<T> result = new ArrayList<>(size);
    for (int i=0; i<size; i++) result.add(source.get(i+start));
    return result;
  }

  /**
   * Return the parameters as an array.
   * @param <T> the ytpe of the elements in the array. Inferred as the common supertype of all the elements.
   * @param elts the elements of the array.
   * @return an array of the specified type.
   */
  public static <T> T[] array(final T...elts)
  {
    return elts;
  }

  /**
   * Return the parameters as an array of Objects.
   * @param elts the elements of the array.
   * @return an array of <code>Object</code> instances.
   */
  public static Object[] objects(final Object...elts)
  {
    return elts;
  }

  /**
   * Print the content of a collection map in an easily readable way.
   * @param map the map to print.
   * @return a string with the map elements properly indented and formatted.
   * @param <K> the type of the keys in the map.
   * @param <V> the type of the values in the map.
   */
  public static <K, V> String prettyPrint(final CollectionMap<K, V> map) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    int count1 = 0;
    for (K key: map.keySet()) {
      if (count1 > 0) sb.append(",\n");
      sb.append("  ").append(key).append(" = [\n");
      int count2 = 0;
      for (V value: map.getValues(key)) {
        if (count2 > 0) sb.append(",\n");
        sb.append("    ").append(value);
        count2++;
      }
      sb.append("\n  ]");
      count1++;
    }
    sb.append("\n}");
    return sb.toString();
  }
}
