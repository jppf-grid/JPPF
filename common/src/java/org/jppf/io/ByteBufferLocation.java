/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

import java.io.*;
import java.nio.ByteBuffer;

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
		this(new byte[capacity], 0, capacity);
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
	private ByteBufferLocation(ByteBuffer buffer)
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
		while (count < size)
		{
			int n = source.read(buffer);
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
		while (count < size)
		{
			int n = dest.write(buffer);
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
	 * Get the byte buffer that backs this location.
	 * @return the backing byte buffer. 
	 */
	public ByteBuffer buffer()
	{
		return buffer;
	}

	/**
	 * Get an input stream for this location.
	 * @return an <code>InputStream</code> instance.
	 * @throws Exception if an I/O error occurs.
	 * @see org.jppf.io.DataLocation#getInputStream()
	 */
	public InputStream getInputStream() throws Exception
	{
		buffer.rewind();
		return new ByteBufferInputStream(buffer, false);
	}

	/**
	 * Get an output stream for this location.
	 * @return an <code>OutputStream</code> instance.
	 * @throws Exception if an I/O error occurs.
	 * @see org.jppf.io.DataLocation#getOutputStream()
	 */
	public OutputStream getOutputStream() throws Exception
	{
		return new ByteBufferOutputStream(buffer);
	}

	/**
	 * Make a shallow copy of this data location.
	 * The data it points to is not copied.
	 * @return a new DataLocation instance pointing to the same data.
	 * @see org.jppf.io.DataLocation#copy()
	 */
	public DataLocation copy()
	{
		byte[] array = buffer.array();
		return new ByteBufferLocation(array, 0, array.length);
	}
}
