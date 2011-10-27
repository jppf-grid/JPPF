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

import java.io.*;
import java.nio.ByteBuffer;

/**
 * Implementation of an <code>OutputStream</code> backed by a <code>ByteBuffer</code>.
 * @author Laurent Cohen
 */
public class ByteBufferOutputStream extends OutputStream
{
	/**
	 * The underlying byte buffer for this input stream.
	 */
	private ByteBuffer buffer = null;

	/**
	 * Initialize this output stream with the specified capacity for the backing ByteBuffer.
	 * @param capacity the capacity of the backing byte buffer.
	 */
	public ByteBufferOutputStream(final int capacity)
	{
		buffer = ByteBuffer.allocate(capacity);
	}

	/**
	 * Initialize this output stream with the specified backing ByteBuffer.
	 * @param buffer the backing byte buffer.
	 */
	public ByteBufferOutputStream(final ByteBuffer buffer)
	{
		this.buffer = buffer;
	}

	/**
	 * Close this output stream and releases any system resources associated with it.
	 * @throws IOException if an I/O error occurs.
	 * @see java.io.OutputStream#close()
	 */
	@Override
	public void close() throws IOException
	{
		super.close();
		buffer = null;
	}

	/**
	 * Flushes this output stream and force any buffered output bytes to be written out.
	 * @throws IOException if an I/O error occurs.
	 * @see java.io.OutputStream#flush()
	 */
	@Override
	public void flush() throws IOException
	{
		super.flush();
	}

	/**
	 * Write <code>len</code> bytes from the specified byte array starting at offset <code>off</code> to this output stream.
	 * @param b the data.
	 * @param off the start offset in the data.
	 * @param len the number of bytes to write.
	 * @exception IOException if an I/O error occurs.
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException
	{
		if (b == null) throw new NullPointerException();
		else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0))
		{
			throw new IndexOutOfBoundsException();
		}
		else if (len == 0) return;
		buffer.put(b, off, len);
	}

	/**
	 * Writes <code>b.length</code> bytes from the specified byte array to this output stream.
	 * @param b the data.
	 * @throws IOException if an I/O error occurs.
	 * @see java.io.OutputStream#write(byte[])
	 */
	@Override
	public void write(final byte[] b) throws IOException
	{
		if (b == null) throw new NullPointerException();
		write(b, 0, b.length);
	}

	/**
	 * Writes the specified byte to this output stream.
	 * @param b the <code>byte</code>.
	 * @throws IOException if an I/O error occurs.
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(final int b) throws IOException
	{
		buffer.put((byte) b);
	}

	/**
	 * Get the content of this output stream as a <code>ByteBuffer</code>.
	 * @return the content as a byte buffer.
	 */
	public ByteBuffer toByteBuffer()
	{
		return buffer;
	}
}
