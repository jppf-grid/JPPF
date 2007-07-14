/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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
package org.jppf.server.nio.classloader;

/**
 * This class represents the key used in the class cache.
 * @author Domingos Creado
 */
public class CacheClassKey
{
	/**
	 * The provider uuid.
	 */
	private String uuid;
	/**
	 * String describing the cached resource.
	 */
	private String res;

	/**
	 * Initialize this key with a specified provider uuid and resource string.
	 * @param uuid the provider uuid.
	 * @param res string describing the cached resource.
	 */
	public CacheClassKey(String uuid, String res)
	{
		this.uuid = uuid;
		this.res = res;
	}

	/**
	 * Determine whether this key is equal to another one.
	 * @param obj the other key to compre with.
	 * @return true if the 2 keys a re equal, false otherwise.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if (obj instanceof CacheClassKey)
		{
			CacheClassKey other = (CacheClassKey) obj;
			return this.uuid.equals(other.uuid) && this.res.equals(other.res);
		}
		return false;
	}

	/**
	 * Calculate the ahsh code of this key.
	 * @return the hashcode as an int value.
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return uuid.hashCode() + res.hashCode();
	}
}
