/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
	public static <T> Set<T> set(T...array)
	{
		Set<T> newSet = new HashSet<T>();
		for (T element: array) newSet.add(element);
		return newSet;
	}

	/**
	 * Convert an array into a <code>List</code>.
	 * @param <T> the type of the elements in the array.
	 * @param array the array to convert.
	 * @return a list of elements with the same type as that of the array element type.
	 */
	public static <T> List<T> list(T...array)
	{
		List<T> list = new ArrayList<T>();
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
	public static <T> T[] concatArrays(T[]...arrays)
	{
		if (arrays == null) return null;
		List<T> result = new ArrayList<T>();
		for (T[] array: arrays)
		{
			for (T t: array) result.add(t);
		}
		return result.toArray((T[]) Array.newInstance(arrays[0][0].getClass(), 0));
	}

	/**
	 * Add an element in a map whose values are lists of elements.
	 * @param <T> the type of the keys in the map.
	 * @param <U> the type of the elements in the lists.
	 * @param key the key for the value to add.
	 * @param value the value to add.
	 * @param map the map in which ot add the key/value pair.
	 */
	public static <T, U> void putInListMap(T key, U value, Map<T, List<U>> map)
	{
		List<U> list = map.get(key);
		if (list == null)
		{
			list = new ArrayList<U>();
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
	public static <T, U> void removeFromListMap(T key, U value, Map<T, List<U>> map)
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
	public static <T, U> int sizeOfListMap(Map<T, List<U>> map)
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
	public static <T, U> String formatSizeMapInfo(String name, Map<T, List<U>> map)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(name).append(": shallow size = ").append(map.size());
		sb.append(", total elements = ").append(sizeOfListMap(map));
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
	public static <T> List<T> getAllElements(List<T> source, int start, int size)
	{
		List<T> result = new ArrayList<T>();
		for (int i=0; i<size; i++) result.add(source.get(i+start));
		return result;
	}
}
