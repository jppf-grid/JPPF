/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
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
	 * elemetns found in all the input arrays.
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
}
