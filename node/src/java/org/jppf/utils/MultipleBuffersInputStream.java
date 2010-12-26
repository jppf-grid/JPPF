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

import org.slf4j.*;

/**
 * An output stream implementation that minimizes memory usage.
 * @author Laurent Cohen
 */
public class MultipleBuffersInputStream extends InputStream
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(MultipleBuffersInputStream.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean traceEnabled = log.isTraceEnabled();
	/**
	 * Contains the data written to this ouptput stream, as a sequence of {@link JPPFBuffer} instances.
	 */
	private List<JPPFBuffer> list = new ArrayList<JPPFBuffer>();
	/**
	 * The JPPFBuffer currently being read from.
	 */
	private JPPFBuffer currentBuffer = null;
	/**
	 * Current index in the list of buffers.
	 */
	private int bufferIndex = -1;
	/**
	 * The total number of bytes written into this output stream.
	 */
	private int totalSize = 0;
	/**
	 * Determines whether end of file was reached.
	 */
	private boolean eofReached = false;

	/**
	 * Intialize this input stream with the specified buffers.
	 * @param buffers an array of {@link JPPFBuffer} instances.
	 */
	public MultipleBuffersInputStream(JPPFBuffer...buffers)
	{
		for (JPPFBuffer b: buffers)
		{
			list.add(new JPPFBuffer(b.buffer, b.length));
			totalSize += b.length;
		}
	}

	/**
	 * Intialize this input stream with the specified buffers.
	 * @param buffers an array of {@link JPPFBuffer} instances.
	 */
	public MultipleBuffersInputStream(List<JPPFBuffer> buffers)
	{
		for (JPPFBuffer b: buffers)
		{
			list.add(new JPPFBuffer(b.buffer, b.length));
			totalSize += b.length;
		}
	}

	/**
	 * Read a single byte from this input stream.
	 * @return the data to write.
	 * @throws IOException if any error occurs.
	 * @see java.io.OutputStream#write(int)
	 */
	public int read() throws IOException
	{
		if ((currentBuffer == null) || (currentBuffer.length - currentBuffer.pos < 1)) nextBuffer();
		if (eofReached) return -1;
		int n = currentBuffer.buffer[currentBuffer.pos];
		currentBuffer.pos++;
		if (traceEnabled) log.trace("read one byte '" + n + "' from " + this);
		return n;
	}

	/**
	 * Writes len bytes from the specified byte array starting at offset off to this output stream.
	 * @param b the data.
	 * @param off the start offset in the data.
	 * @param len the number of bytes to write.
	 * @return the number of bytes read from the stream, or -1 if end of file was reached.
	 * @throws IOException if any error occurs.
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) throws IOException
	{
		if (b == null) throw new NullPointerException("the destination buffer must not be null");
		if ((off < 0) || (off > b.length) || (len < 0) || (off + len > b.length))
			throw new ArrayIndexOutOfBoundsException("b.length=" + b.length + ", off=" + off + ", len=" + len);
		if (eofReached) return -1;
		int count = 0;
		while (count < len)
		{
			//if ((currentBuffer == null) || (currentBuffer.remainingFromPos() <= 0)) nextBuffer();
			if ((currentBuffer == null) || (currentBuffer.length - currentBuffer.pos <= 0)) nextBuffer();
			if (eofReached) break;
			//int n = Math.min(currentBuffer.remainingFromPos(), len - count);
			int n = Math.min(currentBuffer.length - currentBuffer.pos, len - count);
			System.arraycopy(currentBuffer.buffer, currentBuffer.pos, b, off + count, n);
			count += n;
			currentBuffer.pos += n;
		}
		if (traceEnabled) log.trace("read " + count + " bytes from " + this +
			", bytes = " + StringUtils.dumpBytes(currentBuffer.buffer, currentBuffer.pos - count, Math.min(100, count)));
		return count;
	}

	/**
	 * Writes the specified byte array to this output stream.
	 * @param b the data to write.
	 * @return the number of bytes read from the stream, or -1 if end of file was reached.
	 * @throws IOException if any error occurs.
	 * @see java.io.OutputStream#write(byte[])
	 */
	public int read(byte[] b) throws IOException
	{
		if (b == null) throw new NullPointerException("the destination buffer must not be null");
		return read(b, 0, b.length);
	}

	/**
	 * Get to the next buffer in the list and set it as the current buffer.
	 */
	private void nextBuffer()
	{
		bufferIndex++;
		if (bufferIndex >= list.size())
		{
			eofReached = true;
			currentBuffer = null;
			return;
		}
		currentBuffer = list.get(bufferIndex);
		currentBuffer.pos = 0;
	}

	/**
	 * Get the JPPFBuffer currently being read from.
	 * @return a {@link JPPFBuffer} instance.
	 */
	public JPPFBuffer getCurrentBuffer()
	{
		if (eofReached) return null;
		if ((currentBuffer == null) || (currentBuffer.remainingFromPos() <= 0)) nextBuffer();
		return currentBuffer;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName()).append("[");
		sb.append("totalSize=").append(totalSize);
		sb.append(", nbBuffers=").append(list.size());
		sb.append(", bufferIndex=").append(bufferIndex);
		if (currentBuffer == null) sb.append(", currentBuffer=null");
		else
		{
			sb.append(", currentBuffer.pos=").append(currentBuffer.pos);
			sb.append(", currentBuffer.length=").append(currentBuffer.length);
		}
		sb.append("]");
		return sb.toString();
	}
}
