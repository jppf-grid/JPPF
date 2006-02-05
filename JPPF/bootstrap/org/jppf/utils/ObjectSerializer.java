/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
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

import java.io.IOException;

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
	 * @throws IOException if the object can't be serialized.
	 */
	JPPFBuffer serialize(Object o) throws IOException;

	/**
	 * Read an object from an array of bytes.
	 * @param buf buffer holding the array of bytes to deserialize from.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws ClassNotFoundException the class of the deserialized object could not be found.
	 * @throws IOException if the ObjectInputStream used for deserialization raises an error.
	 */
	Object deserialize(JPPFBuffer buf) throws ClassNotFoundException, IOException;

	/**
	 * Read an object from an array of bytes.
	 * @param bytes buffer holding the array of bytes to deserialize from.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws ClassNotFoundException the class of the deserialized object could not be found.
	 * @throws IOException if the ObjectInputStream used for deserialization raises an error.
	 */
	Object deserialize(byte[] bytes) throws ClassNotFoundException, IOException;

	/**
	 * Read an object from an array of bytes.
	 * @param bytes buffer holding the array of bytes to deserialize from.
	 * @param offset position at which to start reading the bytes from.
	 * @param length the number of bytes to read.
	 * @return the object that was deserialized from the array of bytes.
	 * @throws ClassNotFoundException the class of the deserialized object could not be found.
	 * @throws IOException if the ObjectInputStream used for deserialization raises an error.
	 */
	Object deserialize(byte[] bytes, int offset, int length) throws ClassNotFoundException, IOException;
}