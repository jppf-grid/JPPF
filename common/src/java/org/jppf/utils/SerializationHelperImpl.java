/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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

import java.io.*;
import org.apache.log4j.Logger;
import org.jppf.JPPFException;
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
	 * Deserialize a specified number of objects from an array of bytes.
	 * @param bytes the array of bytes to read from.
	 * @param start the offset to start reading at.
	 * @param count the number of objects to read.
	 * @return an array of the deserialized objects.
	 * @throws Exception if an error occurs during deserialization.
	 * @see org.jppf.utils.SerializationHelper#readFromBuffer(byte[], int, int)
	 */
	public Object[] readFromBuffer(byte[] bytes, int start, int count) throws Exception
	{
		Object[] result = new Object[count];
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		DataInputStream dis = new DataInputStream(bais);

		if (log.isDebugEnabled())
		{
	    log.debug("bytes to read : "+StringUtils.dumpBytes(bytes, 0, bytes.length));
			log.debug("buffer length is "+bytes.length+", start is "+start);
		}
		for (int i=0; i<count; i++) result[i] =  readNextObject(dis, false);
		dis.close();
		return result;
	}

	/**
	 * Serialize a set of objects into an array of bytes.
	 * @param objects the objects to serialize.
	 * @return a <code>JPPFBuffer</code> instance.
	 * @throws Exception if an error occurs during the serialization.
	 * @see org.jppf.utils.SerializationHelper#writeToBuffer(java.lang.Object[])
	 */
	public JPPFBuffer writeToBuffer(Object[] objects) throws Exception
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			for (Object o: objects) writeNextObject(o, dos, false);
			dos.flush();
			dos.close();
			return new JPPFBuffer(baos.toByteArray(), baos.size());
		}
		catch(Exception e)
		{
			throw new JPPFException(e);
		}
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
	 * Deserialize the next object from a stream.
	 * @param dis the data stream from which to fetch the serialized object.
	 * @param isCompressed determines whether the serialized representation object should be
	 * decompressed before deserialization. 
	 * @return the deserialized object.
	 * @throws Exception if an error occurs while reading from the stream, uncompressing or deserializing.
	 */
	public Object readNextObject(DataInputStream dis, boolean isCompressed) throws Exception
	{
		byte[] actual = null;
		int len = dis.readInt();
		byte[] temp = new byte[len];
		
		
		int aux = 0;
		while(aux < len){
			aux += dis.read(temp, aux, len - aux);
		}
		
		if (isCompressed) actual = CompressionUtils.unzip(temp, 0, len);
		else actual = temp;
		if (log.isDebugEnabled())
		{
			log.debug(""+actual.length+" bytes to deserialize:\n"+
				StringUtils.dumpBytes(actual, 0, actual.length));
		}
		return getSerializer().deserialize(actual);
	}

	/**
	 * Serialize an object into a stream.
	 * @param o the object to serialize.
	 * @param dos the data stream into which to write the serialized object.
	 * @param isCompressed determines whether the serialized representation object should be
	 * compressed before serialization. 
	 * @throws Exception if an error occurs while writing to the stream, compressing or serializing.
	 */
	public void writeNextObject(Object o, DataOutputStream dos, boolean isCompressed) throws Exception
	{
		byte[] actual = null;
		JPPFBuffer buf = getSerializer().serialize(o);
		if (log.isDebugEnabled())
		{
			log.debug(""+buf.getLength()+" bytes to serialize:\n"+
				StringUtils.dumpBytes(buf.getBuffer(), 0, buf.getLength()));
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
		writeNextBytes(dos, actual, 0, len);
	}
	
	/**
	 * Read the next series of bytes from an input stream.
	 * @param dis the stream to read from.
	 * @return an array of the bytes read from the stream.
	 * @throws Exception if an error occurs while reading from the stream.
	 */
	public byte[] readNextBytes(DataInputStream dis) throws Exception
	{
		int length = dis.readInt();
		byte[] result = new byte[length];
		int aux = 0;
		while(aux < length){
			aux += dis.read(result, aux, length - aux);
		}
		return result;
	}

	/**
	 * Write the next series of bytes to an output stream.
	 * @param dos the stream to write to.
	 * @param bytes the array of bytes to write to the stream.
	 * @param start the start position in the bytes array.
	 * @param length the number of bytes to write.
	 * @throws Exception if an error occurs while writing the stream.
	 */
	public void writeNextBytes(DataOutputStream dos, byte[] bytes, int start, int length) throws Exception
	{
		dos.writeInt(length);
		dos.write(bytes, start, length);
		dos.flush();
	}
}
