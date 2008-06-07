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

package org.jppf.server.nio.message;

import java.nio.ByteBuffer;

/**
 * Data location backed by a byte buffer.
 * @author Laurent Cohen
 */
public class ByteBufferLocation implements DataLocation
{
	/**
	 * The capacity of the underlying buffer.
	 */
	private int capacity = 0;
	/**
	 * The data abstracted by this memory location.
	 */
	private ByteBuffer buffer = null;
	/**
	 * Determines whether a transfer has been started.
	 */
	private boolean transferring = false;

	/**
	 * Initialize this data location with an empty buffer of the specified capacity.
	 * @param capacity the buffer's capacity.
	 */
	public ByteBufferLocation(int capacity)
	{
		this(ByteBuffer.allocateDirect(capacity));
	}

	/**
	 * Initialize this location with the specified buffer.
	 * @param data the buffer containing the data.
	 * @param offset the start position in the buffer.
	 * @param len the size in bytes of the data to write.
	 */
	public ByteBufferLocation(byte[] data, int offset, int len)
	{
		this(ByteBuffer.wrap(data, offset, len));
	}

	/**
	 * Initialize this location with the specified buffer.
	 * @param buffer the data abstracted by this memory location.
	 */
	public ByteBufferLocation(ByteBuffer buffer)
	{
		this.buffer = buffer;
		capacity = buffer.limit();
	}

	/**
	 * Get an input source from this data location.
	 * @return an <code>InputSource</code> instance.
	 * @see org.jppf.server.nio.message.DataLocation#getInputSource()
	 */
	public InputSource getInputSource()
	{
		return new ByteInputSource(buffer);
	}

	/**
	 * Get an output destination from this data location.
	 * @return an <code>OutputDestination</code> instance.
	 * @see org.jppf.server.nio.message.DataLocation#getOutputDestination()
	 */
	public OutputDestination getOutputDestination()
	{
		buffer.rewind();
		return new ByteOutputDestination(buffer);
	}

	/**
	 * Get the size of the data referenced by this data location.
	 * @return the data size as an int.
	 * @see org.jppf.server.nio.message.DataLocation#getSize()
	 */
	public int getSize()
	{
		return capacity;
	}

	/**
	 * Transfer the content of this data location from the specified input source.
	 * @param source the input source to transfer to.
	 * @param blocking if true, the method will block until the entire content has been transferred. 
	 * @return the number of bytes actually transferred. 
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.server.nio.message.DataLocation#transferFrom(org.jppf.server.nio.message.InputSource, boolean)
	 */
	public int transferFrom(InputSource source, boolean blocking) throws Exception
	{
		if (!transferring)
		{
			transferring = true;
			buffer.rewind();
		}
		if (!blocking)
		{
			int n = source.read(buffer);
			if ((n < 0) || !buffer.hasRemaining()) transferring = false;
			return n;
		}
		int count = 0;
		while (count < capacity)
		{
			int n = source.read(buffer);
			if (n < 0) break;
			else count += n;
		}
		transferring = false;
		return count;
	}

	/**
	 * Transfer the content of this data location to the specified output destination.
	 * @param dest the output destination to transfer to.
	 * @param blocking if true, the method will block until the entire content has been transferred. 
	 * @return the number of bytes actually transferred. 
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.server.nio.message.DataLocation#transferTo(org.jppf.server.nio.message.OutputDestination, boolean)
	 */
	public int transferTo(OutputDestination dest, boolean blocking) throws Exception
	{
		if (!transferring)
		{
			transferring = true;
			buffer.rewind();
		}
		if (!blocking)
		{
			int n = dest.write(buffer);
			if ((n < 0) || !buffer.hasRemaining()) transferring = false;
			return n;
		}
		int count = 0;
		while (count < capacity)
		{
			int n = dest.write(buffer);
			if (n < 0)
			{
				transferring = false;
				break;
			}
			else count += n;
		}
		return count;
	}
}
