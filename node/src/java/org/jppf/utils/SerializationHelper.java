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

import java.io.*;


/**
 * Collection of utility methods for serializing and deserializing to and from bytes buffers.
 * @author Laurent Cohen
 */
public interface SerializationHelper
{
	/**
	 * Deserialize a specified number of objects from an array of bytes.
	 * @param bytes the array of bytes to read from.
	 * @param start the offset to start reading at.
	 * @param count the number of objects to read.
	 * @return an array of the deserialized objects.
	 * @throws Exception if an error occurs during deserialization.
	 */
	Object[] readFromBuffer(byte[] bytes, int start, int count) throws Exception;
	/**
	 * Serialize a set of objects into an array of bytes.
	 * @param objects the objects to serialize.
	 * @return a <code>JPPFBuffer</code> instance.
	 * @throws Exception if an error occurs during the serialization.
	 */
	JPPFBuffer writeToBuffer(Object[] objects) throws Exception;
	/**
	 * Get a reference to the <code>ObjectSerializer</code> used by this helper.
	 * @return an <code>ObjectSerializer</code> instance.
	 * @throws Exception if the serializer could not be obtained.
	 */
	ObjectSerializer getSerializer() throws Exception;
	/**
	 * Deserialize the next object from a stream.
	 * @param dis the data stream from which to fetch the serialized object.
	 * @param isCompressed determines whether the serialized representation object should be
	 * decompressed before deserialization. 
	 * @return the deserialized object.
	 * @throws Exception if an error occurs while reading from the stream, uncompressing or deserializing.
	 */
	Object readNextObject(DataInputStream dis, boolean isCompressed) throws Exception;
	/**
	 * Serialize an object into a stream.
	 * @param o the object to serialize.
	 * @param dos the data stream into which to write the serialized object.
	 * @param isCompressed determines whether the serialized representation object should be
	 * compressed before serialization. 
	 * @throws Exception if an error occurs while writing to the stream, compressing or serializing.
	 */
	void writeNextObject(Object o, DataOutputStream dos, boolean isCompressed) throws Exception;
	/**
	 * Read the next series of bytes from an input stream.
	 * @param dis the stream to read from.
	 * @return an array of the bytes read from the stream.
	 * @throws Exception if an error occurs while reading from the stream.
	 */
	byte[] readNextBytes(DataInputStream dis) throws Exception;
	/**
	 * Write the next series of bytes to an output stream.
	 * @param dos the stream to write to.
	 * @param bytes the array of bytes to write to the stream.
	 * @param start the start position in the bytes array.
	 * @param length the number of bytes to write.
	 * @throws Exception if an error occurs while writing the stream.
	 */
	void writeNextBytes(DataOutputStream dos, byte[] bytes, int start, int length) throws Exception;
}