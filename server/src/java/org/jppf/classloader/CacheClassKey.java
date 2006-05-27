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
package org.jppf.classloader;

/**
 * This class represents the key used in the class cache.
 * @author Domingos Creado
 */
class CacheClassKey
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