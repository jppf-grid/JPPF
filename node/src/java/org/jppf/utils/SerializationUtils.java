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

package org.jppf.utils;

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
	 * Deserialize an int value from an array of bytes.
	 * @param data the array of bytes into which to serialize the value.
	 * @param offset the position in the array of byte at which the serializatrion should start.
	 * @return the int value read from the array of bytes
	 * @see org.jppf.utils.SerializationHelper#readInt(byte[], int)
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
	 * Convert a byte value into an unsigned int value.
	 * @param b the value to convert.
	 * @return the unsigned int result.
	 */
	private static int convertByte(int b)
	{
		return b < 0 ? b + 256 : b;
	}
}
