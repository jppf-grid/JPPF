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

package org.jppf.comm.socket.mina;

import java.nio.ByteBuffer;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * 
 * @author Laurent Cohen
 */
public class ByteMessage
{
	/**
	 * The total length to read or write.
	 */
	public int length = 0;
	/**
	 * Indicqtes whether the read or write operation is complete.
	 */
	public boolean complete = false;
	/**
	 * The buffer containing the data to read or write.
	 */
	public ByteBuffer buffer = null;
	/**
	 * The current count of bytes read or written.
	 */
	public int count = 0;
	/**
	 * Captures an eventual exception that may have occurred while receiving or sending a message.
	 */
	public Throwable exception = null;

	/**
	 * Read the next chunk of the message.
	 * @param ioBuffer the buffer into which to read the message.
	 * @return true if the whole message has been read, false otherwise.
	 */
	public boolean read(IoBuffer ioBuffer)
	{
		if (complete) return true;
		ByteBuffer tmp = ioBuffer.buf();
		transfer(buffer, tmp);
		count = buffer.position();
		complete = buffer.remaining() == 0;
		return complete;
	}

	/**
	 * Read the next chunk of the message.
	 * @param ioBuffer the buffer into which to read the message.
	 * @return true if the whole message has been read, false otherwise.
	 */
	public boolean write(IoBuffer ioBuffer)
	{
		if (complete) return true;
		ByteBuffer tmp = ioBuffer.buf();
		transfer(tmp, buffer);
		count = buffer.position();
		complete = buffer.remaining() == 0;
		return complete;
	}

	/**
	 * Transfer the content of src into dest (as much as possible).
	 * @param dest the destination of the transfer.
	 * @param src the source of the transfer.
	 */
	private void transfer(ByteBuffer dest, ByteBuffer src)
	{
		if (dest.remaining() < src.remaining())
		{
			int limit = src.limit();
			src.limit(src.position() + dest.remaining());
			dest.put(src);
			src.limit(limit);
		}
		else dest.put(src);
	}
}
