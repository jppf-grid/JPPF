/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.server.protocol;

import java.io.Serializable;
import java.util.*;

/**
 * Instances of this class hold metadata about a job, that can be used from a load-balancer,
 * to adapt the load balancing to the computational weight of the job and/or the contained tasks.
 * It may be used in other places in future versions.
 * @see org.jppf.server.scheduler.bundle.JobAwareness
 * @author Laurent Cohen
 */
public class JPPFJobMetadata implements Serializable
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The metadata map.
	 */
	private Map<Object, Object> metadata = new HashMap<Object, Object>();

	/**
	 * Set a parameter in the metadata.
	 * If a parameter with the same key already exists, its value is replaced witht he new one.
	 * @param key the parameter's key.
	 * @param value the parameter's value.
	 */
	public void setParameter(Object key, Object value)
	{
		metadata.put(key, value);
	}

	/**
	 * Retrieve a parameter in the metadata.
	 * @param key the parameter's key.
	 * @return the parameter's value or null if no parameter with the specified key exists.
	 */
	public Object getParameter(Object key)
	{
		return metadata.get(key);
	}

	/**
	 * Retrieve a parameter in the metadata.
	 * @param key the parameter's key.
	 * @param def a default value to return if no parameter with the specified key can be found.
	 * @return the parameter's value or null if no parameter with the specified key exists.
	 */
	public Object getParameter(Object key, Object def)
	{
		Object value = metadata.get(key);
		return value != null ? value : def;
	}

	/**
	 * Remove a parameter from the metadata.
	 * @param key the parameter's key.
	 * @return the removed parameter's value or null if no parameter with the specified key exists.
	 */
	public Object removeParameter(Object key)
	{
		return metadata.remove(key);
	}

	/**
	 * Get a copy of the metadata map.
	 * @return a map of the metadata contained in this object.
	 */
	public Map<Object, Object> getAll()
	{
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.putAll(metadata);
		return map;
	}
}
