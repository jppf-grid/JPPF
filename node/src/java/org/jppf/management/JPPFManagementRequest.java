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

package org.jppf.management;

import java.io.Serializable;
import java.util.*;

/**
 * Instances of this class encapsulate the required information for any magament or monitoring request.
 * @param <T> the type of the parameters keys.
 * @param <U> the type of the parameters values.
 * @author Laurent Cohen
 */
public class JPPFManagementRequest <T, U> implements Serializable
{
	/**
	 * The parameters of this request.
	 */
	private Map<T, U> parameters = null;

	/**
	 * Default instanciation of this class is not permitted.
	 */
	private JPPFManagementRequest()
	{
	}

	/**
	 * Initialize this management request witht he specified parameters.
	 * @param parameters the parameters as a map of <code>BundleParameter</code> keys to object values.
	 */
	public JPPFManagementRequest(Map<T, U> parameters)
	{
		this.parameters = parameters;
	}

	/**
	 * Get the names of all parameters in this request.
	 * @return the parameter names as an array of <code>BundleParameter</code> enum instances.
	 */
	public T[] getParametersKeys()
	{
		if (parameters == null) return null;
		return (T[]) parameters.keySet().toArray();
	}

	/**
	 * Get the value of a specified parameter.
	 * @param key the name of the parameter.
	 * @return the parameter value as an object.
	 */
	public U getParameter(T key)
	{
		if (parameters == null) return null;
		return parameters.get(key);
	}

	/**
	 * Get the parameters map.
	 * @return an unmodifiable view of the parameters map.
	 */
	public Map<T, U> getParametersMap()
	{
		if (parameters == null) return null;
		return Collections.unmodifiableMap(parameters);
	}

	/**
	 * Get a string representation of this request.
	 * @return a string that describes this request instance.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return super.toString() + " : " + parameters;
	}
}
