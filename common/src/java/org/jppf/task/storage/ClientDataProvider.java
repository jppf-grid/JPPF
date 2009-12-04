/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import org.apache.commons.logging.*;
import org.jppf.node.JPPFClassLoader;
import org.jppf.utils.*;

/**
 * This data provider is an extension of <code>MemoryMapDataProvider</code> that enables executing a callback method on the client side.
 * @author Laurent Cohen
 */
public class ClientDataProvider extends MemoryMapDataProvider
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(ClientDataProvider.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

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
	 * Initialize this DataProvider.
	 */
	public ClientDataProvider()
	{
	}

	/**
	 * Get a value specified by its key and computed by a callable object on the client side.<br>
	 * If the value has already been computed on the client side, it is simply retrieved locally from this data provider.
	 * @param <V> the type of results returned by the callable.
	 * @param key - the key identifying the value to retrieve in the store.
	 * @param callable - a JPPFCallable object used to compute the value.
	 * @return the value as an <code>Object</code>.
	 * @see org.jppf.task.storage.DataProvider#getValue(java.lang.Object)
	 */
	public <V> Object computeValue(Object key, JPPFCallable<V> callable)
	{
		if (key == null) throw new NullPointerException("key cannot be null");
		Object result = null;
		if (callable != null) result = getValueFromClient(callable);
		setValue(key, (result == null) ? SpecialValue.NULL_VALUE : result);
		return SpecialValue.NULL_VALUE.equals(result) ? null : result;
	}

	/**
	 * Compute a value on the client-side, as the result of the execution of a {@link org.jppf.utils.JPPFCallable JPPFCallable}.
	 * @param <V> - the type of results returned by the callable.
	 * @param callable - the key from which to get the value.
	 * @return the looked-up value, or null if the value could not be found.
	 * @see org.jppf.utils.JPPFCallable
	 */
	private <V> Object getValueFromClient(JPPFCallable<V> callable)
	{
		ClassLoader cl = callable.getClass().getClassLoader();
		if (!(cl instanceof JPPFClassLoader)) return null;
		try
		{
			JPPFClassLoader loader = (JPPFClassLoader) cl;
			Class clazz = loader.loadClass("org.jppf.utils.ObjectSerializerImpl");
			ObjectSerializer ser = (ObjectSerializer) clazz.newInstance();
			byte[] bytes = ser.serialize(callable).getBuffer();
			bytes = ((JPPFClassLoader) cl).computeRemoteData(bytes);
			if (bytes == null) return null;
			return ser.deserialize(bytes);
		}
		catch(Exception ignored)
		{
			ignored.printStackTrace();
		}
		return null;
	}
}
