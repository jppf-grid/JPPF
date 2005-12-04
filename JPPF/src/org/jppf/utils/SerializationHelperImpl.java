/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.utils;

import java.io.*;
import org.jppf.classloader.JPPFClassLoader;

/**
 * Collection of utility methods for serializing and deserializing to and from bytes buffers.
 * @author Laurent Cohen
 */
public class SerializationHelperImpl implements SerializationHelper
{
	static
	{
		System.out.println("SerializationHelperImpl loaded by "+SerializationHelperImpl.class.getClassLoader());
	}

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
	 * @see org.jppf.utils.SerializationHelper#readFromBuffer(byte[], int, int)
	 */
	public Object[] readFromBuffer(byte[] bytes, int start, int count) throws Exception
	{
		Object[] result = new Object[count];
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		DataInputStream dis = new DataInputStream(bais);
		int offset = start;
		for (int i=0; i<count; i++)
		{
			int len = dis.readInt();
			offset += 4;
			result[i] =  getSerializer().deserialize(bytes, offset, len);
			offset += len;
			if (i < count-1) dis.skip(len);
		}
		dis.close();
		return result;
	}

	/**
	 * @see org.jppf.utils.SerializationHelper#writeToBuffer(java.lang.Object[])
	 */
	public JPPFBuffer writeToBuffer(Object[] objects) throws Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		JPPFBuffer buf = null;
		for (Object o: objects)
		{
			buf = getSerializer().serialize(o);
			dos.writeInt(buf.getLength());
			dos.write(buf.getBuffer(), 0, buf.getLength());
		}
		dos.flush();
		dos.close();
		buf = new JPPFBuffer();
		buf.setLength(baos.size());
		buf.setBuffer(baos.toByteArray());
		return buf;
	}
	
	/**
	 * Get the object serializer for this helper.
	 * @return an <code>ObjectSerializer</code> instance.
	 * @throws Exception if the object serializer could not be instantiated.
	 */
	private ObjectSerializer getSerializer() throws Exception
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
}
