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
 * Input source backed by an array of bytes.
 * @author Laurent Cohen
 */
public class ByteBufferInputSource implements InputSource
{
	/**
	 * The buffer that backs this input source.
	 */
	private ByteBuffer data = null;

	/**
	 * Initialize this input source with the specified data.
	 * @param data the buffer from which to read.
	 * @param offset the start position in the buffer.
	 * @param len the length of data to read from the buffer.
	 */
	public ByteBufferInputSource(byte[] data, int offset, int len)
	{
		this(ByteBuffer.wrap(data, offset, len));
	}

	/**
	 * Initialize this input source with the specified byte buffer.
	 * @param data the buffer from which to read.
	 */
	public ByteBufferInputSource(ByteBuffer data)
	{
		this.data = data;
	}

	/**
	 * Read data from this input source and write it into an array of bytes.
	 * @param buffer the buffer into which to write.
	 * @param offset the position in the buffer where to start storing the data.
	 * @param len the size in bvytes of the data to read. 
	 * @return the number of bytes actually read, or -1 if end of stream was reached. 
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.InputSource#read(byte[], int, int)
	 */
	public int read(byte[] buffer, int offset, int len) throws Exception
	{
		int pos = data.position();
		data.get(buffer, offset, len);
		return data.position() - pos;
	}

	/**
	 * Read data from this input source into a byte buffer.
	 * @param buffer the buffer into which to write.
	 * @return the number of bytes actually read, or -1 if end of stream was reached.
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.InputSource#read(java.nio.ByteBuffer)
	 */
	public int read(ByteBuffer buffer) throws Exception
	{
		int pos = buffer.position();
		if (data.remaining() > buffer.remaining())
		{
			int limit = data.limit();
			data.limit(data.position() + buffer.remaining());
			buffer.put(data);
			data.limit(limit);
		}
		else buffer.put(data);
		return buffer.position() - pos;
	}

	/**
	 * Read an int value from this input source.
	 * @return the value read, or -1 if an end of file condition was reached. 
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.InputSource#readInt()
	 */
	public int readInt() throws Exception
	{
		return data.getInt();
	}

	/**
	 * Skip <code>n</code> bytes of data form this input source.
	 * @param n the number of bytes to skip.
	 * @return the number of bytes actually skipped.
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.InputSource#skip(int)
	 */
	public int skip(int n) throws Exception
	{
		if (!data.hasRemaining()) return -1;
		int count = Math.min(n, data.remaining());
		data.position(count);
		return count;
	}

	/**
	 * Close this input source and release any system resources associated with it.
	 * @throws IOException if an IO error occurs.
	 * @see java.io.Closeable#close()
	 */
	public void close() throws IOException
	{
	}
}
