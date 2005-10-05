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
package org.jppf.task.storage;

import java.io.Serializable;

/**
 * Instances of this class provide a way for tasks to share common data.
 * The objective is to avoid data duplication through marshaling/unmarshaling of the data,
 * which can cause crashes due to insufficient available memory in a node.
 * @author Laurent Cohen
 */
public interface DataProvider extends Serializable
{
	/**
	 * Get a value specified by its key.
	 * @param key the key identifying the value to retrieve in the store.
	 * @return the value as an <code>Object</code>.
	 */
	Object getValue(Object key);
	
	/**
	 * Set a value specified by its key in the store.
	 * @param key the key identifying the value to retrieve in the store.
	 * @param value the value to store, associated with the key.
	 */
	void getValue(Object key, Object value);
}
