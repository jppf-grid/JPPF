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

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jppf.comm.socket.SocketWrapper;

/**
 * Input source backed by a {@link org.jppf.comm.socket.SocketWrapper SocketWrapper}.
 * @author Laurent Cohen
 */
public class SocketWrapperInputSource implements InputSource
{
	/**
	 * The backing <code>SocketWrapper</code>.
	 */
	private SocketWrapper socketWrapper = null;

	/**
	 * Initialize this input source with the specified <code>SocketWrapper</code>.
	 * @param socketWrapper the backing <code>SocketWrapper</code>.
	 */
	public SocketWrapperInputSource(SocketWrapper socketWrapper)
	{
		this.socketWrapper = socketWrapper;
	}

	/**
	 * Read data from this input source into an array of bytes.
	 * @param data the buffer into which to write.
	 * @param offset the position in the buffer where to start storing the data.
	 * @param len the size in bytes of the data to read.
	 * @return the number of bytes actually read, or -1 if end of stream was reached.
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.InputSource#read(byte[], int, int)
	 */
	public int read(byte[] data, int offset, int len) throws Exception
	{
		return socketWrapper.read(data, offset, len);
	}

	/**
	 * Read data from this input source into a byte buffer.
	 * @param data the buffer into which to write.
	 * @return the number of bytes actually read, or -1 if end of stream was reached.
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.InputSource#read(java.nio.ByteBuffer)
	 */
	public int read(ByteBuffer data) throws Exception
	{
		ByteBuffer tmp = ByteBuffer.wrap(new byte[IOHelper.TEMP_BUFFER_SIZE]);
		byte[] buf = tmp.array();
		int size = Math.min(buf.length, data.remaining());
		int n = socketWrapper.read(buf, 0, size);
		if (n > 0) data.put(buf, 0, n);
		return n;
	}

	/**
	 * Read an int value from this input source.
	 * @return the value read, or -1 if an end of file condition was reached. 
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.InputSource#readInt()
	 */
	public int readInt() throws Exception
	{
		return socketWrapper.readInt();
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
		return socketWrapper.skip(n);
	}

	/**
	 * This method does nothing.
	 * @throws IOException if an IO error occurs.
	 * @see java.io.Closeable#close()
	 */
	public void close() throws IOException
	{
	}
}
