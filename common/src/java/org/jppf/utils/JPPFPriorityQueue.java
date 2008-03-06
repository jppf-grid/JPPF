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

import java.util.*;

/**
 * A priority queue that allows the traversal of its elements in the order specified in the constructor. 
 * @param <S>
 * @author Laurent Cohen
 */
public class JPPFPriorityQueue<S>
{
	/**
	 * 
	 */
	private Map<JPPFPriority<?>, LinkedList<S>> map = new TreeMap<JPPFPriority<?>, LinkedList<S>>();
	
	/**
	 * Add an element with the specified priority.
	 * @param element the element to add.
	 * @param priority the priority of the element to add.
	 */
	public void add(S element, JPPFPriority<?> priority)
	{
		LinkedList<S> list = map.get(priority);
		if (list == null)
		{
			list = new LinkedList<S>();
			map.put(priority, list);
		}
		list.add(element);
	}
}
