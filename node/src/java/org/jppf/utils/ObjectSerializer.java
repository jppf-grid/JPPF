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
package org.jppf.utils;

import java.io.*;


/**
 * Instances of this class are used to serialize or deserialize objects to or from an array of bytes.<br>
 * A specific use of this class is that it can be loaded by a new classloader, making the execution transparent
 * to any change in the client code.
 * @author Laurent Cohen
 */
public interface ObjectSerializer
{
	/**
	 * Serialize an object into an array of bytes.
	 * @param o the object to Serialize.
	 * @return a <code>JPPFBuffer</code> instance holding the serialized object.
	 * @throws Exception if the object can't be serialized.
	 */
	JPPFBuffer serialize(Object o) throws Exception;

	/**
	 * Serialize an object into an output stream.
	 * @param o - the object to Serialize.
	 * @param os - the output stream to serialize to.
	 * @throws Exception if the object can't be serialized.
	 */
	void serialize(Object o, OutputStream os) throws Exception;

	/**
	 * Read an object from an array of bytes.
	 * @param buf buffer holding the array of bytes to deserialize from.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws Exception the class of the deserialized object could not be found.
	 */
	Object deserialize(JPPFBuffer buf) throws Exception;

	/**
	 * Read an object from an array of bytes.
	 * @param bytes buffer holding the array of bytes to deserialize from.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws Exception if the ObjectInputStream used for deserialization raises an error.
	 */
	Object deserialize(byte[] bytes) throws Exception;

	/**
	 * Read an object from an array of bytes.
	 * @param bytes buffer holding the array of bytes to deserialize from.
	 * @param offset position at which to start reading the bytes from.
	 * @param length the number of bytes to read.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws Exception if the ObjectInputStream used for deserialization raises an error.
	 */
	Object deserialize(byte[] bytes, int offset, int length) throws Exception;

	/**
	 * Read an object from an input stream.
	 * @param is - the input stream to deserialize from.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws Exception if the ObjectInputStream used for deserialization raises an error.
	 */
	Object deserialize(InputStream is) throws Exception;
}
