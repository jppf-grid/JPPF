/*
 * Java Parallel Processing Framework.
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

package org.jppf.gigaspaces.serialization;

import java.io.*;

import org.jppf.serialization.JPPFObjectStreamFactory;
import org.jppf.utils.ObjectSerializerImpl;

/**
 * 
 * @author Laurent Cohen
 */
public class GSObjectSerializer extends ObjectSerializerImpl
{
	/**
	 * The default constructor must be public to allow for instantiation through Java reflection.
	 */
	public GSObjectSerializer()
	{
	}

	/**
	 * Read an object from an array of bytes.
	 * @param bytes buffer holding the array of bytes to deserialize from.
	 * @param offset position at which to start reading the bytes from.
	 * @param length the number of bytes to read.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws Exception if the ObjectInputStream used for deserialization raises an error.
	 * @see org.jppf.utils.ObjectSerializerImpl#deserialize(byte[], int, int)
	 */
	public Object deserialize(byte[] bytes, int offset, int length) throws Exception
	{
		Object o = null;
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes, offset, length);
		ObjectInputStream ois = JPPFObjectStreamFactory.newObjectInputStream(bis);
		o = ois.readObject();
		ois.close();
		return o;
	}
}
