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

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.utils.streams.StreamConstants;

/**
 * Output destination backed by a {@link org.jppf.comm.socket.SocketWrapper SocketWrapper}.
 * @author Laurent Cohen
 */
public class SocketWrapperOutputDestination implements OutputDestination
{
	/**
	 * The backing <code>SocketWrapper</code>.
	 */
	private SocketWrapper socketWrapper = null;

	/**
	 * Initialize this output destination with the specified <code>SocketWrapper</code>.
	 * @param socketWrapper the backing <code>SocketWrapper</code>.
	 */
	public SocketWrapperOutputDestination(final SocketWrapper socketWrapper)
	{
		this.socketWrapper = socketWrapper;
	}

	/**
	 * Write data to this output destination from an array of bytes.
	 * @param data the buffer containing the data to write.
	 * @param offset the position in the buffer where to start reading the data.
	 * @param len the size in bytes of the data to write.
	 * @return the number of bytes actually written, or -1 if end of stream was reached.
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.OutputDestination#write(byte[], int, int)
	 */
	@Override
	public int write(final byte[] data, final int offset, final int len) throws Exception
	{
		socketWrapper.write(data, offset, len);
		//socketWrapper.flush();
		return len;
	}

	/**
	 * Write data to this output destination from a byte buffer.
	 * @param data the buffer containing the data to write.
	 * @return the number of bytes actually written, or -1 if end of stream was reached.
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.OutputDestination#write(java.nio.ByteBuffer)
	 */
	@Override
	public int write(final ByteBuffer data) throws Exception
	{
		ByteBuffer tmp = ByteBuffer.wrap(new byte[StreamConstants.TEMP_BUFFER_SIZE]);
		byte[] buf = tmp.array();
		int size = Math.min(buf.length, data.remaining());
		data.get(buf, 0, size);
		socketWrapper.write(buf, 0, size);
		//socketWrapper.flush();
		return size;
	}

	/**
	 * Write an int value to this output destination.
	 * @param value the value to write.
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.io.OutputDestination#writeInt(int)
	 */
	@Override
	public void writeInt(final int value) throws Exception
	{
		socketWrapper.writeInt(value);
	}

	/**
	 * This method does nothing.
	 * @throws IOException if an IO error occurs.
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException
	{
	}
}
