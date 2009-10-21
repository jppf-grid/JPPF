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

package org.jppf.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * 
 * @author Laurent Cohen
 */
public final class SerializationUtils
{
	/**
	 * Instantiation of this class is not permitted.
	 */
	private SerializationUtils()
	{
	}

	/**
	 * Serialize an int value into an array of bytes.
	 * @param value the int value to serialize.
	 * @return an array of bytes filled with the value's representation.
	 */
	public static byte[] writeInt(int value)
	{
    return writeInt(value, new byte[4], 0);
	}

	/**
	 * Serialize an int value into an array of bytes.
	 * @param value the int value to serialize.
	 * @param data the array of bytes into which to serialize the value.
	 * @param offset the position in the array of byte at which the serializatrion should start.
	 * @return an array of bytes filled with the value's representation, starting at the specified offset.
	 */
	public static byte[] writeInt(int value, byte[] data, int offset)
	{
		int pos = offset;
    data[pos++] = (byte) ((value >>> 24) & 0xFF);
    data[pos++] = (byte) ((value >>> 16) & 0xFF);
    data[pos++] = (byte) ((value >>>  8) & 0xFF);
    data[pos++] = (byte) ((value >>>  0) & 0xFF);
    return data;
	}

	/**
	 * Serialize an int value to a stream.
	 * @param value the int value to serialize.
	 * @param os the stream to write to.
	 * @throws IOException if an error occurs while writing the data.
	 */
	public static void writeInt(int value, OutputStream os) throws IOException
	{
    os.write((byte) ((value >>> 24) & 0xFF));
    os.write((byte) ((value >>> 16) & 0xFF));
    os.write((byte) ((value >>>  8) & 0xFF));
    os.write((byte) ((value >>>  0) & 0xFF));
	}

	/**
	 * Wrtie an integer value to a channel.
	 * @param channel the channel to write to.
	 * @param value the value to write.
	 * @throws IOException if an error occurs while writing the data.
	 */
	public static void writeInt(WritableByteChannel channel, int value) throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocateDirect(4);
		buf.putInt(value);
		buf.flip();
		int count = 0;
		while (count < 4)
		{
			int n = 0;
			while (n == 0) n = channel.write(buf);
			if (n < 0) throw new ClosedChannelException();
			count += n;
			/*
			count += channel.write(buf);
			if (count < 0) throw new ClosedChannelException();
			*/
		}
	}

	/**
	 * Read an integer value from a channel.
	 * @param channel the channel to read from.
	 * @return the value read from the channel.
	 * @throws IOException if an error occurs while reading the data.
	 */
	public static int readInt(ReadableByteChannel channel) throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocateDirect(4);
		int count = 0;
		while (count < 4)
		{
			int n = 0;
			while (n == 0) n = channel.read(buf);
			if (n < 0) throw new ClosedChannelException();
			count += n;
			/*
			count += channel.read(buf);
			if (count < 0) throw new ClosedChannelException();
			*/
		}
		buf.flip();
		return buf.getInt();
	}

	/**
	 * Deserialize an int value from an array of bytes.
	 * @param data the array of bytes into which to serialize the value.
	 * @param offset the position in the array of byte at which the serializatrion should start.
	 * @return the int value read from the array of bytes
	 */
	public static int readInt(byte[] data, int offset)
	{
		int pos = offset;
    int result = convertByte(data[pos++]) << 24;
    result    += convertByte(data[pos++]) << 16;
    result    += convertByte(data[pos++]) <<  8;
    result    += convertByte(data[pos++]) <<  0;
    return result;
	}

	/**
	 * Deserialize an int value from a stream.
	 * @param is the stream to read from.
	 * @return the int value read from the stream.
	 * @throws IOException if an error occurs while reading the data.
	 */
	public static int readInt(InputStream is) throws IOException
	{
    int result = convertByte(is.read()) << 24;
    result    += convertByte(is.read()) << 16;
    result    += convertByte(is.read()) <<  8;
    result    += convertByte(is.read()) <<  0;
    return result;
	}

	/**
	 * Convert a byte value into an unsigned int value.
	 * @param b the value to convert.
	 * @return the unsigned int result.
	 */
	private static int convertByte(int b)
	{
		return b < 0 ? b + 256 : b;
	}
}
