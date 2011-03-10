/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.client;

import java.io.Serializable;
import java.util.Comparator;

/**
 * This comparator defines a decending value order for integers.
 */
class DescendingIntegerComparator implements Comparator<Integer>, Serializable
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Compare two integers. This comparator defines a descending order for integers.
	 * @param o1 first integer to compare.
	 * @param o2 second integer to compare.
	 * @return -1 if o1 > o2, 0 if o1 == o2, 1 if o1 < o2
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Integer o1, Integer o2)
	{
		return o2 - o1; 
	}
}