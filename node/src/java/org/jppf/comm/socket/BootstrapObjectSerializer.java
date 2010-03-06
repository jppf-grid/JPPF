/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.comm.socket;

import java.io.*;

import org.jppf.serialization.JPPFObjectStreamFactory;
import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public class BootstrapObjectSerializer implements ObjectSerializer
{
	/**
	 * The default constructor must be public to allow for instantiation through Java reflection.
	 */
	public BootstrapObjectSerializer()
	{
	}

	/**
	 * Serialize an object into an array of bytes.
	 * @param o the object to Serialize.
	 * @return a <code>JPPFBuffer</code> instance holding the serialized object.
	 * @throws Exception if the object can't be serialized.
	 * @see org.jppf.utils.ObjectSerializer#serialize(java.lang.Object)
	 */
	public JPPFBuffer serialize(Object o) throws Exception
	{
		ByteArrayOutputStream baos = new JPPFByteArrayOutputStream();
		serialize(o, baos);
		JPPFBuffer buf = new JPPFBuffer(baos.toByteArray(), baos.size());
		return buf;
	}

	/**
	 * Serialize an object into an output stream.
	 * @param o - the object to Serialize.
	 * @param os - the output stream to serialize to.
	 * @throws Exception if the object can't be serialized.
	 * @see org.jppf.utils.ObjectSerializer#serialize(java.lang.Object, java.io.OutputStream)
	 */
	public void serialize(Object o, OutputStream os) throws Exception
	{
		ObjectOutputStream oos = JPPFObjectStreamFactory.newObjectOutputStream(os);
		oos.writeObject(o);
		oos.flush();
		oos.close();
	}

	/**
	 * Read an object from an array of bytes.
	 * @param buf buffer holding the array of bytes to deserialize from.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws Exception if the ObjectInputStream used for deserialization raises an error.
	 * @see org.jppf.utils.ObjectSerializer#deserialize(org.jppf.utils.JPPFBuffer)
	 */
	public Object deserialize(JPPFBuffer buf) throws Exception
	{
		return deserialize(new ByteArrayInputStream(buf.getBuffer()));
	}

	/**
	 * Read an object from an array of bytes.
	 * @param bytes buffer holding the array of bytes to deserialize from.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws Exception if the ObjectInputStream used for deserialization raises an error.
	 * @see org.jppf.utils.ObjectSerializer#deserialize(byte[])
	 */
	public Object deserialize(byte[] bytes) throws Exception
	{
		return deserialize(new ByteArrayInputStream(bytes));
	}

	/**
	 * Read an object from an array of bytes.
	 * @param bytes buffer holding the array of bytes to deserialize from.
	 * @param offset position at which to start reading the bytes from.
	 * @param length the number of bytes to read.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws Exception if the ObjectInputStream used for deserialization raises an error.
	 * @see org.jppf.utils.ObjectSerializer#deserialize(byte[], int, int)
	 */
	public Object deserialize(byte[] bytes, int offset, int length) throws Exception
	{
		return deserialize(new ByteArrayInputStream(bytes, offset, length));
	}

	/**
	 * Read an object from an input stream.
	 * @param is - the input stream to deserialize from.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws Exception if the ObjectInputStream used for deserialization raises an error.
	 * @see org.jppf.utils.ObjectSerializer#deserialize(java.io.InputStream)
	 */
	public Object deserialize(InputStream is) throws Exception
	{
		Object o = null;
		ObjectInputStream ois = JPPFObjectStreamFactory.newObjectInputStream(is);
		o = ois.readObject();
		ois.close();
		return o;
	}
}
