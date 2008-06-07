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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.jppf.utils.SerializationUtils;

/**
 * Output destination backed by a file.
 * @author Laurent Cohen
 */
public class FileOutputDestination implements OutputDestination
{
	/**
	 * File channel used to write to the underlying file.
	 */
	private FileChannel channel = null;

	/**
	 * Initialize this file output destination with the specified file path.
	 * @param path the path to the file to read from.
	 * @throws Exception if an IO error occurs.
	 */
	public FileOutputDestination(String path) throws Exception
	{
		this(new File(path));
	}

	/**
	 * Initialize this file output destination with the specified file.
	 * @param file the file to read from.
	 * @throws Exception if an IO error occurs.
	 */
	public FileOutputDestination(File file) throws Exception
	{
		channel = new FileOutputStream(file).getChannel();
	}

	/**
	 * Write data to this output destination from an array of bytes.
	 * @param data the buffer containing the data to write.
	 * @param offset the position in the buffer where to start reading the data.
	 * @param len the size in bytes of the data to write.
	 * @return the number of bytes actually written, or -1 if end of stream was reached.
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.server.nio.message.OutputDestination#write(byte[], int, int)
	 */
	public int write(byte[] data, int offset, int len) throws Exception
	{
		return channel.write(ByteBuffer.wrap(data, offset, len));
	}

	/**
	 * Write data to this output destination from a byte buffer.
	 * @param data the buffer containing the data to write.
	 * @return the number of bytes actually written, or -1 if end of stream was reached.
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.server.nio.message.OutputDestination#write(java.nio.ByteBuffer)
	 */
	public int write(ByteBuffer data) throws Exception
	{
		return channel.write(data);
	}

	/**
	 * Write an int value to this output destination.
	 * @param value the value to write. 
	 * @throws Exception if an IO error occurs.
	 * @see org.jppf.server.nio.message.OutputDestination#writeInt(int)
	 */
	public void writeInt(int value) throws Exception
	{
		SerializationUtils.writeInt(channel, value);
	}

	/**
	 * Close this output destination and release any system resources associated with it.
	 * @throws IOException if an IO error occurs.
	 * @see java.io.Closeable#close()
	 */
	public void close() throws IOException
	{
		channel.force(false);
		channel.close();
	}
}
