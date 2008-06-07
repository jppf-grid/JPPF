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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.jppf.utils.SerializationUtils;

/**
 * Input source backed by a {@link java.nio.channels.ReadableByteChannel ReadableByteChannel}.
 * @author Laurent Cohen
 */
public class ChannelInputSource implements InputSource
{
	/**
	 * The backing <code>ReadableByteChannel</code>.
	 */
	private ReadableByteChannel channel = null;

	/**
	 * Initialize this input source with the specified <code>SocketWrapper</code>.
	 * @param channel the backing <code>SocketWrapper</code>.
	 */
	public ChannelInputSource(ReadableByteChannel channel)
	{
		this.channel = channel;
	}

	/**
	 * Read data from this input source into an array of bytes.
	 * @param data the buffer into which to write.
	 * @param offset the position in the buffer where to start storing the data.
	 * @param len the size in bytes of the data to read.
	 * @return the number of bytes actually read, or -1 if end of stream was reached.
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.server.nio.message.InputSource#read(byte[], int, int)
	 */
	public int read(byte[] data, int offset, int len) throws Exception
	{
		ByteBuffer buffer = ByteBuffer.wrap(data, offset, len);
		return channel.read(buffer);
	}

	/**
	 * Read data from this input source into a byte buffer.
	 * @param data the buffer into which to write.
	 * @return the number of bytes actually read, or -1 if end of stream was reached.
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.server.nio.message.InputSource#read(java.nio.ByteBuffer)
	 */
	public int read(ByteBuffer data) throws Exception
	{
		return channel.read(data);
	}

	/**
	 * Read an int value from this input source.
	 * @return the value read, or -1 if an end of file condition was reached. 
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.server.nio.message.InputSource#readInt()
	 */
	public int readInt() throws Exception
	{
		return SerializationUtils.readInt(channel);
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
