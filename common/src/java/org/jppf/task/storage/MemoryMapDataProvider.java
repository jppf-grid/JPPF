/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
	 * The actual store implementation for the shared data.
	 */
	private Map<Object, Object> store = new HashMap<Object, Object>();
	
	/**
	 * Get a value specified by its key.
	 * @param key the key identifying the value to retrieve in the store.
	 * @return the value as an <code>Object</code>.
	 * @see org.jppf.task.storage.DataProvider#getValue(java.lang.Object)
	 */
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
	public void setValue(Object key, Object value)
	{
		store.put(key, value);
	}
}
