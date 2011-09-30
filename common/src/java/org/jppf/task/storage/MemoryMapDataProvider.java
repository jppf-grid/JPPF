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
package org.jppf.task.storage;

import java.util.*;

/**
 * Implementation of a data provider that handles in-memory data backed by a <code>Map</code>.
 * @see org.jppf.task.storage.DataProvider
 * @author Laurent Cohen
 */
public class MemoryMapDataProvider implements DataProvider
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The actual store implementation for the shared data.
	 */
	private Map<Object, Object> store = new HashMap<Object, Object>();
	
	/**
	 * Get a value specified by its key.
	 * @param key the key identifying the value to retrieve in the store.
	 * @return the value as an <code>Object</code>.
	 * @see org.jppf.task.storage.DataProvider#getValue(java.lang.Object)
	 */
	@Override
    public Object getValue(Object key)
	{
		return store.get(key);
	}
	
	/**
	 * Set a value specified by its key in the store.
	 * @param key the key identifying the value to retrieve in the store.
	 * @param value the value to store, associated with the key.
	 * @see org.jppf.task.storage.DataProvider#setValue(java.lang.Object, java.lang.Object)
	 */
	@Override
    public void setValue(Object key, Object value)
	{
		store.put(key, value);
	}
}
