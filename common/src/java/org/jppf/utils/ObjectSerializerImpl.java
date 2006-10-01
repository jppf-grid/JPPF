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

/**
 * Instances of this class are used to serialize or deserialize objects to or from an array of bytes.<br>
 * A specific use of this class is that it can be loaded by a new classloader, making the execution transparent
 * to any change in the client code.
 * @author Laurent Cohen
 */
public class ObjectSerializerImpl implements ObjectSerializer
{
	/**
	 * Log4j logger for this class.
	 */
	//private static Logger log = Logger.getLogger(ObjectSerializerImpl.class);

	/**
	 * The default constructor must be public to allow for instantiation through Java reflection.
	 */
	public ObjectSerializerImpl()
	{
	}

	/**
	 * Serialize an object into an array of bytes.
	 * @param o the object to Serialize.
	 * @return a <code>JPPFBuffer</code> instance holding the serialized object.
	 * @throws IOException if the object can't be serialized.
	 * @see org.jppf.utils.ObjectSerializer#serialize(java.lang.Object)
	 */
	public JPPFBuffer serialize(Object o) throws IOException
	{
		ByteArrayOutputStream baos = new JPPFByteArrayOutputStream();
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
	 * Read an object from an array of bytes.
	 * @param buf buffer holding the array of bytes to deserialize from.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws ClassNotFoundException the class of the deserialized object could not be found.
	 * @throws IOException if the ObjectInputStream used for deserialization raises an error.
	 * @see org.jppf.utils.ObjectSerializer#deserialize(org.jppf.utils.JPPFBuffer)
	 */
	public Object deserialize(JPPFBuffer buf) throws ClassNotFoundException, IOException
	{
		return deserialize(buf.getBuffer(), 0, buf.getLength());
	}

	/**
	 * Read an object from an array of bytes.
	 * @param bytes buffer holding the array of bytes to deserialize from.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws ClassNotFoundException the class of the deserialized object could not be found.
	 * @throws IOException if the ObjectInputStream used for deserialization raises an error.
	 * @see org.jppf.utils.ObjectSerializer#deserialize(byte[])
	 */
	public Object deserialize(byte[] bytes) throws ClassNotFoundException, IOException
	{
		return deserialize(bytes, 0, bytes.length);
	}

	/**
	 * Read an object from an array of bytes.
	 * @param bytes buffer holding the array of bytes to deserialize from.
	 * @param offset position at which to start reading the bytes from.
	 * @param length the number of bytes to read.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws ClassNotFoundException the class of the deserialized object could not be found.
	 * @throws IOException if the ObjectInputStream used for deserialization raises an error.
	 * @see org.jppf.utils.ObjectSerializer#deserialize(byte[], int, int)
	 */
	public Object deserialize(byte[] bytes, int offset, int length)
		throws ClassNotFoundException, IOException
	{
		Object o = null;
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes, offset, length);
		ObjectInputStream ois = new ObjectInputStream(bis);
		o = ois.readObject();
		ois.close();
		return o;
	}
}
