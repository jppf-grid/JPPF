/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Output destination backed by a {@link java.nio.ByteBuffer ByteBuffer}.
 * @author Laurent Cohen
 */
public class ByteBufferOutputDestination implements OutputDestination
{
	/**
	 * The buffer that backs this output destination.
	 */
	private ByteBuffer data = null;

	/**
	 * Initialize this input source with the specified data.
	 * @param size trhe size of the data to write.
	 */
	public ByteBufferOutputDestination(int size)
	{
		this(ByteBuffer.allocate(size));
	}

	/**
	 * Initialize this output stream with the specified backing byte array.
	 * @param buffer the buffer into which to write.
	 * @param offset the start position in the buffer.
	 * @param len the length of data to write to the buffer.
	 */
	public ByteBufferOutputDestination(byte[] buffer, int offset, int len)
	{
		this(ByteBuffer.wrap(buffer, offset, len));
	}

	/**
	 * Initialize this output stream with the specified backing ByteBuffer.
	 * @param buffer the backing byte buffer.
	 */
	public ByteBufferOutputDestination(ByteBuffer buffer)
	{
		this.data = buffer;
	}

	/**
	 * Write data to this output destination from an array of bytes.
	 * @param buffer the buffer containing the data to write.
	 * @param offset the position in the buffer where to start reading the data.
	 * @param len the size in bvytes of the data to write.
	 * @return the number of bytes actually written, or -1 if end of stream was reached.
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.OutputDestination#write(byte[], int, int)
	 */
	public int write(byte[] buffer, int offset, int len) throws Exception
	{
		int pos = data.position();
		data.put(buffer, offset, len);
		return data.position() - pos;
	}

	/**
	 * Write data to this output destination from a byte buffer.
	 * @param buffer the buffer containing the data to write.
	 * @return the number of bytes actually written, or -1 if end of stream was reached.
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.OutputDestination#write(java.nio.ByteBuffer)
	 */
	public int write(ByteBuffer buffer) throws Exception
	{
		int pos = data.position();

		if (buffer.remaining() > data.remaining())
		{
			int limit = buffer.limit();
			buffer.limit(buffer.position() + data.remaining());
			data.put(buffer);
			buffer.limit(limit);
		}
		else data.put(buffer);
		return data.position() - pos;
	}

	/**
	 * Write an int value to this output destination.
	 * @param value the value to write. 
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.OutputDestination#writeInt(int)
	 */
	public void writeInt(int value) throws Exception
	{
		data.putInt(value);
	}

	/**
	 * Close this output destination and release any system resources associated with it.
	 * @throws IOException if an IO error occurs.
	 * @see java.io.Closeable#close()
	 */
	public void close() throws IOException
	{
	}
}
