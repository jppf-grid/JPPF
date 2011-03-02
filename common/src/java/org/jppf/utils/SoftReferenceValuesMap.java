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

package org.jppf.utils;

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
public class SoftReferenceValuesMap<K, V> extends AbstractMap<K, V>
{
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
	private ReferenceQueue<Pair<K,V>> refQueue;
	/**
	 * The underlying map that backs this soft map.
	 */
	private Map<K, SoftReference<Pair<K,V>>> map; 

	/**
	 * Default constructor.
	 */
	public SoftReferenceValuesMap()
	{
		refQueue = new ReferenceQueue<Pair<K,V>>();
		map = new HashMap<K, SoftReference<Pair<K,V>>>();
	}

	/**
	 * {@inheritDoc}
	 */
	public int size()
	{
		cleanup();
		return map.size();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isEmpty()
	{
		cleanup();
		return map.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	public V get(Object key)
	{
		cleanup();
		SoftReference<Pair<K,V>> ref = map.get(key);
		return ref == null ? null : ref.get().second();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public V put(K key, V value)
	{
		cleanup();
		SoftReference<Pair<K,V>> ref = map.put(key, new SoftReference(new Pair(key, value), refQueue));
		return ref == null ? null : ref.get().second();
	}

	/**
	 * {@inheritDoc}
	 */
	public V remove(Object key)
	{
		cleanup();
		SoftReference<Pair<K,V>> ref = map.remove(key);
		return ref == null ? null : ref.get().second();
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<java.util.Map.Entry<K, V>> entrySet()
	{
		return null;
	}

	/**
	 * Cleanup the reference queue, by removing entries whose value was garbage collected.
	 */
	private void cleanup()
	{
		Reference<? extends Pair<K,V>> ref = null;
		while ((ref = refQueue.poll()) != null)
		{
			K key = ref.get().first();
			if (traceEnabled) log.trace("removing entry for key=" + key);
			map.remove(key);
		}
	}
}
