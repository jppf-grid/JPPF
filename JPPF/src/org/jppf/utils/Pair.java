/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.utils;

import java.io.Serializable;

/**
 * Utility class holding a pair of references to two objects.
 * @author Laurent Cohen
 */
public class Pair<U, V> implements Serializable
{
	private U first = null;
	private V second = null;
	
	/**
	 * INitialize this pair with two values.
	 * @param first the first value of the new pair.
	 * @param second the second value of the new pair.
	 */
	public Pair(U first, V second)
	{
		this.first = first;
		this.second = second;
	}

	/**
	 * Get the first value of this pair.
	 * @return an object of type U.
	 */
	public U first()
	{
		return first;
	}

	/**
	 * Get the second value of this pair.
	 * @return an object of type V.
	 */
	public V second()
	{
		return second;
	}
}
