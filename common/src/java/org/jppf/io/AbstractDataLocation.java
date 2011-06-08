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

package org.jppf.io;


/**
 * Abstract implementation of the <code>DataLocation</code> interface.>br>
 * This class provides default implementations for the <code>transferFrom(ReadableByteChannel, boolean)</code> and
 * <code>transferTo(WritableByteChannel, boolean)</code> methods.
 * @author Laurent Cohen
 */
public abstract class AbstractDataLocation implements DataLocation
{
	/**
	 * The capacity of the underlying buffer.
	 */
	protected int size = UNKNOWN_SIZE;

	/**
	 * Determines whether a transfer has been started.
	 */
	protected boolean transferring = false;

	/**
	 * Get the size of the data referenced by this data location.
	 * @return the data size as an int.
	 * @see org.jppf.io.DataLocation#getSize()
	 */
	public int getSize()
	{
		return size;
	}

	/**
	 * Set the size of the data referenced by this data location.
	 * @param size - the data size as an int.
	 */
	public void setSize(int size)
	{
		this.size = size;
	}
}
