/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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

import java.net.URL;

/**
 * This data provider is composed of multiple data provider implementations
 * to which it delegates its operations. The determination of which underlying
 * data provider to use is performed based on the types of the arguments.
 * @see org.jppf.task.storage.DataProvider
 * @author Laurent Cohen
 */
public class CompositeDataProvider implements DataProvider
{
	/**
	 * Data provider for reading from or writing to a URL.
	 */
	URLDataProvider udp = new URLDataProvider();
	/**
	 * Data provider for reading from or writing to an in-memory map.
	 */
	MemoryMapDataProvider mmdp = new MemoryMapDataProvider();

	/**
	 * Get a value specified by its key.
	 * If the key is an instance of {@link java.net.URL URL}, this method call is delegated
	 * to a <code>URLDataProvider</code>, otherwise it is delegated to a <code>MemoryMapDataProvider</code>.
	 * @param key the key identifying the value to retrieve in the store.
	 * @return the value as an <code>Object</code>.
	 * @throws Exception if an error occured while retrieving the data.
	 * @see org.jppf.task.storage.DataProvider#getValue(java.lang.Object)
	 */
	public Object getValue(Object key) throws Exception
	{
		if (key instanceof URL) return udp.getValue(key);
		return mmdp.getValue(key);
	}

	/**
	 * Set a value specified by its key in the store.
	 * If the key is an instance of {@link java.net.URL URL}, this method call is delegated
	 * to a <code>URLDataProvider</code>, otherwise it is delegated to a <code>MemoryMapDataProvider</code>.
	 * @param key the key identifying the value to retrieve in the store.
	 * @param value the value to store, associated with the key.
	 * @throws Exception if an error occured setting the data.
	 * @see org.jppf.task.storage.DataProvider#setValue(java.lang.Object, java.lang.Object)
	 */
	public void setValue(Object key, Object value) throws Exception
	{
		if (key instanceof URL) udp.setValue(key, value);
		else mmdp.setValue(key, value);
	}
}
