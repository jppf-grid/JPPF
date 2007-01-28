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