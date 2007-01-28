/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.utils;

import java.util.List;

import org.apache.log4j.Logger;
import org.jppf.node.JPPFClassLoader;

/**
 * Collection of utility methods for serializing and deserializing to and from bytes buffers.
 * @author Laurent Cohen
 */
public class SerializationHelperImpl implements SerializationHelper
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(SerializationHelperImpl.class);
	/**
	 * Determines whether the debug level is enabled in the log4j configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Determines whether dumping byte arrays in the log is enabled.
	 */
	private boolean dumpEnabled = JPPFConfiguration.getProperties().getBoolean("byte.array.dump.enabled", false);

	/**
	 * Used to serialize and deserialize objects to and from object streams.
	 */
	private ObjectSerializer serializer = null;

	/**
	 * Instantiation of this class is not permitted.
	 */
	public SerializationHelperImpl()
	{
	}

	/**
	 * Get the object serializer for this helper.
	 * @return an <code>ObjectSerializer</code> instance.
	 * @throws Exception if the object serializer could not be instantiated.
	 * @see org.jppf.utils.SerializationHelper#getSerializer()
	 */
	public ObjectSerializer getSerializer() throws Exception
	{
		if (serializer == null)
		{
			ClassLoader cl = getClass().getClassLoader();
			if (cl instanceof JPPFClassLoader)
			{
				serializer = (ObjectSerializer)
					((JPPFClassLoader) cl).loadJPPFClass("org.jppf.utils.ObjectSerializerImpl").newInstance();
			}
			else
			{
				serializer = (ObjectSerializer)
					cl.loadClass("org.jppf.utils.ObjectSerializerImpl").newInstance();
			}
		}
		return serializer;
	}

	/**
	 * Serialize an object into an array of bytes.
	 * @param o the object to serialize.
	 * @param isCompressed determines whether the serialized representation object should be
	 * compressed before serialization.
	 * @return a <code>JPPFBuffer</code> instance encapsulating the resulting array of bytes. 
	 * @throws Exception if an error occurs while writing to the stream, compressing or serializing.
	 */
	public JPPFBuffer toBytes(Object o, boolean isCompressed) throws Exception
	{
		byte[] actual = null;
		JPPFBuffer buf = new ObjectSerializerImpl().serialize(o);
		if (debugEnabled)
		{
			//log.debug(""+buf.getLength()+" bytes to serialize");
			//if (dumpEnabled) log.debug("dump of bytes to serialize:\n"+ StringUtils.dumpBytes(buf.getBuffer(), 0, buf.getLength()));
		}
		int len = 0;
		if (isCompressed)
		{
			actual = CompressionUtils.zip(buf.getBuffer(), 0, buf.getLength());
			len = actual.length;
		}
		else
		{
			actual = buf.getBuffer();
			len = buf.getLength();
		}
		return new JPPFBuffer(actual, len);
	}

	/**
	 * Serialize an int value into an array of bytes.
	 * @param value the int value to serialize.
	 * @param data the array of bytes into which to serialize the value.
	 * @param offset the position in the array of byte at which the serializatrion should start.
	 * @return the new offset after serialization.
	 */
	public int writeInt(int value, byte[] data, int offset)
	{
		int pos = offset;
    data[pos++] = (byte) ((value >>> 24) & 0xFF);
    data[pos++] = (byte) ((value >>> 16) & 0xFF);
    data[pos++] = (byte) ((value >>>  8) & 0xFF);
    data[pos++] = (byte) ((value >>>  0) & 0xFF);
    return pos;
	}

	/**
	 * Copy some byte data to a byte buffer.
	 * @param source the source data.
	 * @param dest the destination buffer
	 * @param offset the position at which to start copying in the destination.
	 * @param length the length of the data to copy.
	 * @return the new postion in the destination buffer.
	 * @see org.jppf.utils.SerializationHelper#copyToBuffer(byte[], byte[], int, int)
	 */
	public int copyToBuffer(byte[] source, byte[] dest, int offset, int length)
	{
		int pos = writeInt(length, dest, offset);
		System.arraycopy(source, 0, dest, pos, length);
		pos += length;
    return pos;
	}

	/**
	 * Deserialize an int value from an array of bytes.
	 * @param data the array of bytes into which to serialize the value.
	 * @param offset the position in the array of byte at which the serializatrion should start.
	 * @return the int value read from the array of bytes
	 * @see org.jppf.utils.SerializationHelper#readInt(byte[], int)
	 */
	public int readInt(byte[] data, int offset)
	{
		int pos = offset;
		//ByteArrayInputStream dis = null;
		//dis.read();
		
    int result = convertByte(data[pos++]) << 24;
    result += convertByte(data[pos++]) << 16;
    result += convertByte(data[pos++]) << 8;
    result += convertByte(data[pos++]) << 0;
    return result;
	}

	/**
	 * Convert a byte value into an unsigned int value.
	 * @param b the value to convert.
	 * @return the unsigned int result.
	 */
	private int convertByte(int b)
	{
		return b < 0 ? b + 256 : b;
	}

	/**
	 * Copy some byte data from a byte buffer.
	 * @param source the source data.
	 * @param offset the position at which to start copying from the source.
	 * @return the copied data as an array of bytes.
	 * @see org.jppf.utils.SerializationHelper#copyFromBuffer(byte[], int)
	 */
	public byte[] copyFromBuffer(byte[] source, int offset)
	{
		int len = readInt(source, offset);
		byte[] data = new byte[len];
		System.arraycopy(source, offset + 4, data, 0, len);
		return data;
	}

	/**
	 * Deserialize a number of objects from an array of bytes.
	 * @param <T> the type of the objects to deserialize.
	 * @param source the array pf bytes from which to deserialize.
	 * @param offset the position in the source data at whcih to start reading.
	 * @param compressed determines whether the source data is comprssed or not.
	 * @param result a list holding the resulting deserialized objects.
	 * @param count the number of objects to deserialize.
	 * @return the new position in the source data after deserialization.
	 * @throws Exception if an error occurs while deserializing.
	 * @see org.jppf.utils.SerializationHelper#fromBytes(byte[], int, boolean, java.util.List, int)
	 */
	@SuppressWarnings("unchecked")
	public <T> int fromBytes(byte[] source, int offset, boolean compressed, List<T> result, int count) throws Exception
	{
		int pos = offset;
		for (int i=0; i<count; i++)
		{
			byte[] data = copyFromBuffer(source, pos);
			byte[] actual = null;
			pos += 4 + data.length;
			if (compressed) actual = CompressionUtils.unzip(data, 0, data.length);
			else actual = data;
			result.add((T) getSerializer().deserialize(actual));
		}
		return pos;
	}
}
