/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.server.nio;

import java.nio.ByteBuffer;

import org.jppf.io.*;

/**
 * Instances of this class represent a data frame read asynchronously from an input source.
 * @author Laurent Cohen
 */
public class NioObject
{
	/**
	 * The message length.
	 */
	private int size = 0;
	/**
	 * What has currently been read from the message.
	 */
	private int count = 0;
	/**
	 * Location of the data to read or write.
	 */
	private DataLocation data = null;
	/**
	 * Determines whether the I/O performed by this object are blocking.
	 */
	private boolean blocking = false;

	/**
	 * Initialize this IOObject with the specified size.
	 * @param size - the size of the internal buffer.
	 * @param blocking - specfifes whether the I/O performed by this object are blocking.
	 */
	public NioObject(int size, boolean blocking)
	{
		this(new ByteBufferLocation(size), blocking);
	}

	/**
	 * Initialize this location with the specified buffer.
	 * @param data the buffer containing the data.
	 * @param offset the start position in the buffer.
	 * @param len the size in bytes of the data to write.
	 * @param blocking - specifIes whether the I/O performed by this object are blocking.
	 */
	public NioObject(byte[] data, int offset, int len, boolean blocking)
	{
		this(new ByteBufferLocation(ByteBuffer.wrap(data, offset, len)), blocking);
	}

	/**
	 * Initialize this IOObject with the specified size.
	 * @param data - the location of the data to read from or write to.
	 * @param blocking - specfifes whether the I/O performed by this object are blocking.
	 */
	public NioObject(DataLocation data, boolean blocking)
	{
		this.size = data.getSize();
		this.data = data;
	}

	/**
	 * Read the current frame.
	 * @param source - the source to read from.
	 * @return true if the frame has been read fully, false otherwise.
	 * @throws Exception if any error occurs.
	 */
	public boolean read(InputSource source) throws Exception
	{
		if (count >= size) return true;
		int n = data.transferFrom(source, blocking);
		if (n > 0) count += n;
		if (count >= size)
		{
			if (data instanceof ByteBufferLocation) ((ByteBufferLocation) data).buffer().flip();
			return true;
		}
		return false;
	}

	/**
	 * Write the current data object.
	 * @param dest - the destination to write to.
	 * @return true if the data has been written fully, false otherwise.
	 * @throws Exception if any error occurs.
	 */
	public boolean write(OutputDestination dest) throws Exception
	{
		if (count >= size) return true;
		int n = data.transferTo(dest, blocking);
		if (n > 0) count += n;
		if (count >= size)
		{
			if (data instanceof ByteBufferLocation) ((ByteBufferLocation) data).buffer().flip();
			return true;
		}
		return false;
	}

	/**
	 * Location of the data to read or write.
	 * @return a <code>DataLocation</code> instance.
	 */
	public DataLocation getData()
	{
		return data;
	}
}
