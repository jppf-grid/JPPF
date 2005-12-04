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
import org.apache.log4j.Logger;

/**
 * Instances of this class are used to serialize or deserialize objects to or from an array of bytes.<br>
 * A specific use of this class is that it can be loaded by a new classloader, making the execution transparent
 * to any change in the client code.
 * @author Laurent Cohen
 */
public class ObjectSerializerImpl implements ObjectSerializer
{
	static
	{
		System.out.println("ObjectSerializerImpl loaded by "+ObjectSerializerImpl.class.getClassLoader());
	}

	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(ObjectSerializerImpl.class);

	/**
	 * The default constructor must be public to allow for instantiation through Java reflection.
	 */
	public ObjectSerializerImpl()
	{
	}

	/**
	 * @see org.jppf.utils.ObjectSerializer#serialize(java.lang.Object)
	 */
	public JPPFBuffer serialize(Object o) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.flush();
		oos.close();
		JPPFBuffer buf = new JPPFBuffer();
		buf.setBuffer(baos.toByteArray());
		buf.setLength(baos.size());
		return buf;
	}

	/**
	 * @see org.jppf.utils.ObjectSerializer#deserialize(org.jppf.utils.JPPFBuffer)
	 */
	public Object deserialize(JPPFBuffer buf) throws ClassNotFoundException, IOException
	{
		return deserialize(buf.getBuffer(), 0, buf.getLength());
	}

	/**
	 * @see org.jppf.utils.ObjectSerializer#deserialize(byte[])
	 */
	public Object deserialize(byte[] bytes) throws ClassNotFoundException, IOException
	{
		return deserialize(bytes, 0, bytes.length);
	}

	/**
	 * @see org.jppf.utils.ObjectSerializer#deserialize(byte[], int, int)
	 */
	public Object deserialize(byte[] bytes, int offset, int length)
		throws ClassNotFoundException, IOException
	{
		Object o = null;
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes, offset, length));
		o = ois.readObject();
		ois.close();
		return o;
	}
}
