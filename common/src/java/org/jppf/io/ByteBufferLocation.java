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

package org.jppf.io;

import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * Data location backed by a byte buffer.
 * @author Laurent Cohen
 */
public class ByteBufferLocation extends AbstractDataLocation
{
	/**
	 * The data abstracted by this memory location.
	 */
	private ByteBuffer buffer = null;

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
		size = buffer.limit();
	}

	/**
	 * Transfer the content of this data location from the specified input source.
	 * @param source the input source to transfer from.
	 * @param blocking if true, the method will block until the entire content has been transferred. 
	 * @return the number of bytes actually transferred. 
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.DataLocation#transferFrom(org.jppf.io.InputSource, boolean)
	 */
	public int transferFrom(InputSource source, boolean blocking) throws Exception
	{
		return transferFrom_0(source, blocking, false);
	}

	/**
	 * Transfer the content of this data location from the specified channel.
	 * @param source the channel to transfer from.
	 * @param blocking if true, the method will block until the entire content has been transferred.
	 * @return the number of bytes actually transferred. 
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.DataLocation#transferFrom(java.nio.channels.ReadableByteChannel, boolean)
	 */
	public int transferFrom(ReadableByteChannel source, boolean blocking) throws Exception
	{
		return transferFrom_0(source, blocking, true);
	}

	/**
	 * Transfer the content of this data location from the specified source.
	 * @param source the source to transfer from.
	 * @param blocking if true, the method will block until the entire content has been transferred.
	 * @param isChannel if true, the source is a <code>ReadableByteChannel</code>, otherwise it is an <code>OutputDestination</code>. 
	 * @return the number of bytes actually transferred. 
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.DataLocation#transferFrom(java.nio.channels.ReadableByteChannel, boolean)
	 */
	private int transferFrom_0(Object source, boolean blocking, boolean isChannel) throws Exception
	{
		if (!transferring)
		{
			transferring = true;
			buffer.rewind();
		}
		if (!blocking)
		{
			int n = isChannel ? ((ReadableByteChannel) source).read(buffer) : ((InputSource) source).read(buffer);
			if ((n < 0) || !buffer.hasRemaining()) transferring = false;
			return n;
		}
		int count = 0;
		while (count < size)
		{
			int n = isChannel ? ((ReadableByteChannel) source).read(buffer) : ((InputSource) source).read(buffer);
			if (n < 0)
			{
				transferring = false;
				return -1;
			}
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
	 * @see org.jppf.io.DataLocation#transferTo(org.jppf.io.OutputDestination, boolean)
	 */
	public int transferTo(OutputDestination dest, boolean blocking) throws Exception
	{
		return transferTo_0(dest, blocking, false);
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
		return transferTo_0(dest, blocking, true);
	}


	/**
	 * Transfer the content of this data location to the specified destination.
	 * @param dest the channel to transfer to.
	 * @param blocking if true, the method will block until the entire content has been transferred. 
	 * @param isChannel if true, the destination is a <code>WritableByteChannel</code>, otherwise it is an <code>OutputDestination</code>. 
	 * @return the number of bytes actually transferred. 
	 * @throws Exception if an IO error occurs.
	 */
	private int transferTo_0(Object dest, boolean blocking, boolean isChannel) throws Exception
	{
		if (!transferring)
		{
			transferring = true;
			buffer.rewind();
		}
		if (!blocking)
		{
			int n = isChannel ? ((WritableByteChannel) dest).write(buffer) : ((OutputDestination) dest).write(buffer);
			if ((n < 0) || !buffer.hasRemaining()) transferring = false;
			return n;
		}
		int count = 0;
		while (count < size)
		{
			int n = isChannel ? ((WritableByteChannel) dest).write(buffer) : ((OutputDestination) dest).write(buffer);
			if (n < 0)
			{
				transferring = false;
				return -1;
			}
			else count += n;
		}
		transferring = false;
		return count;
	}
}
