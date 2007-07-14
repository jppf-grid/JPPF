/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

/**
 * Utility class holding a pair of references to two objects.
 * @param <U> the type of the first element in the pair.
 * @param <V> the type of the second element in the pair.
 * @author Laurent Cohen
 */
public class Pair<U, V> implements Serializable
{
	/**
	 * The first object of this pair.
	 */
	private U first = null;
	/**
	 * The second object of this pair.
	 */
	private V second = null;
	
	/**
	 * Initialize this pair with two values.
	 * @param first the first value of the new pair.
	 * @param second the second value of the new pair.
	 */
	public Pair(U first, V second)
	{
		this.first = first;
		this.second = second;
	}

	/**
	 * Get the first value of this pair.
	 * @return an object of type U.
	 */
	public U first()
	{
		return first;
	}

	/**
	 * Get the second value of this pair.
	 * @return an object of type V.
	 */
	public V second()
	{
		return second;
	}
}
