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

package org.jppf.utils;

import java.io.*;
import java.util.*;

/**
 * An output stream implementation that minimizes memory usage.
 * @author Laurent Cohen
 */
public class MultipleBuffersOutputStream extends OutputStream
{
	/**
	 * Default length of each new buffer.
	 */
	private int defaultLength = 32768;
	/**
	 * Contains the data written to this ouptput stream, as a sequence of {@link JPPFBuffer} instances.
	 */
	private List<JPPFBuffer> list = new ArrayList<JPPFBuffer>();
	/**
	 * The JPPFBuffer currently being written to.
	 */
	private JPPFBuffer currentBuffer = null;
	/**
	 * The total number of bytes written into this output stream.
	 */
	private int totalSize = 0;

	/**
	 * Intialize this output stream with a default buffer length of 32768.
	 */
	public MultipleBuffersOutputStream()
	{
	}

	/**
	 * Intialize this output stream with the specified default buffer length.
	 * @param defaultLength the default buffer length to use, must be strictly greater than 0.
	 * @throws IllegalArgumentException if the specified default length is less than 1.
	 */
	public MultipleBuffersOutputStream(int defaultLength) throws IllegalArgumentException
	{
		if (defaultLength <= 0) throw new IllegalArgumentException("the default buffer length must be > 0");
		this.defaultLength = defaultLength;
	}

	/**
	 * Write a single byte into this output stream.
	 * @param b the data to write.
	 * @throws IOException if any error occurs.
	 * @see java.io.OutputStream#write(int)
	 */
	public void write(int b) throws IOException
	{
		if ((currentBuffer == null) || (currentBuffer.remaining() < 1)) newCurrentBuffer(defaultLength);
		currentBuffer.buffer[currentBuffer.length] = (byte) b;
		currentBuffer.length++;
		totalSize++;
	}

	/**
	 * Writes len bytes from the specified byte array starting at offset off to this output stream.
	 * @param b the data.
	 * @param off the start offset in the data.
	 * @param len the number of bytes to write.
	 * @throws IOException if any error occurs.
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public void write(byte[] b, int off, int len) throws IOException
	{
		if (b == null) throw new NullPointerException("the input buffer must not be null");
		if ((off < 0) || (off > b.length) || (len < 0) || (off + len > b.length))
			throw new ArrayIndexOutOfBoundsException("b.length=" + b.length + ", off=" + off + ", len=" + len);
		if ((currentBuffer == null) || (currentBuffer.remaining() < len)) newCurrentBuffer(Math.max(defaultLength, len));
		System.arraycopy(b, off, currentBuffer.buffer, currentBuffer.length, len);
		currentBuffer.length += len;
		totalSize += len;
	}

	/**
	 * Writes the specified byte array to this output stream.
	 * @param b the data to write.
	 * @throws IOException if any error occurs.
	 * @see java.io.OutputStream#write(byte[])
	 */
	public void write(byte[] b) throws IOException
	{
		if (b == null) throw new NullPointerException("the input buffer must not be null");
		write(b, 0, b.length);
	}

	/**
	 * Create a new current buffer with the specified size and a length of 0, and add it to the list of buffers.
	 * @param size the size of the new buffer.
	 */
	private void newCurrentBuffer(int size)
	{
		currentBuffer = new JPPFBuffer(new byte[size], 0);
		list.add(currentBuffer);
	}

	/**
	 * Get the size of the content of this output stream.
	 * @return the size as an int value.
	 */
	public int size()
	{
		return totalSize;
	}

	/**
	 * Get the content of the output stream as a list of {@link JPPFBuffer} instances.
	 * @return a list of {@link JPPFBuffer} instances.
	 */
	public List<JPPFBuffer> toBufferList()
	{
		return list;
	}

	/**
	 * Get te content of the output stream as a single array of byte
	 * @return an array of bytes.
	 */
	public byte[] toByteArray()
	{
		if ((totalSize == 1) && (list.get(0).length == totalSize)) return list.get(0).buffer;
		int pos = 0;
		byte[] tmp = new byte[totalSize];
		for (JPPFBuffer buf: list)
		{
			System.arraycopy(buf.buffer, 0, tmp, pos, buf.length);
			pos += buf.length;
		}
		return tmp;
	}
}
