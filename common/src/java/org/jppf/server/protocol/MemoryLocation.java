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

package org.jppf.server.protocol;

import java.io.*;
import java.nio.ByteBuffer;

import org.jppf.io.*;

/**
 * Wrapper fro manipulating a file.
 * @author Laurent Cohen
 */
public class MemoryLocation extends AbstractLocation<byte[]>
{
	/**
	 * Start offset in the byte array.
	 */
	private int offset = 0;
	/**
	 * Length of data to handle.
	 */
	private int len = -1;

	/**
	 * Initialize this location and create a buffer of the specified size.
	 * @param size the size of the buffer handled by this memory location.
	 */
	public MemoryLocation(int size)
	{
		super(new byte[size]);
		len = size;
	}

	/**
	 * Initialize this location with the specified buffer.
	 * @param buffer an array of bytes.
	 */
	public MemoryLocation(byte[] buffer)
	{
		super(buffer);
		len = buffer.length;
	}

	/**
	 * Initialize this location with the specified byte array.
	 * @param buffer an array of bytes.
	 * @param offset the start position in the array of bytes.
	 * @param len the length of the buffer.
	 */
	public MemoryLocation(byte[] buffer, int offset, int len)
	{
		super(buffer);
		this.offset = offset;
		this.len = len;
	}

	/**
	 * Obtain an input stream to read from this location.
	 * @return an <code>InputStream</code> instance.
	 * @throws Exception if an I/O error occurs.
	 * @see org.jppf.server.protocol.Location#getInputStream()
	 */
	public InputStream getInputStream() throws Exception
	{
		return new ByteArrayInputStream(path, offset, len);
	}

	/**
	 * Obtain an output stream to write to this location.
	 * @return an <code>OutputStream</code> instance.
	 * @throws Exception if an I/O error occurs.
	 * @see org.jppf.server.protocol.Location#getOutputStream()
	 */
	public OutputStream getOutputStream() throws Exception
	{
		return new ByteBufferOutputStream(ByteBuffer.wrap(path, offset, len));
	}

	/**
	 * Get the size of the file this location points to.
	 * @return the size as a long value, or -1 if the file does not exist.
	 * @see org.jppf.server.protocol.Location#size()
	 */
	public long size()
	{
		return len;
	}

	/**
	 * Get the content at this location as an array of bytes. This method is
	 * overriden from {@link org.jppf.server.protocol.AbstractLocation#toByteArray() AbstractLocation.toByteArray()} for improved performance.
	 * @return a byte array.
	 * @throws Exception if an I/O error occurs.
	 * @see org.jppf.server.protocol.AbstractLocation#toByteArray()
	 */
	public byte[] toByteArray() throws Exception
	{
		return path;
	}
}
