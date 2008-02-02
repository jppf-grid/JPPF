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
