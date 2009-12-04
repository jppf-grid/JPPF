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

package org.jppf.io;

import java.nio.channels.*;

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
	protected int size = 0;
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
	 * Transfer the content of this data location from the specified channel.
	 * @param source the channel to transfer to.
	 * @param blocking if true, the method will block until the entire content has been transferred.
	 * @return the number of bytes actually transferred. 
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.DataLocation#transferFrom(java.nio.channels.ReadableByteChannel, boolean)
	 */
	public int transferFrom(ReadableByteChannel source, boolean blocking) throws Exception
	{
		return transferFrom(new ChannelInputSource(source), blocking);
	}

	/**
	 * Transfer the content of this data location to the specified channel.
	 * @param dest the channel to transfer to.
	 * @param blocking if true, the method will block until the entire content has been transferred. 
	 * @return the number of bytes actually transferred. 
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.DataLocation#transferTo(java.nio.channels.WritableByteChannel, boolean)
	 */
	public int transferTo(WritableByteChannel dest, boolean blocking) throws Exception
	{
		return transferTo(new ChannelOutputDestination(dest), blocking);
	}
}
