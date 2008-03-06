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

import java.io.Serializable;
import java.util.Comparator;

/**
 * 
 * @param <T>
 * @author Laurent Cohen
 */
public class JPPFPriority<T> implements Comparable<JPPFPriority<T>>, Serializable
{
	/**
	 * The comparator used to compare priority objects.
	 */
	private Comparator<T> comparator = null;
	/**
	 * The object used as priority.
	 */
	private T value = null;

	/**
	 * Initialize this priority witht he specified object.
	 * @param value the object used as priority.
	 * @throws IllegalArgumentException if value is not an instance of <code>Comparable</code>.
	 */
	public JPPFPriority(T value) throws IllegalArgumentException
	{
		if (!(value instanceof Comparable))
			throw new IllegalArgumentException("this constructor only accepts Comparable objects");
		this.value = value;
	}

	/**
	 * Initialize this priority witht he specified object and comparator.
	 * @param value the object used as priority.
	 * @param comparator the comparator used to compare priority objects.
	 * @throws IllegalArgumentException if value is null or comparator is null.
	 */
	public JPPFPriority(T value, Comparator<T> comparator) throws IllegalArgumentException
	{
		if (value == null)
			throw new IllegalArgumentException("the value argument must not be null");
		if (comparator == null)
			throw new IllegalArgumentException("the comparator argument must not be null");
		this.value = value;
		this.comparator = comparator;
	}

	/**
	 * Compare this priority with another.
	 * @param o the priority to compare with.
	 * @return a positive value if this priority is greater, a negative value if it is less, otherwise 0.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(JPPFPriority<T> o)
	{
		if (comparator != null) return comparator.compare(value, o.getValue());
		return ((Comparable) value).compareTo(o.getValue());
	}

	/**
	 * Return the object use as priority.
	 * @return an instance of T.
	 */
	public T getValue()
	{
		return value;
	}
}
