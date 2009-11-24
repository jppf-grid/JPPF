/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import java.util.List;


/**
 * Collection of utility methods for serializing and deserializing to and from bytes buffers.
 * @author Laurent Cohen
 */
public interface SerializationHelper
{
	/**
	 * Get a reference to the <code>ObjectSerializer</code> used by this helper.
	 * @return an <code>ObjectSerializer</code> instance.
	 * @throws Exception if the serializer could not be obtained.
	 */
	ObjectSerializer getSerializer() throws Exception;
	/**
	 * Serialize an object into an array of bytes.
	 * @param o the object to serialize.
	 * @param isCompressed determines whether the serialized representation object should be
	 * compressed before serialization.
	 * @return a <code>JPPFBuffer</code> instance encapsulating the resulting array of bytes. 
	 * @throws Exception if an error occurs while writing to the stream, compressing or serializing.
	 */
	JPPFBuffer toBytes(Object o, boolean isCompressed) throws Exception;
	/**
	 * Serialize an int value into an array of bytes.
	 * @param value the int value to serialize.
	 * @param data the array of bytes into which to serialize the value.
	 * @param offset the position in the array of byte at which the serializatrion should start.
	 * @return the new offset after serialization.
	 */
	int writeInt(int value, byte[] data, int offset);
	/**
	 * Copy some byte data to a byte buffer.
	 * @param source the source data.
	 * @param dest the destination buffer
	 * @param offset the position at which to start copying in the destination.
	 * @param length the length of the data to copy.
	 * @return the new postion in the destination buffer.
	 */
	int copyToBuffer(byte[] source, byte[] dest, int offset, int length);
	/**
	 * Deserialize an int value from an array of bytes.
	 * @param data the array of bytes into which to serialize the value.
	 * @param offset the position in the array of byte at which the serializatrion should start.
	 * @return the int value read from the array of bytes
	 */
	int readInt(byte[] data, int offset);
	/**
	 * Copy some byte data from a byte buffer.
	 * @param source the source data.
	 * @param offset the position at which to start copying from the source.
	 * @return the copied data as an array of bytes.
	 */
	byte[] copyFromBuffer(byte[] source, int offset);
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
	 */
	<T> int fromBytes(byte[] source, int offset, boolean compressed, List<T> result, int count) throws Exception;
}
