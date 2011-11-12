/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.utils.pooling;

/**
 * 
 * @param <E>
 * @author Laurent Cohen
 */
public interface ObjectPool<E>
{
	/**
	 * Get an object from the pool, or create it if the pool is empty.
	 * @return a new Object of the type handled by this pool.
	 */
	E get();

	/**
	 * Release an object into this pool, and make it available.
	 * @param content the object to release.
	 */
	void put(E content);

	/**
	 * Determine whether this pool is empty.
	 * @return <code>true</code> if the pool is empty, <code>false</code> otherwise.
	 */
	boolean isEmpty();
	/**
	 * Get the number of objects in this pool.
	 * This is an optional operation, as it may not always be done in constant time.
	 * @return the size of this pool, or -1 if this operation is not supported.
	 */
	int size();
}
