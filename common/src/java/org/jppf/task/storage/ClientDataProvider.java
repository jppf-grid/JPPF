/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import java.io.InputStream;
import java.util.*;

import org.jppf.node.JPPFClassLoader;
import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public class ClientDataProvider implements DataProvider
{
	/**
	 * This enum provides a way of specifying special values for data providers,
	 * that can be compared no matter where they are located.
	 */
	private enum SpecialValue
	{
		/**
		 * Specififies a null value.
		 */
		NULL_VALUE
	}

	/**
	 * Map of client-side data providers to their uuid.
	 */
	private static Map<String, ClientDataProvider> dpMap = Collections.synchronizedMap(new HashMap<String, ClientDataProvider>());
	/**
	 * Determines whether this data provider is invoked locally on the client.
	 */
	private static Boolean localMarker = Boolean.TRUE;
	/**
	 * This data provider's unique identifier.
	 */
	private String uuid = new JPPFUuid().toString();
	/**
	 * The actual store implementation for the shared data.
	 */
	private transient Map<Object, Object> store = new HashMap<Object, Object>();

	/**
	 * Initialize this DataProvider.
	 */
	public ClientDataProvider()
	{
		if (dpMap != null) dpMap.put(uuid, this);
	}

	/**
	 * Get a value specified by its key.
	 * @param key the key identifying the value to retrieve in the store.
	 * @return the value as an <code>Object</code>.
	 * @see org.jppf.task.storage.DataProvider#getValue(java.lang.Object)
	 */
	public Object getValue(Object key)
	{
		if (key == null) throw new NullPointerException("key cannot be null");
		if (store == null) store = new HashMap<Object, Object>();
		Object result = store.get(key);
		if (result == null)
		{
			result = getValueFromClient(key.toString());
			store.put(key, (result == null) ? SpecialValue.NULL_VALUE : result);
		}
		return SpecialValue.NULL_VALUE.equals(result) ? null : result;
	}

	/**
	 * Lookup the value on the client-side.
	 * @param key the key from which to get the value.
	 * @return the looked-up value, or null if the value could not be found.
	 */
	private Object getValueFromClient(String key)
	{
		ClassLoader cl = getClass().getClassLoader();
		if (!(cl instanceof JPPFClassLoader)) return null;
		String resName = "[cdp:" + uuid + "]" + key;
		InputStream is = cl.getResourceAsStream(resName);
		ObjectSerializer ser = new ObjectSerializerImpl();
		try
		{
			byte[] bytes = FileUtils.getInputStreamAsByte(is);
			return ser.deserialize(bytes);
		}
		catch(Exception ignored)
		{
		}
		return null;
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

	/**
	 * Get a ClientDataProvider given its uuid. This method is used on the clent side only.
	 * @param uuid the uuid of the data provider to find.
	 * @return a <code>ClientDataProvider</code> instance, or null if no client data provider with this uuid exists.
	 */
	public static ClientDataProvider getClientDataProvider(String uuid)
	{
		if (localMarker == null) return null;
		return dpMap.get(uuid);
	}
}
