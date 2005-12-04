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
}