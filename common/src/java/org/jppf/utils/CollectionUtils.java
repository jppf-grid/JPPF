/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
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
}
